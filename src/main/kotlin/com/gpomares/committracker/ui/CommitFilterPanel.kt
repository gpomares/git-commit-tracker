package com.gpomares.committracker.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.gpomares.committracker.services.RepositoryDetectionService
import java.awt.FlowLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.swing.*

class CommitFilterPanel(private val project: Project) : JPanel() {

    private val listeners = mutableListOf<(FilterCriteria) -> Unit>()
    private val fetchListeners = mutableListOf<() -> Unit>()

    private val searchField = SearchTextField()
    private val dateFromField = JBTextField(10)
    private val dateToField = JBTextField(10)
    private val repoComboBox = JComboBox<String>()

    init {
        layout = FlowLayout(FlowLayout.LEFT, 10, 5)
        setupUI()
        loadRepositories()
    }

    private fun setupUI() {
        // Search field
        add(JBLabel("Search:"))
        searchField.textEditor.toolTipText = "Search in commit message, hash, or author"
        add(searchField)

        // Date range filters
        add(JBLabel("From:"))
        dateFromField.toolTipText = "Start date (yyyy-MM-dd)"
        add(dateFromField)

        add(JBLabel("To:"))
        dateToField.toolTipText = "End date (yyyy-MM-dd)"
        add(dateToField)

        // Repository filter
        add(JBLabel("Repository:"))
        repoComboBox.toolTipText = "Filter by repository"
        add(repoComboBox)

        // Apply button
        val applyButton = JButton("Apply Filters")
        applyButton.addActionListener {
            notifyFilterChange()
        }
        add(applyButton)

        // Clear button
        val clearButton = JButton("Clear")
        clearButton.addActionListener {
            clearFilters()
        }
        add(clearButton)

        // Refresh button
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener {
            loadRepositories()
            notifyFilterChange()
        }
        add(refreshButton)

        // Fetch button
        val fetchButton = JButton("Fetch All Repos")
        fetchButton.toolTipText = "Fetch latest changes from all remote repositories"
        fetchButton.addActionListener {
            notifyFetchRequested()
        }
        add(fetchButton)
    }

    private fun loadRepositories() {
        val repoService = project.service<RepositoryDetectionService>()
        val repos = repoService.detectRepositories()

        val currentSelection = repoComboBox.selectedItem as? String

        repoComboBox.removeAllItems()
        repoComboBox.addItem("All Repositories")

        repos.forEach { repo ->
            repoComboBox.addItem(repo.path)
        }

        // Restore previous selection if possible
        if (currentSelection != null) {
            repoComboBox.selectedItem = currentSelection
        }
    }

    private fun notifyFilterChange() {
        val criteria = FilterCriteria(
            searchText = searchField.text.takeIf { it.isNotBlank() },
            dateFrom = parseDateOrNull(dateFromField.text),
            dateTo = parseDateOrNull(dateToField.text),
            repository = (repoComboBox.selectedItem as? String)?.takeIf { it != "All Repositories" }
        )

        listeners.forEach { it(criteria) }
    }

    private fun clearFilters() {
        searchField.text = ""
        dateFromField.text = ""
        dateToField.text = ""
        repoComboBox.selectedIndex = 0
        notifyFilterChange()
    }

    private fun parseDateOrNull(dateString: String): LocalDate? {
        if (dateString.isBlank()) return null

        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    fun addFilterChangeListener(listener: (FilterCriteria) -> Unit) {
        listeners.add(listener)
    }

    fun addFetchListener(listener: () -> Unit) {
        fetchListeners.add(listener)
    }

    private fun notifyFetchRequested() {
        fetchListeners.forEach { it() }
    }

    data class FilterCriteria(
        val searchText: String?,
        val dateFrom: LocalDate?,
        val dateTo: LocalDate?,
        val repository: String?
    )
}
