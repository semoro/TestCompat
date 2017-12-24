package compat.process

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.*
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes


const val existsInDesc = "Lcompat/rt/ExistsIn;"
const val compatiblieWithDesc = "Lcompat/rt/CompatibleWith;"

open class VerifyCompatibility : DefaultTask() {


    @InputFiles
    var classpath: Iterable<File> = emptyList()
    @InputFiles
    var checkpath: Iterable<File> = emptyList()

    data class VersionData(val original: String) {

        fun allow(other: VersionData): Boolean {
            return false
        }
    }

    val versionParseCache = mutableMapOf<String, VersionData>()

    val classToVersionInfo = mutableMapOf<String, VersionData>()
    val fieldToVersionInfo = mutableMapOf<String, VersionData>()
    val methodToVersionInfo = mutableMapOf<String, VersionData>()

    fun parseVersionData(versionString: String): VersionData {
        return versionParseCache.getOrPut(versionString) { VersionData(versionString) }
    }

    inner class ClassCompatReadVisitor : ClassVisitor(Opcodes.ASM5) {

        fun doVisitAnnotation(desc: String?, out: (VersionData) -> Unit): AnnotationVisitor? {
            if (desc != existsInDesc && desc != compatiblieWithDesc) {
                return null
            }
            return object : AnnotationVisitor(Opcodes.ASM5) {
                override fun visit(name: String?, value: Any?) {
                    if (name == "version") {
                        out(parseVersionData(value as String))
                    }
                    super.visit(name, value)
                }
            }
        }

        lateinit var classFqName: String
        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            classFqName = name!!
            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return object : MethodVisitor(Opcodes.ASM5) {
                override fun visitAnnotation(annotationDesc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(annotationDesc) { methodToVersionInfo["$classFqName.$name $desc"] = it }
                }
            }
        }


        override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor {
            return object : FieldVisitor(Opcodes.ASM5) {
                override fun visitAnnotation(annotationDesc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(annotationDesc) { fieldToVersionInfo["$classFqName.$name $desc"] = it }
                }
            }
        }

        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
            return doVisitAnnotation(desc) { classToVersionInfo[classFqName.replace('/', '.')] = it }
        }
    }

    fun classpathToClassFiles(classpath: Iterable<File>): Sequence<Path> {
        val (dirs, files) = classpath.partition { it.isDirectory }
        val jars = files.filter { it.extension == "jar" }


        val dirClasses = dirs.asSequence().map {
            project.fileTree(it).include {
                it.isDirectory || it.name.endsWith(".class")
            } as FileTree
        }.flatten().map { it.toPath() }


        val jarClasses = jars.map { it.toPath() }.asSequence().map {
            FileSystems.newFileSystem(it, null).rootDirectories
        }.flatten().map {
            filteredFiles(it) { it.fileName.toString().endsWith("class") }
        }.flatten()

        return dirClasses + jarClasses
    }


    fun loadClasspathIndex() {
        val classes = classpathToClassFiles(classpath).map {
            println("R $it")
            Files.newInputStream(it)
        }

        classes.map {
            ClassReader(it)
        }.forEach {
            it.accept(ClassCompatReadVisitor(), SKIP_FRAMES or SKIP_CODE)
        }
    }

    inner class ClassCompatCheckingVisitor() : ClassVisitor(Opcodes.ASM5) {



        fun allowedType(type: Type, restriction: VersionData): Boolean {
            val typeVersion = classToVersionInfo[type.className] ?: return true
            return restriction.allow(typeVersion)
        }

        lateinit var className: String

        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            className = name!!.replace('/', '.')
            super.visit(version, access, name, signature, superName, interfaces)
        }

        var classLevelVersion: VersionData? = null
        fun doVisitAnnotation(annDesc: String?, out: (VersionData) -> Unit): AnnotationVisitor? {
            if (annDesc != compatiblieWithDesc) {
                return null
            }
            return object : AnnotationVisitor(Opcodes.ASM5) {
                override fun visit(name: String?, value: Any?) {
                    if (name == "version") {
                        out(parseVersionData(value as String))
                    }
                    super.visit(name, value)
                }
            }
        }

        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {

            return object : MethodVisitor(Opcodes.ASM5) {

                var methodLevelVersion: VersionData? = null
                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(desc) { methodLevelVersion = it }
                }


                override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
                    when {
                        name == "enterVersionScope" && owner == "compat/rt/ScopesKt" -> {
                            println()
                        }
                        name == "leaveVersionScope" && owner == "compat/rt/ScopesKt" -> {

                        }
                        else -> {}
                    }

                    super.visitMethodInsn(opcode, owner, name, desc, itf)
                }

                override fun visitEnd() {
                    val versionRestriction = methodLevelVersion ?: classLevelVersion
                    if (versionRestriction != null) {
                        val argTypes = Type.getArgumentTypes(desc) + Type.getReturnType(desc)
                        argTypes.forEach {
                            if (!allowedType(it, versionRestriction)) {
                                println("Version restriction violation! at $className.$name $desc")
                            }
                        }
                    }

                    super.visitEnd()
                }
            }
        }


        override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor {

            return object : FieldVisitor(Opcodes.ASM5) {
                var fieldLevelVersion: VersionData? = null
                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(desc) { fieldLevelVersion = it }
                }

                override fun visitEnd() {
                    val versionRestriction = fieldLevelVersion ?: classLevelVersion
                    if (versionRestriction != null) {
                        val returnType = Type.getType(desc)
                        if (!allowedType(returnType, versionRestriction)) {
                            println("Version restriction violation! at $className.$name $desc")
                        }
                    }
                }
            }
        }

        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
            return doVisitAnnotation(desc) { classLevelVersion = it }
        }
    }

    @TaskAction
    fun doCheck() {

        loadClasspathIndex()
        println(classToVersionInfo)

        val checkpathClasses = classpathToClassFiles(checkpath).map {
            println("V $it")
            Files.newInputStream(it)
        }

        checkpathClasses.map {
            ClassReader(it)
        }.forEach {
            it.accept(ClassCompatCheckingVisitor(), SKIP_FRAMES)
        }
    }
}


fun filteredFiles(root: Path, f: (Path) -> Boolean): List<Path> {
    val out = mutableListOf<Path>()
    Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (f(file)) {
                out.add(file)
            }
            return super.visitFile(file, attrs)
        }
    })
    return out
}