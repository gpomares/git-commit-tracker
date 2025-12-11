package com.gpomares.committracker.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.gpomares.committracker.models.CommitInfo
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
import javax.swing.JButton
import java.awt.FlowLayout
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import javax.swing.JOptionPane
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CommitTrackerPanel(private val project: Project) : JPanel(), Disposable {

    private val log = logger<CommitTrackerPanel>()
    private val commitService = project.service<GitCommitService>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val commitTableModel = CommitTableModel()
    private val commitTable = JBTable(commitTableModel)
    private val filterPanel = CommitFilterPanel(project)
    private val statusLabel = JLabel("Loading commits...", SwingConstants.CENTER)
    private val extractButton = JButton("Extract")

    init {
        layout = BorderLayout()
        setupUI()
        setupListeners()
        loadCommitsAsync()
    }

    private fun setupUI() {
        // Create top panel with filter and extract button
        val topPanel = JPanel(BorderLayout())
        topPanel.add(filterPanel, BorderLayout.CENTER)

        // Wrap extract button in a panel to match height
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        buttonPanel.add(extractButton)
        topPanel.add(buttonPanel, BorderLayout.EAST)
        add(topPanel, BorderLayout.NORTH)

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

        extractButton.addActionListener {
            extractCommits()
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

    private fun extractCommits() {
        val commits = commitTableModel.getAllCommits()
        if (commits.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No commits to extract", "Extract", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        // Show dialog with options
        val options = arrayOf("Export to CSV", "Copy to Clipboard", "Cancel")
        val choice = JOptionPane.showOptionDialog(
            this,
            "Choose extraction method:",
            "Extract Commits",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )

        when (choice) {
            0 -> exportToCSV(commits)
            1 -> copyToClipboard(commits)
            2 -> return
        }
    }

    private fun exportToCSV(commits: List<CommitInfo>) {
        try {
            val writer = StringWriter()
            val csvWriter = java.io.PrintWriter(writer)

            // Write header
            csvWriter.println("Repository,Hash,Message,Author,Email,Date,Branch")

            // Write data
            commits.forEach { commit ->
                csvWriter.println(
                    "\"${commit.repository}\",\"${commit.hash}\",\"${commit.message.replace("\"", "\"\"")}\",\"${commit.author}\",\"${commit.authorEmail}\",\"${commit.formattedDate}\",\"${commit.branch}\""
                )
            }

            csvWriter.flush()
            csvWriter.close()

            // Save to file
            val content = writer.toString()
            val fileChooser = javax.swing.JFileChooser()
            fileChooser.dialogTitle = "Save Commits as CSV"
            fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv")
            
            if (fileChooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                java.nio.file.Files.write(
                    java.nio.file.Paths.get(file.absolutePath),
                    content.toByteArray(StandardCharsets.UTF_8)
                )
                JOptionPane.showMessageDialog(this, "Commits exported successfully", "Export Complete", JOptionPane.INFORMATION_MESSAGE)
            }
        } catch (e: Exception) {
            log.error("Error exporting to CSV", e)
            JOptionPane.showMessageDialog(this, "Error exporting to CSV: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun copyToClipboard(commits: List<CommitInfo>) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val stringSelection = StringSelection(commits.joinToString("\n") { commit ->
                "${commit.repository}\t${commit.shortHash}\t${commit.firstLineMessage}\t${commit.author}\t${commit.formattedDate}"
            })
            clipboard.setContents(stringSelection, null)
            JOptionPane.showMessageDialog(this, "Commits copied to clipboard", "Copy Complete", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
            log.error("Error copying to clipboard", e)
            JOptionPane.showMessageDialog(this, "Error copying to clipboard: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}
