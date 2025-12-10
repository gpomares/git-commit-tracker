package com.gpomares.committracker.models

import com.intellij.openapi.vfs.VirtualFile

data class RepositoryInfo(
    val name: String,
    val path: String,
    val currentBranch: String,
    val root: VirtualFile
)
