package me.blep.intellij.plugin.webpackmodule.listener

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import me.blep.intellij.plugin.webpackmodule.WebpackIntellijModuleConfig

class WebpackIntellijModuleModuleListener: ModuleListener {

    override fun moduleAdded(project: Project, module: Module) {
        WebpackIntellijModuleConfig.INSTANCE.updateProject(project)
    }

    override fun moduleRemoved(project: Project, module: Module) {
        WebpackIntellijModuleConfig.INSTANCE.updateProject(project)
    }

}