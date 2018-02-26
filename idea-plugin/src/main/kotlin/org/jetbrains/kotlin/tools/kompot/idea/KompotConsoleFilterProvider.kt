package org.jetbrains.kotlin.tools.kompot.idea

import com.intellij.execution.filters.ConsoleFilterProviderEx
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.HyperlinkInfoFactory
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.FileIndexUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class KompotConsoleFilterProvider : ConsoleFilterProviderEx {
    override fun getDefaultFilters(project: Project, scope: GlobalSearchScope): Array<Filter> {
        return arrayOf(KompotConsoleFilter(project, scope))
    }

    override fun getDefaultFilters(project: Project): Array<Filter> {
        return getDefaultFilters(project, GlobalSearchScope.allScope(project))
    }

}

private class KompotConsoleFilter(val project: Project, val scope: GlobalSearchScope) : Filter {
    override fun applyFilter(line: String?, entireLength: Int): Filter.Result? {
        line ?: return null
        if (reportStartPrefixes.none { line.startsWith(it) }) return null
        val matches = sourcePattern.findAll(line)

        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)
        val offset = entireLength - line.length

        val results = matches.map { match ->
            val (fileName, lineNumber) = match.destructured
            val factory = HyperlinkInfoFactory.getInstance()

            val files = FilenameIndex.getFilesByName(project, fileName, scope).map { it.virtualFile }

            val info = factory.createMultipleFilesHyperlinkInfo(files, lineNumber.toInt(), project)

            Filter.ResultItem(offset + match.range.start + 1, offset + match.range.last, info, attrs)
        }
        return Filter.Result(results.toList())
    }

    companion object {
        private val sourcePattern = "\\(([\\w,\\s-.]+):(\\d+)\\)".toRegex()

        private val reportStartPrefixes = setOf("TR", "MR", "FR")
    }
}
