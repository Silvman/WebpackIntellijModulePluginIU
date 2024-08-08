package me.blep.intellij.plugin.webpackmodule.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import me.blep.intellij.plugin.webpackmodule.WebpackIntellijModuleConfig

class UpdateConfigAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project != null) {
            val project = e.project!!
            WebpackIntellijModuleConfig.INSTANCE.init(project)
            WebpackIntellijModuleConfig.INSTANCE.updateProject(project, true)
        }
    }

}