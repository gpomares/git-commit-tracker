package com.gpomares.committracker.utils

import com.intellij.openapi.project.Project
import git4idea.config.GitConfigUtil
import git4idea.repo.GitRepositoryManager

object GitConfigHelper {

    /**
     * Get the current Git user name from the project's Git config
     */
    fun getCurrentUserName(project: Project): String? {
        return try {
            val repos = GitRepositoryManager.getInstance(project).repositories
            repos.firstOrNull()?.let { repo ->
                GitConfigUtil.getValue(project, repo.root, "user.name")
            } ?: System.getProperty("user.name")
        } catch (e: Exception) {
            System.getProperty("user.name")
        }
    }

    /**
     * Get the current Git user email from the project's Git config
     */
    fun getCurrentUserEmail(project: Project): String? {
        return try {
            val repos = GitRepositoryManager.getInstance(project).repositories
            repos.firstOrNull()?.let { repo ->
                GitConfigUtil.getValue(project, repo.root, "user.email")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a commit author matches the current user
     */
    fun isCurrentUser(project: Project, authorName: String, authorEmail: String): Boolean {
        val userName = getCurrentUserName(project)
        val userEmail = getCurrentUserEmail(project)

        return when {
            userName != null && authorName.equals(userName, ignoreCase = true) -> true
            userEmail != null && authorEmail.equals(userEmail, ignoreCase = true) -> true
            else -> false
        }
    }
}
