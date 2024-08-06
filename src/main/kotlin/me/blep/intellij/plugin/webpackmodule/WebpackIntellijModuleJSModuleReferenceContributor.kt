package me.blep.intellij.plugin.webpackmodule

import com.intellij.javascript.JSModuleBaseReference
import com.intellij.lang.javascript.frameworks.modules.JSDefaultFileReferenceCompletionFilter
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.resolve.JSModuleReferenceContributor
import com.intellij.lang.typescript.modules.TypeScriptFileModuleReference
import com.intellij.lang.typescript.modules.TypeScriptModuleFileReferenceSet
import com.intellij.lang.typescript.modules.TypeScriptModuleReferenceContributor
import com.intellij.lang.typescript.tsconfig.TypeScriptConfig
import com.intellij.lang.typescript.tsconfig.TypeScriptConfigService
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.util.PsiUtilCore
import me.blep.intellij.plugin.webpackmodule.helper.WebpackIntellijFileModuleReference
import me.blep.intellij.plugin.webpackmodule.helper.WebpackIntellijModuleFileReferenceSet

class WebpackIntellijModuleJSModuleReferenceContributor : TypeScriptModuleReferenceContributor() {
    override fun isApplicable(host: PsiElement): Boolean {
        return super.isApplicable(host) && WebpackIntellijModuleConfig.INSTANCE.useWebpackIntellijModule(host)
    }

    override fun getDefaultWeight(): Int {
        return 100;
    }

    override fun getReferences(unquotedRefText: String, host: PsiElement, offset: Int, provider: PsiReferenceProvider?, isCommonJS: Boolean): Array<PsiReference> {
        val readWebpackModuleConfDependencyPath = WebpackIntellijModuleConfig.INSTANCE.readWebpackModuleConfDependencyPath(host, unquotedRefText)
        if (readWebpackModuleConfDependencyPath != null) {
            val path = JSModuleReferenceContributor.getActualPath(unquotedRefText)

            val modulePath = path.second as String
            val resourcePathStartInd = path.first as Int
            val index = resourcePathStartInd + offset
            val service = TypeScriptConfigService.Provider.get(host.project)
            val configForFile = service.getPreferableOrParentConfig(PsiUtilCore.getVirtualFile(host))

            val value = object : WebpackIntellijModuleFileReferenceSet(modulePath, configForFile, host, index, provider, FileType.EMPTY_ARRAY) {
                override fun createFileReference(textRange: TextRange, i: Int, text: String): FileReference? {
                    return WebpackIntellijFileModuleReference(text, configForFile, i, textRange, this, JSModuleBaseReference.ModuleTypes.PATH_MAPPING)
                }
            }

            return value.allReferences.map { fileReference -> fileReference as PsiReference }.toTypedArray()
        }

        return emptyArray()
    }


}