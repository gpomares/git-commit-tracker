package com.gpomares.committracker.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.gpomares.committracker.services.RepositoryDetectionService

class CommitTrackerToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val commitTrackerPanel = CommitTrackerPanel(project)
        val content = contentFactory.createContent(commitTrackerPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        // Only show tool window if project has Git repositories
        val repoService = project.service<RepositoryDetectionService>()
        return repoService.hasGitRepositories()
    }
}
