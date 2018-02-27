package org.jetbrains.kotlin.tools.kompot.postprocess

import org.jetbrains.kotlin.tools.kompot.commons.classpathToClassFiles
import org.jetbrains.kotlin.tools.kompot.test.SimpleTestVersion
import org.jetbrains.kotlin.tools.kompot.test.SimpleTestVersionLoader
import org.jetbrains.kotlin.tools.kompot.test.TestClassVerifier
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Files


class SimpleStripPostProcessTest {

    val verifier = TestClassVerifier()

    companion object {

        @get:ClassRule
        @JvmStatic
        val tmpDir = TemporaryFolder()

        @BeforeClass
        @JvmStatic
        fun setup() {
            val versionLoader = SimpleTestVersionLoader()
            val postProcessor = StripPostProcessor(versionLoader) {
                it as? SimpleTestVersion ?: return@StripPostProcessor true
                it.s == setOf("2")
            }

            postProcess("testSimple.jar", postProcessor)
        }


        private fun postProcess(jarName: String, postProcessor: StripPostProcessor) {

            tmpDir.create()
            val inFile = File("test-data/build/libs/", jarName)
            val classpath = classpathToClassFiles(listOf(inFile))

            classpath.map {
                Files.newInputStream(it)
            }.map { input ->
                input.use {
                    val reader = ClassReader(input)
                    val node = ClassNode()
                    reader.accept(node, 0)

                    node
                }
            }.forEach { node ->
                val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                var classWritten = false
                node.accept(postProcessor.createVisitor(object : ClassVisitor(Opcodes.ASM5, writer) {
                    override fun visitEnd() {
                        super.visitEnd()
                        classWritten = true
                    }
                }))
                val outFile = tmpDir.root.resolve(File("${node.name}.class"))
                outFile.parentFile.mkdirs()

                if (classWritten) {
                    outFile.writeBytes(writer.toByteArray())
                }
            }
        }
    }

    private fun verifyOut(filePath: String, withBodies: Boolean = true) {
        val testDataDir = File("test-data/expected/simple")
        val actualFile = File(tmpDir.root, filePath)
        verifier.verifyOut(tmpDir.root, actualFile, testDataDir, !withBodies)
    }

    @Test
    fun testScopes() {
        verifyOut("p/Scopes.class")
    }


    @Test
    fun testClassCompatibleWith() {
        verifyOut("p/ClassCompatibleWith.class")
    }

    @Test
    fun testCompatibleWithMembers() {
        verifyOut("p/CompatibleWithMembers.class", withBodies = false)
    }
}