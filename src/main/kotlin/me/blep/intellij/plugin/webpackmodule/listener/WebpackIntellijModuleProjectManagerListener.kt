package me.blep.intellij.plugin.webpackmodule.listener

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import me.blep.intellij.plugin.webpackmodule.WebpackIntellijModuleConfig

class WebpackIntellijModuleProjectManagerListener: ProjectManagerListener {
    override fun projectOpened(project: Project) {
        WebpackIntellijModuleConfig.INSTANCE.init(project)
        WebpackIntellijModuleConfig.INSTANCE.updateProject(project)
    }

}