package org.jetbrains.kotlin.tools.kompot.idea

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteral
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.toUElementOfType

class KompotCompatibleCallInspection : LocalInspectionTool() {


    private fun processCallExpression(holder: ProblemsHolder, call: UCallExpression) {
        println("processCallExpression $call")
        val method = call.resolve() ?: return
        println("called method: $method")
        val existsIn =
            method.modifierList.findAnnotation("org.jetbrains.kotlin.tools.kompot.api.annotations.ExistsIn") ?: return
        val version = (existsIn.findAttributeValue("version") as? PsiLiteral)?.value as? String ?: return
        println("version = $version")
        val compatibleWith =
            call.getContainingUMethod()?.findAnnotation("org.jetbrains.kotlin.tools.kompot.api.annotations.CompatibleWith")
                    ?: return
        val compatible = compatibleWith.findAttributeValue("version")?.evaluateString()
        println("compatible = $compatible")
        if (compatible != version) {
            val psiElement = call.methodIdentifier?.sourcePsi ?: return
            holder.registerProblem(
                psiElement,
                "Kompot incompatible call (expected version is '$version' but given is '$compatible')",
                ProblemHighlightType.GENERIC_ERROR
            )
        }
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