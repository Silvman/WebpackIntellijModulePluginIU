package me.blep.intellij.plugin.webpackmodule.listener

import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import me.blep.intellij.plugin.webpackmodule.WebpackIntellijModuleConfig

class WebpackIntellijModulePackageJsonChangeListener: PackageJsonFileManager.PackageJsonChangeListener {

    override fun onChange(var1: PackageJsonFileManager.PackageJsonChangeEvent) {
        for (openProject in ProjectManager.getInstance().openProjects) {
            val inContent = ProjectFileIndex.getInstance(openProject).isInContent(var1.file)
            if (inContent) {
                WebpackIntellijModuleConfig.INSTANCE.updateProject(openProject)
            }
        }
    }

}