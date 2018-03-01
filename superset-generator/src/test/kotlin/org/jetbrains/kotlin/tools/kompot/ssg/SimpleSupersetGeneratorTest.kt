package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.classpathToClassFiles
import org.jetbrains.kotlin.tools.kompot.test.SimpleTestVersion
import org.jetbrains.kotlin.tools.kompot.test.SimpleTestVersionHandler
import org.jetbrains.kotlin.tools.kompot.test.TestClassVerifier
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.io.File

class SimpleSupersetGeneratorTest {

    companion object {

        val logger = LoggerFactory.getLogger(SimpleSupersetGeneratorTest::class.java)

        private val configuration = Configuration(
            writeParameters = true,
            loadParameterNamesFromLVT = true,
            writeParametersToLVT = true
        )

        @get:ClassRule
        @JvmStatic
        val tmpDir = TemporaryFolder()

        @BeforeClass
        @JvmStatic
        fun setup() {
            val gen = SupersetGenerator(logger,
                SimpleTestVersionHandler(), configuration)
            gen.appendClasses(listOf("testSimple1.jar"),
                SimpleTestVersion(setOf("1"))
            )
            gen.appendClasses(listOf("testSimple2.jar"),
                SimpleTestVersion(setOf("2"))
            )
            tmpDir.create()
            gen.doOutput(tmpDir.root)
        }

        fun SupersetGenerator.appendClasses(roots: List<String>, version: Version) {
            val classes = classpathToClassFiles(roots.map { File("test-data/build/libs/$it") })
            val reader = ClassToIRReader(classes, version, configuration)
            appendClasses(reader.classes)
        }
    }

    private val verifier = TestClassVerifier()

    private fun verifyOut(filePath: String, withBodies: Boolean = false) {
        val testDataDir = File("test-data/expected/simple")
        val actualFile = File(tmpDir.root, filePath)
        verifier.verifyOut(tmpDir.root, actualFile, testDataDir, !withBodies)
    }

    @Test
    fun testSimple() {
        verifyOut("p/A.class")
    }

    @Test
    fun testAlternativeModality() {
        verifyOut("p/WasAbstract.class")
    }

    @Test
    fun testAnnotated() {
        verifyOut("p/Annotated.class")
    }

    @Test
    fun testMyAnnotation() {
        verifyOut("p/MyAnnotation.class")
    }

    @Test
    fun testNullability() {
        verifyOut("p/WithNullability.class")
    }

    @Test
    fun testParameterNameToLVT() {
        verifyOut("p/ParameterNameToLVT.class", withBodies = true)
    }


    @Test
    fun testEmptyParameterMerging() {
        verifyOut("p/AbstractOne.class")
    }

    @Test
    fun testTypeParameters() {
        verifyOut("p/TypeParameters.class")
    }

    @Test
    fun testSuperclassMismatch() {
        verifyOut("p/SuperclassMismatch.class")
    }

    @Test
    fun testInterfacesMismatch() {
        verifyOut("p/InterfacesMismatch.class")
    }
}