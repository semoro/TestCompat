package org.jetbrains.kotlin.tools.kompot.idea

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

@TestDataPath("\$CONTENT_ROOT/../../testData")
class InspectionTest : LightCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String {
        return "./testData"
    }

    fun testA() {

        myFixture.enableInspections(KompotCompatibleCallInspection::class.java)

        myFixture.copyFileToProject("org/jetbrains/kotlin/tools/kompot/api/annotations/annotations.kt")
        myFixture.copyFileToProject("org/jetbrains/kotlin/tools/kompot/api/source/sourceApi.kt")
        myFixture.copyFileToProject("api/SomeApi.java")

        myFixture.testHighlighting("api/SomeApi.java")


        myFixture.addFileToProject(
            "pkg/Usage.kt", """
            import org.jetbrains.kotlin.tools.kompot.api.annotations.*
            import org.jetbrains.kotlin.tools.kompot.api.source.*

            class Usage{

                @CompatibleWith("173")
                fun bar(sapi: api.SomeApi){
                    sapi.foo()
                    sapi.<error>foo</error>(1)
                }

                @CompatibleWith("174")
                fun bar2(sapi: api.SomeApi){
                    sapi.<error>foo</error>()
                    sapi.foo(1)
                }

            }
        """.trimIndent())

        myFixture.testHighlighting("pkg/Usage.kt")


    }

}