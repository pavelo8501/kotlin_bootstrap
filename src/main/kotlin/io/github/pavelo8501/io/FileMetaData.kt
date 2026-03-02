package io.github.pavelo8501.io

import java.io.File
import java.time.Instant


interface FileMetaData{
    val fileName: String
    val relativePath: String
    val size: Long
    val lastModified: Instant
    val file: File
    fun readBytes(): ByteArray
}

data class FileMeta(
    override val relativePath: String,
    override val file: File
): FileMetaData{


    override val size: Long = file.length()
    override val lastModified: Instant =  Instant.ofEpochMilli(file.lastModified())

    override fun readBytes(): ByteArray {
        return file.readBytes()
    }

    override val fileName: String get() = file.name

    fun readFile():ByteArray{
        return file.readBytes()
    }
}