@file:JvmName("IdeMerger")

package org.jetbrains.kotlin.tools.kompot.idea.merger

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.ssg.Configuration
import org.jetbrains.kotlin.tools.kompot.ssg.SSGClassReadVisitor
import org.jetbrains.kotlin.tools.kompot.ssg.SupersetGenerator
import org.jetbrains.kotlin.tools.kompot.ssg.visibility
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

class SupersetGeneratorProvider(private val factory: () -> SupersetGenerator) {
    val all = mutableMapOf<File, SupersetGenerator>()

    operator fun get(target: File): SupersetGenerator {
        return all.getOrPut(target, factory)
    }
}

val configuration = Configuration(loadParameterNamesFromLVT = true, writeParameters = false)

fun main(args: Array<String>) {
    val paths = args.map { Paths.get(it) }
    if (paths.size < 3) {
        System.err.println("Specify IDEs to merge and output: java IdeMerger <IDE #1> <IDE #2> ... <IDE #N> <output>")
        System.err.println("Example: java IdeMerger IU-173.4301 IU-181.2260 IU-super")
        exitProcess(1)
    }

    val idePaths = paths.dropLast(1)
    val outputPath = paths.last()

    val versionHandler = IdeMergedVersionHandler()
    val loader = IdeMergedVersionLoader()


    val provider = SupersetGeneratorProvider {
        SupersetGenerator(
            LoggerFactory.getLogger("IdeMerger"),
            versionHandler,
            loader,
            configuration
        )
    }
    for (idePath in idePaths) {
        appendIde(provider, idePath)
    }

    val outputDirFile = outputPath.toFile()

    provider.all.forEach { (_, gen) -> gen.merge() }

    provider.all.forEach { (file, superset) ->
        info("Writing $file")
        superset.doOutput(outputDirFile.resolve(file))
    }
}

private fun appendIde(supersetGeneratorProvider: SupersetGeneratorProvider, idePath: Path) {
    info("Reading classes of IDE $idePath")
    val ide = IdeManager.createManager().createIde(idePath.toFile())
    createIdeResolver(ide).use { ideResolver ->
        val ideMergedVersion = IdeMergedVersion(listOf(ide.version))
        info("Reading bundled plugins classes")
        val bundledPluginsClassesLocations = readBundledPluginsClassesLocations(ide)
        try {
            appendResolver(supersetGeneratorProvider[File("./libClasses")], ideResolver, ideMergedVersion)
            val pluginsDir = File("plugins")
            for (pluginClassesLocations in bundledPluginsClassesLocations) {
                val plugin = pluginClassesLocations.idePlugin
                val pluginName = plugin.originalFile?.nameWithoutExtension ?: plugin.pluginId ?: plugin.pluginName
                ?: error("Failed to acknowledge plugin name")
                appendResolver(
                    supersetGeneratorProvider[pluginsDir.resolve(pluginName).resolve("libClasses")],
                    pluginClassesLocations.getPluginClassesResolver(),
                    ideMergedVersion
                )
            }
        } finally {
            bundledPluginsClassesLocations.forEach { it.closeLogged() }
        }

    }
}

//replace with logger, if necessary.
private fun info(s: String) = println(s)

/**
 * Processes all the class files contained in the IDE and its bundled plugins
 * and merges them with the [supersetGenerator].
 */
private fun appendResolver(
    supersetGenerator: SupersetGenerator,
    resolver: Resolver,
    ideMergedVersion: IdeMergedVersion
) {
    resolver.processAllClasses { classNode ->
        if (classNode.name.startsWith("kotlin")) return@processAllClasses true

        val visitor =
            SSGClassReadVisitor(ideMergedVersion, loadParameterNamesFromLVT = configuration.loadParameterNamesFromLVT)
        classNode.accept(visitor)
        val result = visitor.result
        if (!(result.isMemberClass && result.visibility == Visibility.PACKAGE_PRIVATE)) {
            supersetGenerator.appendClassNode(result)
        }
        true
    }
}

/**
 * Creates a [Resolver] for accessing class files of the [ide].
 */
private fun createIdeResolver(ide: Ide) = IdeResolverCreator.createIdeResolver(ide)

/**
 * Specifies which classes should be put into the plugin's class files resolver.
 * Currently, we select all the classes from:
 * 1) for `.jar`-red plugin, all classes contained in the `.jar`
 * 2) for directory-based plugins, all classes from the `/lib/` directory and
 * from the `/classes` directory, if any
 * 3) JPS-used classes, such as `Kotlin/lib/jps`.
 */
private val pluginClassesLocationsKeys = IdePluginClassesFinder.MAIN_CLASSES_KEYS + listOf(CompileServerExtensionKey)

/**
 * Merges all the classes by different locations (`/lib/`, `/classes/`, etc) into
 * one resolver.
 */
private fun IdePluginClassesLocations.getPluginClassesResolver(): Resolver =
    pluginClassesLocationsKeys.mapNotNull { getResolver(it) }.let { UnionResolver.create(it) }

/**
 * Finds class files of all plugins bundled into the [ide].
 *
 * The [IdePluginClassesLocations] must be [closed] [IdePluginClassesLocations.close]
 * when no more required to free up possibly occupied disk space:
 * plugins might have been extracted to a temporary directory.
 */
private fun readBundledPluginsClassesLocations(ide: Ide): List<IdePluginClassesLocations> =
    ide.bundledPlugins.mapNotNull { safeFindPluginClasses(ide, it) }

private fun safeFindPluginClasses(ide: Ide, idePlugin: IdePlugin) = try {
    if (idePlugin.pluginId != "org.jetbrains.kotlin") {
        info("Reading class files of a plugin $idePlugin bundled into $ide")
        IdePluginClassesFinder.findPluginClasses(idePlugin, pluginClassesLocationsKeys)
    } else {
        null
    }
} catch (e: Exception) {
    info("Unable to read class files of a bundled plugin $idePlugin: ${e.message}")
    null
}

