package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.test.classpathToClassFiles
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.io.File

class TestSupersetGenerator {

    val logger = LoggerFactory.getLogger(javaClass)
    lateinit var gen: SupersetGenerator

    val tmpDir = TemporaryFolder()

    @Before
    fun setup() {
        gen = SupersetGenerator(logger, SimpleTestVersionHandler())
    }

    fun appendClasses(roots: List<String>, version: Version) {
        val classes = classpathToClassFiles(roots.map { File("test-data/build/libs/$it") })
        val reader = ClassToIRReader(classes, version)
        gen.appendClasses(reader.classes)
    }

    @Test
    fun testSimple() {
        appendClasses(listOf("testSimple1.jar"), SimpleTestVersion(setOf("1")))
        appendClasses(listOf("testSimple2.jar"), SimpleTestVersion(setOf("2")))
        tmpDir.create()
        gen.doOutput(tmpDir.root)
        val verifier = TestClassVerifier()
        verifier.verifyOut(tmpDir.root, File("test-data/expected/simple"))
    }
}