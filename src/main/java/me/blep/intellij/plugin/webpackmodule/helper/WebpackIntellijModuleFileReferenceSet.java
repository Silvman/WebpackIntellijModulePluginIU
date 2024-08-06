package me.blep.intellij.plugin.webpackmodule.helper;

import com.intellij.lang.typescript.modules.TypeScriptModuleFileReferenceSet;
import com.intellij.lang.typescript.tsconfig.TypeScriptConfig;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebpackIntellijModuleFileReferenceSet extends TypeScriptModuleFileReferenceSet {
    public WebpackIntellijModuleFileReferenceSet(@NotNull String reference, @Nullable TypeScriptConfig configForFile, @NotNull PsiElement element, int startInElement, @Nullable PsiReferenceProvider provider, FileType @Nullable [] suitableFileTypes) {
        super(reference, configForFile, element, startInElement, provider, suitableFileTypes);
    }
}
