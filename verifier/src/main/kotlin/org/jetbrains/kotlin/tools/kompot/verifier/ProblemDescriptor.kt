package org.jetbrains.kotlin.tools.kompot.verifier

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.formatForReport
import org.objectweb.asm.Type




sealed class ProblemDescriptor(val sourceData: SourceInfo) {

    data class SourceInfo(val at: String, val file: String, val line: String, val restriction: Version?) {
        override fun toString(): String {
            return "${restriction.formatForReport("@CompatibleWith")} '$at' ($file:$line)"
        }
    }


    class TypeReferenceProblem(val refType: Type, val refVersionData: Version?, sourceData: SourceInfo) : ProblemDescriptor(sourceData) {
        override fun toString(): String {
            return "TR ${refVersionData.formatForReport()} '${refType.formatForReport()}' at $sourceData"
        }
    }

    class MethodReferenceProblem(val refType: Type, val refMethod: String, val refMethodType: Type, val refVersionData: Version?, sourceData: SourceInfo) : ProblemDescriptor(sourceData) {
        override fun toString(): String {
            return "MR ${refVersionData.formatForReport()} '${refType.formatForReport()}.$refMethod${refMethodType.formatForReport()}' at $sourceData"
        }
    }

    class FieldReferenceProblem(val refType: Type, val refField: String, val refFieldType: Type, val refVersionData: Version?, sourceData: SourceInfo) : ProblemDescriptor(sourceData) {
        override fun toString(): String {
            return "FR ${refVersionData.formatForReport()} '${refType.formatForReport()}.$refField: ${refFieldType.formatForReport()}' at $sourceData"
        }
    }
}