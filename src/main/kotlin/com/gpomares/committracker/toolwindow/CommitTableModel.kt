package com.gpomares.committracker.toolwindow

import com.gpomares.committracker.models.CommitInfo
import javax.swing.table.AbstractTableModel

class CommitTableModel : AbstractTableModel() {

    private var commits = listOf<CommitInfo>()

    private val columnNames = arrayOf(
        "Repository",
        "Hash",
        "Message",
        "Author",
        "Date"
    )

    override fun getRowCount(): Int = commits.size

    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val commit = commits[rowIndex]
        return when (columnIndex) {
            0 -> commit.repository
            1 -> commit.shortHash
            2 -> commit.firstLineMessage  // First line only
            3 -> commit.author
            4 -> commit.formattedDate
            else -> ""
        }
    }

    /**
     * Set the commits to display in the table
     */
    fun setCommits(newCommits: List<CommitInfo>) {
        commits = newCommits
        fireTableDataChanged()
    }

    /**
     * Add commits to the existing list (for lazy loading)
     */
    fun addCommits(newCommits: List<CommitInfo>) {
        val startIndex = commits.size
        commits = commits + newCommits
        fireTableRowsInserted(startIndex, commits.size - 1)
    }

    /**
     * Get commit at a specific row
     */
    fun getCommit(row: Int): CommitInfo? {
        return commits.getOrNull(row)
    }

    /**
     * Clear all commits
     */
    fun clear() {
        commits = emptyList()
        fireTableDataChanged()
    }

    /**
     * Get all commits
     */
    fun getAllCommits(): List<CommitInfo> {
        return commits.toList()
    }
}
