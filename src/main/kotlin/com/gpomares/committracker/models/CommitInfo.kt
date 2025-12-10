package com.gpomares.committracker.models

import java.text.SimpleDateFormat
import java.util.Date

data class CommitInfo(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val authorEmail: String,
    val timestamp: Long,
    val repository: String,
    val repositoryPath: String,
    val branch: String
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(timestamp))

    val firstLineMessage: String
        get() = message.lines().firstOrNull() ?: ""
}
