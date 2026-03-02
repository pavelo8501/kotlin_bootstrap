package io.github.pavelo8501.io

import io.github.pavelo8501.console.output
import io.github.pavelo8501.exceptions.text
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Paths



class DeletionList(){
    internal val relPathsToDelete = mutableListOf<String>()
    fun addPath(relativePath: String){
        relPathsToDelete.add(relativePath)
    }
}

fun deleteFile(relativePath: String): Boolean{
    try {
        val path = Paths.get(System.getProperty("user.dir")).resolve(relativePath)
        Files.delete(path)
        return true
    }catch (th: Throwable){
        th.text().output()
        return false
    }
}


private fun executeDeleteAllOrNan(
    block:DeletionList.() -> Unit
): Boolean{
    val list = DeletionList()
    list.block()
    val files = mutableListOf<Pair<String, File>>()
    for(relPath in  list.relPathsToDelete){

        fileExists(relPath)?.let {meta->
            files.add(Pair(relPath,meta.file))
        }?:run {
            return false
        }
    }
    files.forEach {
        it.second.delete()
    }
    return true
}

fun deleteAllOrNan(block:DeletionList.() -> Unit): Boolean = executeDeleteAllOrNan(block)