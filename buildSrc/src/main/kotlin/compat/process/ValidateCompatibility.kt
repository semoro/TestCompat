package compat.process

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.*
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*


const val existsInDesc = "Lcompat/rt/ExistsIn;"
const val compatiblieWithDesc = "Lcompat/rt/CompatibleWith;"


fun Type.formatForReport(): String {
    return when (sort) {
        Type.VOID -> "Unit"
        Type.ARRAY -> "Array<" + elementType.formatForReport() + ">"
        Type.INT -> "Int"
        Type.BOOLEAN -> "Boolean"
        Type.BYTE -> "Byte"
        Type.CHAR -> "Char"
        Type.DOUBLE -> "Double"
        Type.FLOAT -> "Float"
        Type.LONG -> "Long"
        Type.SHORT -> "Short"
        Type.OBJECT -> className
        Type.METHOD -> "(" + argumentTypes.joinToString { it.formatForReport() } + "): " + returnType.formatForReport()
        else -> error("Unsupported type kind")
    }
}

private fun VerifyCompatibility.VersionData?.formatForReport(ann: String = "@ExistsIn") = "$ann(${this?.original ?: "*"})"

open class VerifyCompatibility : DefaultTask() {


    @InputFiles
    var classpath: Iterable<File> = emptyList()
    @InputFiles
    var checkpath: Iterable<File> = emptyList()

    data class VersionData(val original: String)


    fun VersionData?.disallows(other: VersionData?): Boolean {
        return !this.allows(other)
    }

    fun VersionData?.allows(other: VersionData?): Boolean {
        if (other == null) return true
        if (this == null) return false
        return this.original == other.original
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
            logger.debug("R $it")
            Files.newInputStream(it)
        }

        classes.map {
            ClassReader(it)
        }.forEach {
            it.accept(ClassCompatReadVisitor(), SKIP_FRAMES or SKIP_CODE)
        }
    }

    data class SourceInfo(val at: String, val file: String, val line: String, val restriction: VersionData?) {
        override fun toString(): String {
            return "${restriction.formatForReport("@CompatibleWith")} '$at' ($file:$line)"
        }
    }


    sealed class ProblemDescriptor(val sourceData: SourceInfo) {

        class TypeReferenceProblem(val refType: Type, val refVersionData: VersionData?, sourceData: SourceInfo) : ProblemDescriptor(sourceData) {
            override fun toString(): String {
                return "TR ${refVersionData.formatForReport()} '${refType.formatForReport()}' at $sourceData"
            }
        }

        class MethodReferenceProblem(val refType: Type, val refMethod: String, val refMethodType: Type, val refVersionData: VersionData?, sourceData: SourceInfo) : ProblemDescriptor(sourceData) {
            override fun toString(): String {
                return "MR ${refVersionData.formatForReport()} '${refType.formatForReport()}.$refMethod${refMethodType.formatForReport()}' at $sourceData"
            }
        }

        class FieldReferenceProblem(val refType: Type, val refField: String, val refFieldType: Type, val refVersionData: VersionData?, sourceData: SourceInfo) : ProblemDescriptor(sourceData) {
            override fun toString(): String {
                return "FR ${refVersionData.formatForReport()} '${refType.formatForReport()}.$refField: ${refFieldType.formatForReport()}' at $sourceData"
            }
        }
    }

    inner class ClassCompatCheckingVisitor(val deepMethodAnalysis: Boolean, val problemSink: (ProblemDescriptor) -> Unit) : ClassVisitor(Opcodes.ASM5) {

//        fun allowedType(type: Type, restriction: VersionData?): Boolean {
//            val typeVersion = classToVersionInfo[type.className] ?: return true
//            return restriction?.allow(typeVersion) == true
//        }

        lateinit var className: String

        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            className = name!!.replace('/', '.')
            super.visit(version, access, name, signature, superName, interfaces)
        }

        var source: String? = null

        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, debug)
            this.source = source
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
            val methodNode = MethodNode(access, name, desc, signature, exceptions)
            return object : MethodVisitor(Opcodes.ASM5, methodNode) {
                var methodLevelVersion: VersionData? = null
                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(desc) { methodLevelVersion = it }
                }

                val versionScopes = ArrayDeque<VersionData>()

                var currentScope: VersionData? = null

                fun traceUpToVersionConst(insnNode: AbstractInsnNode): String? {
                    val prevNode = insnNode.previous
                    when {
                        prevNode is LdcInsnNode -> return prevNode.cst as String
                        prevNode is VarInsnNode && prevNode.opcode == Opcodes.ALOAD -> {
                            var node = prevNode.previous
                            while (node !is VarInsnNode || node.opcode != Opcodes.ASTORE || node.`var` != prevNode.`var`) {
                                node = node.previous ?: return null
                            }
                            return traceUpToVersionConst(node)
                        }
                        else -> return traceUpToVersionConst(prevNode)
                    }
                }

                override fun visitCode() {
                    currentScope = methodLevelVersion ?: classLevelVersion
                    super.visitCode()
                }

                var firstLine = -1
                var lineNumber = 0
                override fun visitLineNumber(line: Int, start: Label?) {
                    super.visitLineNumber(line, start)
                    lineNumber = line
                    if (firstLine == -1) {
                        firstLine = line
                    }
                }

                fun getSourceInfo(line: Int = lineNumber): SourceInfo {
                    return SourceInfo("$className.$name${Type.getMethodType(desc).formatForReport()}", source!!, "$line", currentScope)
                }

                override fun visitMethodInsn(opcode: Int, mowner: String?, mname: String?, mdesc: String?, itf: Boolean) {
                    super.visitMethodInsn(opcode, mowner, mname, mdesc, itf)

                    if (mowner == "compat/rt/ScopesKt") {
                        if (mname == "enterVersionScope") {
                            val versionString = traceUpToVersionConst(methodNode.instructions.last) ?: return println("WARN: Unresolved scope")
                            val vd = parseVersionData(versionString)
                            versionScopes.add(vd)
                            currentScope = vd
                        } else if (mname == "leaveVersionScope") {
                            versionScopes.remove()
                            currentScope = if (versionScopes.isEmpty()) {
                                methodLevelVersion ?: classLevelVersion
                            } else {
                                versionScopes.peek()
                            }
                        }
                    } else {
                        val calleeVersionInfo = methodToVersionInfo["$mowner.$mname $mdesc"] ?: return
                        val calleeOwnerType = Type.getObjectType(mowner)
                        val calleeMethodType = Type.getMethodType(mdesc)
                        if (currentScope.disallows(calleeVersionInfo)) {
                            problemSink(ProblemDescriptor.MethodReferenceProblem(calleeOwnerType, mname!!, calleeMethodType, calleeVersionInfo, getSourceInfo()))
                        }
                    }
                }

                override fun visitFieldInsn(opcode: Int, fowner: String?, fname: String?, fdesc: String?) {
                    super.visitFieldInsn(opcode, fowner, fname, fdesc)
                    val fieldVersionInfo = fieldToVersionInfo["$fowner.$fname $fdesc"]
                    if (currentScope.disallows(fieldVersionInfo)) {
                        val fieldOwnerType = Type.getType(fowner)
                        val fieldType = Type.getType(fdesc)
                        problemSink(ProblemDescriptor.FieldReferenceProblem(fieldOwnerType, fname!!, fieldType, fieldVersionInfo, getSourceInfo()))
                    }
                }

                override fun visitTypeInsn(opcode: Int, typeName: String?) {
                    super.visitTypeInsn(opcode, typeName)
                    val type = Type.getObjectType(typeName)
                    val restriction = classToVersionInfo[type.className]
                    if (currentScope.disallows(restriction)) {
                        problemSink(ProblemDescriptor.TypeReferenceProblem(type, restriction, getSourceInfo()))
                    }
                }

                override fun visitEnd() {
                    super.visitEnd()

                    val versionRestriction = methodLevelVersion ?: classLevelVersion
                    val argTypes = Type.getArgumentTypes(desc) + Type.getReturnType(desc)
                    argTypes.forEach {
                        val restriction = classToVersionInfo[it.className]
                        if (currentScope.disallows(restriction)) {
                            problemSink(ProblemDescriptor.TypeReferenceProblem(it, restriction, getSourceInfo(firstLine)))
                        }
                    }
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
                    val returnType = Type.getType(desc)
                    val restriction = classToVersionInfo[returnType.className]
                    if (versionRestriction.disallows(restriction)) {
                        problemSink(ProblemDescriptor.TypeReferenceProblem(returnType, restriction, SourceInfo("$className.$name: ${returnType.formatForReport()}", source!!, "?", versionRestriction)))
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
        logger.info("Classes = $classToVersionInfo")
        logger.info("Methods = $methodToVersionInfo")
        logger.info("Fields = $fieldToVersionInfo")

        val checkpathClasses = classpathToClassFiles(checkpath).map {
            logger.debug("V $it")
            Files.newInputStream(it)
        }

        val problems = mutableListOf<ProblemDescriptor>()
        checkpathClasses.map {
            ClassReader(it)
        }.forEach {
            it.accept(ClassCompatCheckingVisitor(true, { problems.add(it) }), SKIP_FRAMES)
        }

        problems.forEach { logger.error(it.toString()) }
        if (problems.isNotEmpty()) {
            throw GradleException("Compatibility verification failure. See log for more details")
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