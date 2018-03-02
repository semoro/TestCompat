package org.jetbrains.kotlin.tools.kompot.idea

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

@TestDataPath("\$CONTENT_ROOT/../../testData")
class InspectionTest : LightCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String {
        return "./testData"
    }


    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(KompotCompatibleCallInspection::class.java)

        myFixture.copyFileToProject("org/jetbrains/kotlin/tools/kompot/api/annotations/annotations.kt")
        myFixture.copyFileToProject("org/jetbrains/kotlin/tools/kompot/api/source/sourceApi.kt")
        myFixture.copyFileToProject("api/SomeApi.java")

        myFixture.testHighlighting("api/SomeApi.java")
    }

    fun testAnnotation() {
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
        """.trimIndent()
        )

        myFixture.testHighlighting("pkg/Usage.kt")


    }

    fun testForVersion() {
        myFixture.addFileToProject(
            "pkg/Usage.kt", """
            import org.jetbrains.kotlin.tools.kompot.api.annotations.*
            import org.jetbrains.kotlin.tools.kompot.api.source.*

            class Usage{

                fun bar(sapi: api.SomeApi){
                    forVersion("173"){
                        sapi.foo()
                        sapi.<error>foo</error>(1)
                    }
                }

            }
        """.trimIndent()
        )

        myFixture.testHighlighting("pkg/Usage.kt")


    }


    fun testNotMarked() {
        myFixture.addFileToProject(
            "pkg/Usage.kt", """
            import org.jetbrains.kotlin.tools.kompot.api.annotations.*
            import org.jetbrains.kotlin.tools.kompot.api.source.*

            class Usage{

                fun bar2(sapi: api.SomeApi){
                    sapi.<error>foo</error>()
                    sapi.<error>foo</error>(1)
                }

            }
        """.trimIndent()
        )

        myFixture.testHighlighting("pkg/Usage.kt")


    }

    fun testTransitionalUsage() {
        myFixture.addFileToProject(
            "pkg/Usage.kt", """
            import org.jetbrains.kotlin.tools.kompot.api.annotations.*
            import org.jetbrains.kotlin.tools.kompot.api.source.*

            class Usage1{

                @CompatibleWith("173")
                fun bar(sapi: api.SomeApi){
                    sapi.foo()
                }

                fun compatibleWithAll(){}

            }

             class Usage2{

                fun bar(u1: Usage1, sapi: api.SomeApi){
                    u1.<error>bar</error>(sapi)
                    u1.compatibleWithAll()
                    forVersion("173"){
                        u1.bar(sapi)
                    }
                }

            }
        """.trimIndent())

        myFixture.testHighlighting("pkg/Usage.kt")


    }

}