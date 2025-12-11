package com.gpomares.committracker.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.gpomares.committracker.services.GitCommitService
import com.gpomares.committracker.services.RepositoryDetectionService
import com.gpomares.committracker.ui.CommitFilterPanel
import com.gpomares.committracker.ui.CommitRenderer
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.SwingConstants

class CommitTrackerPanel(private val project: Project) : JPanel(), Disposable {

    private val log = logger<CommitTrackerPanel>()
    private val commitService = project.service<GitCommitService>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val commitTableModel = CommitTableModel()
    private val commitTable = JBTable(commitTableModel)
    private val filterPanel = CommitFilterPanel(project)
    private val statusLabel = JLabel("Loading commits...", SwingConstants.CENTER)

    init {
        layout = BorderLayout()
        setupUI()
        setupListeners()
        loadCommitsAsync()
    }

    private fun setupUI() {
        // Add filter panel at the top
        add(filterPanel, BorderLayout.NORTH)

        // Add table in the center
        val scrollPane = JBScrollPane(commitTable)
        add(scrollPane, BorderLayout.CENTER)

        // Add status label at the bottom
        add(statusLabel, BorderLayout.SOUTH)

        // Configure table
        commitTable.setShowGrid(false)
        commitTable.intercellSpacing = Dimension(0, 0)
        commitTable.autoCreateRowSorter = true  // Enable sorting
        commitTable.setDefaultRenderer(Any::class.java, CommitRenderer())

        // Set column widths
        commitTable.columnModel.getColumn(0).preferredWidth = 150  // Repository
        commitTable.columnModel.getColumn(1).preferredWidth = 80   // Hash
        commitTable.columnModel.getColumn(2).preferredWidth = 400  // Message
        commitTable.columnModel.getColumn(3).preferredWidth = 150  // Author
        commitTable.columnModel.getColumn(4).preferredWidth = 150  // Date
    }

    private fun setupListeners() {
        filterPanel.addFilterChangeListener { filters ->
            applyFilters(filters)
        }

        filterPanel.addFetchListener {
            fetchAllRepositories()
        }
    }

    private fun loadCommitsAsync() {
        updateStatus("Loading commits...")

        scope.launch {
            try {
                val commits = commitService.getAllUserCommits()

                ApplicationManager.getApplication().invokeLater {
                    commitTableModel.setCommits(commits)
                    updateStatus("Loaded ${commits.size} commits")
                }
            } catch (e: Exception) {
                log.error("Error loading commits", e)
                ApplicationManager.getApplication().invokeLater {
                    updateStatus("Error loading commits: ${e.message}")
                }
            }
        }
    }

    private fun applyFilters(filters: CommitFilterPanel.FilterCriteria) {
        updateStatus("Applying filters...")

        scope.launch {
            try {
                val commits = commitService.getAllUserCommits(
                    dateRange = if (filters.dateFrom != null || filters.dateTo != null) {
                        com.gpomares.committracker.services.DateRange(
                            filters.dateFrom,
                            filters.dateTo
                        )
                    } else null,
                    repositories = filters.repository?.let { listOf(it) }
                )

                // Apply text search filter if provided
                val filteredCommits = if (!filters.searchText.isNullOrBlank()) {
                    commits.filter { commit ->
                        commit.message.contains(filters.searchText, ignoreCase = true) ||
                        commit.hash.contains(filters.searchText, ignoreCase = true) ||
                        commit.author.contains(filters.searchText, ignoreCase = true)
                    }
                } else {
                    commits
                }

                ApplicationManager.getApplication().invokeLater {
                    commitTableModel.setCommits(filteredCommits)
                    updateStatus("Found ${filteredCommits.size} commits")
                }
            } catch (e: Exception) {
                log.error("Error applying filters", e)
                ApplicationManager.getApplication().invokeLater {
                    updateStatus("Error applying filters: ${e.message}")
                }
            }
        }
    }

    private fun updateStatus(message: String) {
        statusLabel.text = message
    }

    private fun fetchAllRepositories() {
        updateStatus("Fetching from all repositories...")

        scope.launch {
            try {
                val repoService = project.service<RepositoryDetectionService>()
                val repos = repoService.detectRepositories()

                var successCount = 0
                var failCount = 0

                repos.forEach { repoInfo ->
                    try {
                        withContext(Dispatchers.IO) {
                            // Get Git repository
                            val gitRepo = GitRepositoryManager.getInstance(project).repositories
                                .find { it.root.path == repoInfo.path }

                            if (gitRepo != null) {
                                // Create fetch command
                                val handler = GitLineHandler(project, gitRepo.root, GitCommand.FETCH)
                                handler.addParameters("--all")
                                handler.setSilent(false)

                                // Execute fetch
                                val result = Git.getInstance().runCommand(handler)

                                if (result.success()) {
                                    successCount++
                                    ApplicationManager.getApplication().invokeLater {
                                        updateStatus("Fetching... ($successCount/${repos.size} complete)")
                                    }
                                } else {
                                    failCount++
                                    log.warn("Fetch failed for ${repoInfo.name}: ${result.errorOutputAsJoinedString}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        failCount++
                        log.error("Error fetching repository: ${repoInfo.name}", e)
                    }
                }

                ApplicationManager.getApplication().invokeLater {
                    val message = if (failCount > 0) {
                        "Fetch complete: $successCount succeeded, $failCount failed"
                    } else {
                        "Fetch complete: All $successCount repositories updated successfully"
                    }
                    updateStatus(message)

                    // Refresh commits after fetch
                    loadCommitsAsync()
                }
            } catch (e: Exception) {
                log.error("Error during fetch operation", e)
                ApplicationManager.getApplication().invokeLater {
                    updateStatus("Fetch failed: ${e.message}")
                }
            }
        }
    }

    fun refresh() {
        loadCommitsAsync()
    }

    override fun dispose() {
        scope.cancel()
    }
}
