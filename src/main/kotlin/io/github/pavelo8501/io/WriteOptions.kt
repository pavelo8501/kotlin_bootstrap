package io.github.pavelo8501.io


data class WriteOptions(
    val overwriteExistent: Boolean = false,
    val createSubfolders: Boolean = true,
    val throwIfFileExists: Boolean = false
)