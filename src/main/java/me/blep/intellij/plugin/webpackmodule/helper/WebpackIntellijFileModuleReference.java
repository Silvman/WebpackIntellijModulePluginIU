package me.blep.intellij.plugin.webpackmodule.helper;

import com.intellij.lang.typescript.modules.TypeScriptFileModuleReference;
import com.intellij.lang.typescript.tsconfig.TypeScriptConfig;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.util.PsiUtilCore;
import me.blep.intellij.plugin.webpackmodule.WebpackIntellijModuleConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;

public class WebpackIntellijFileModuleReference extends TypeScriptFileModuleReference {
    public WebpackIntellijFileModuleReference(@NotNull String text, @Nullable TypeScriptConfig config, int i, @NotNull TextRange textRange, @NotNull FileReferenceSet refSet, @NotNull ModuleTypes moduleType) {
        super(text, config, i, textRange, refSet, moduleType);
    }

    @Override
    protected void processResolveInContext(@NotNull String referenceText, @NotNull PsiFileSystemItem context, Collection<ResolveResult> results, boolean caseSensitive) {
        WebpackIntellijModuleFileReferenceSet fileReferenceSet = (WebpackIntellijModuleFileReferenceSet) this.getFileReferenceSet();
        PsiElement element = this.getFileReferenceSet().getElement();
        String pathString = fileReferenceSet.getPathString();
        String dependencyPath = WebpackIntellijModuleConfig.Companion.getINSTANCE().readWebpackModuleConfDependencyPath(
                element, pathString
        );

        if (dependencyPath != null && pathString.endsWith(referenceText)) {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(dependencyPath));
            if (virtualFile != null) {
                PsiDirectory directory = context.getManager().findDirectory(virtualFile);
                if (directory != null) {
                    Collection<ResolveResult> resolveResults = resolveDirectory(directory);
                    results.addAll(resolveResults);
                }
            }
        }

        if (results.isEmpty()) {
            super.processResolveInContext(referenceText, context, results, caseSensitive);
        }
    }
}
