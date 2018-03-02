package org.jetbrains.kotlin.tools.kompot.idea

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteral
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader
import org.jetbrains.uast.*


private const val COMPATIBLE_WITH = "org.jetbrains.kotlin.tools.kompot.api.annotations.CompatibleWith"

private const val EXISTS_IN = "org.jetbrains.kotlin.tools.kompot.api.annotations.ExistsIn"

class KompotCompatibleCallInspection : LocalInspectionTool() {


    private fun processCallExpression(holder: ProblemsHolder, call: UCallExpression) {
        println("processCallExpression $call")

        val module = call.sourcePsiElement?.let { ModuleUtilCore.findModuleForPsiElement(it) } ?: return
        val versionLoader = ModuleServiceManager.getService(module, VersionLoader::class.java) ?: return

        val expectedVersionString = getExpectedVersion(call)
        val specifiedVersionString = getCompatibleVersionString(call)

        val version = versionLoader.load(expectedVersionString)

        if (specifiedVersionString == null && expectedVersionString != null) {
            val psiElement = call.methodIdentifier?.sourcePsi ?: return
            holder.registerProblem(
                psiElement,
                "Kompot compatible version is not specified (expected  '${version.asLiteralValue()}'')",
                ProblemHighlightType.GENERIC_ERROR
            )
            return
        }
        val specifiedVersion = versionLoader.load(specifiedVersionString)
        if (!version.contains(specifiedVersion)) {
            val psiElement = call.methodIdentifier?.sourcePsi ?: return
            holder.registerProblem(
                psiElement,
                "Kompot incompatible call (expected version is '${version.asLiteralValue()}' but given is '${specifiedVersion.asLiteralValue()}')",
                ProblemHighlightType.GENERIC_ERROR
            )
        }
    }

    private fun getExpectedVersion(call: UCallExpression): String? {
        val method = call.resolve() ?: return null

        val existsIn = method.modifierList
            .let { it.findAnnotation(EXISTS_IN) ?: it.findAnnotation(COMPATIBLE_WITH) }
                ?: return null

        return (existsIn.findAttributeValue("version") as? PsiLiteral)?.value as? String
    }

    private fun getCompatibleVersionString(call: UCallExpression): String? {
        for (element in call.withContainingElements) {
            when (element) {
                is UMethod -> return element
                    .findAnnotation(COMPATIBLE_WITH)
                    ?.findAttributeValue("version")?.evaluateString()
                is UCallExpression ->
                    if (element.resolve()?.let { it.name == "forVersion" && it.containingClass?.qualifiedName == "org.jetbrains.kotlin.tools.kompot.api.source.SourceApiKt" } == true) {
                    return element.valueArguments.firstOrNull()?.evaluateString()
                }
            }
        }
        return null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {

            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                element.toUElementOfType<UCallExpression>()?.let { processCallExpression(holder, it) }
            }
        }
    }
}