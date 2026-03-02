package io.github.pavelo8501.io

import io.github.pavelo8501.console.output
import io.github.pavelo8501.exceptions.text

import io.github.pavelo8501.named.Named
import io.github.pavelo8501.time.TimeHelper
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import javax.tools.FileObject
import kotlin.io.path.Path
import kotlin.io.path.createTempFile


data class FileMime(val mime: String, val extension: String)

enum class MimeType(val type: FileMime): Named {
    Json(FileMime("application/json", ".json")),
    Jpg(FileMime("image/jpeg", ".jpg"));
}



@JvmInline
value class AbsolutePath(val value: String):Named{
    object Path
    override val name: String get() =  "Path"
}


class FilePath(
    val path: AbsolutePath,
    val fileName: String,
    val mime: MimeType,
): FileObject{


    val file: File =createTempFile("${path.value}/$fileName", mime.type.extension).toFile()


    override fun toUri(): URI = file.toURI()
    override fun getName(): String = file.name
    override fun openInputStream(): InputStream = file.inputStream()
    override fun openOutputStream(): OutputStream = file.outputStream()

    override fun openReader(ignoreEncodingErrors: Boolean): Reader = file.reader()
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence= file.readText()
    override fun openWriter(): Writer = file.writer()
    override fun getLastModified(): Long = file.lastModified()
    override fun delete(): Boolean = file.delete()

}


class LocalFile(
    val bytes: ByteArray,
    val meta: FileMetaData,
) : FileMetaData by meta, TimeHelper {

    fun readText(charset: Charset = Charsets.UTF_8): String = file.readText(charset)

    override fun readBytes(): ByteArray {
        return bytes
    }

    fun rewrite(withBytes: ByteArray? = null): Boolean{
        val byteArray = withBytes?:bytes
        return  try {
            file.writeBytes(byteArray)
            true
        }catch (th: Throwable){
            th.text().output()
            false
        }
    }
}

class SourcedFile<T: Any>(
    val bytes: ByteArray,
    val meta: FileMetaData,
    val provider: (ByteArray)-> T
) : FileMetaData by meta, TimeHelper {

    val firstCreated: Instant = nowTime()

    constructor(localFile: LocalFile, provider: (ByteArray)-> T):this(localFile.bytes, localFile.meta, provider)

    private var sourceBacking:T? = null

    val source: T get() {
        return sourceBacking ?:run {
            val fromProvider = provider(bytes)
            sourceBacking = fromProvider
            fromProvider
        }
    }



    override fun readBytes(): ByteArray {
        return bytes
    }

    fun updateSource(source: T, bytes: ByteArray): Boolean{
        return try {
            file.writeBytes(bytes)
            sourceBacking = source
            true
        }catch (th: Throwable){
            th.text().output()
            false
        }
    }

}
