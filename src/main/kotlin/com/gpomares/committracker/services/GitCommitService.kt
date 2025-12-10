package com.gpomares.committracker.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.gpomares.committracker.models.CommitInfo
import com.gpomares.committracker.models.RepositoryInfo
import com.gpomares.committracker.utils.GitConfigHelper
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class DateRange(
    val from: LocalDate?,
    val to: LocalDate?
) {
    operator fun contains(timestamp: Long): Boolean {
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )

        val afterFrom = from?.let { date.isAfter(it) || date.isEqual(it) } ?: true
        val beforeTo = to?.let { date.isBefore(it) || date.isEqual(it) } ?: true

        return afterFrom && beforeTo
    }
}

@Service(Service.Level.PROJECT)
class GitCommitService(private val project: Project) {

    private val log = logger<GitCommitService>()
    private val repoService = project.service<RepositoryDetectionService>()

    /**
     * Get all commits by the current user across all repositories
     */
    suspend fun getAllUserCommits(
        dateRange: DateRange? = null,
        repositories: List<String>? = null
    ): List<CommitInfo> = withContext(Dispatchers.Default) {

        val currentUser = GitConfigHelper.getCurrentUserName(project)
        val currentEmail = GitConfigHelper.getCurrentUserEmail(project)

        if (currentUser == null && currentEmail == null) {
            log.warn("Could not determine current user from Git config")
            return@withContext emptyList()
        }

        val repos = repoService.detectRepositories()

        val filteredRepos = if (repositories != null) {
            repos.filter { it.path in repositories }
        } else {
            repos
        }

        filteredRepos.flatMap { repoInfo ->
            getCommitsForRepository(repoInfo, currentUser, currentEmail, dateRange)
        }
        .distinctBy { it.hash }  // Remove duplicate commits (same commit on multiple branches)
        .sortedByDescending { it.timestamp }
    }

    /**
     * Get commits for a specific repository
     */
    private suspend fun getCommitsForRepository(
        repoInfo: RepositoryInfo,
        currentUser: String?,
        currentEmail: String?,
        dateRange: DateRange?
    ): List<CommitInfo> = withContext(Dispatchers.IO) {

        val commits = mutableListOf<CommitInfo>()

        try {
            val gitRepo = GitRepositoryManager.getInstance(project).repositories
                .find { it.root.path == repoInfo.path } ?: return@withContext emptyList()

            // Load commit history from all branches
            val history = GitHistoryUtils.history(project, gitRepo.root, "--all", "--no-merges")

            history.forEach { commit ->
                // Filter by author
                val authorMatches = when {
                    currentUser != null && commit.author.name.equals(currentUser, ignoreCase = true) -> true
                    currentEmail != null && commit.author.email.equals(currentEmail, ignoreCase = true) -> true
                    else -> false
                }

                if (authorMatches) {
                    // Filter by date range
                    if (dateRange == null || commit.commitTime in dateRange) {
                        val branch = getBranchForCommit(gitRepo, commit.id.asString())

                        commits.add(
                            CommitInfo(
                                hash = commit.id.asString(),
                                shortHash = commit.id.toShortString(),
                                message = commit.fullMessage,
                                author = commit.author.name,
                                authorEmail = commit.author.email,
                                timestamp = commit.commitTime,
                                repository = repoInfo.name,
                                repositoryPath = repoInfo.path,
                                branch = branch
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Error loading commits from repository: ${repoInfo.path}", e)
        }

        commits
    }

    /**
     * Get branch info for display
     * Since commits are deduplicated, this shows which branch it's currently on
     */
    private fun getBranchForCommit(gitRepo: GitRepository, commitHash: String): String {
        return try {
            // Return current branch or check local branches
            gitRepo.currentBranchName ?: gitRepo.branches.localBranches.firstOrNull()?.name ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
