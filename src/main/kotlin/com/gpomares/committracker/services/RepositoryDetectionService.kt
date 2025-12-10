package com.gpomares.committracker.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.gpomares.committracker.models.RepositoryInfo
import git4idea.repo.GitRepositoryManager

@Service(Service.Level.PROJECT)
class RepositoryDetectionService(private val project: Project) {

    private val repositoryCache = mutableListOf<RepositoryInfo>()

    /**
     * Detect all Git repositories in the current project
     */
    fun detectRepositories(): List<RepositoryInfo> {
        val gitRepositoryManager = GitRepositoryManager.getInstance(project)
        val repositories = gitRepositoryManager.repositories

        repositoryCache.clear()
        repositoryCache.addAll(repositories.map { gitRepo ->
            RepositoryInfo(
                name = gitRepo.root.name,
                path = gitRepo.root.path,
                currentBranch = gitRepo.currentBranchName ?: "detached",
                root = gitRepo.root
            )
        })

        return repositoryCache.toList()
    }

    /**
     * Check if the project has any Git repositories
     */
    fun hasGitRepositories(): Boolean {
        val gitRepositoryManager = GitRepositoryManager.getInstance(project)
        return gitRepositoryManager.repositories.isNotEmpty()
    }

    /**
     * Get repository by path
     */
    fun getRepository(path: String): RepositoryInfo? {
        if (repositoryCache.isEmpty()) {
            detectRepositories()
        }
        return repositoryCache.find { it.path == path }
    }

    /**
     * Get all cached repositories
     */
    fun getCachedRepositories(): List<RepositoryInfo> {
        return repositoryCache.toList()
    }

    /**
     * Clear the repository cache
     */
    fun clearCache() {
        repositoryCache.clear()
    }
}
