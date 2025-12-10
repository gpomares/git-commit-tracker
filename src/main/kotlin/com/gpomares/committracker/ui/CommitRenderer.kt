package com.gpomares.committracker.ui

import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class CommitRenderer : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        // Set tooltip for long messages
        if (column == 3 && value != null) {  // Message column
            toolTipText = value.toString()
        }

        return component
    }
}
