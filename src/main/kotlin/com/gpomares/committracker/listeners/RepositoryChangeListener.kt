package com.gpomares.committracker.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.gpomares.committracker.services.CommitCacheService
import com.gpomares.committracker.services.RepositoryDetectionService
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

class RepositoryChangeListener : GitRepositoryChangeListener {

    private val log = logger<RepositoryChangeListener>()

    override fun repositoryChanged(repository: GitRepository) {
        log.info("Repository changed: ${repository.root.path}")

        val project = repository.project

        // Invalidate cache for this repository
        val cacheService = project.service<CommitCacheService>()
        cacheService.invalidateRepository(repository.root.path)

        // Refresh repository detection cache
        val repoService = project.service<RepositoryDetectionService>()
        repoService.clearCache()
        repoService.detectRepositories()
    }
}
