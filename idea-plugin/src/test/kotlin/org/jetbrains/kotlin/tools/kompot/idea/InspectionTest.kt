package org.jetbrains.kotlin.tools.kompot.idea

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.uast.toUElement

class InspectionTest : LightCodeInsightFixtureTestCase() {


    fun testA() {

        val forName = Class.forName("org.jetbrains.kotlin.idea.PluginStartupComponent")

        println("forName = $forName")


        val psiFile = myFixture.addFileToProject("annotations.kt", """
            package org.jetbrains.kotlin.tools.kompot.api.annotations

            annotation class ExistsIn(val version: String)

            annotation class CompatibleWith(val version: String)
        """.trimIndent())

        myFixture.addFileToProject("sourceApi.kt", """
            package org.jetbrains.kotlin.tools.kompot.api.source


            inline fun <T> forVersion(v: String, l: () -> T): T? = l()
        """.trimIndent())


        println("psiFile = " + psiFile + " of " + psiFile.javaClass)
        println("uFile = " + psiFile.toUElement() )


    }

}