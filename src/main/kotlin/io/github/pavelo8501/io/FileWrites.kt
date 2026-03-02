package io.github.pavelo8501.io


import io.github.pavelo8501.exceptions.text
import io.github.pavelo8501.style.Colour
import io.github.pavelo8501.style.SpecialChars
import io.github.pavelo8501.style.colorize
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists


data class FileIOError(val throwable:Throwable, val path: String){
    val formattedString: String get() = this.toString().colorize(Colour.Red)
    override fun toString(): String {
        return "FileWriteError"+ SpecialChars.NEW_LINE + "${throwable.text()} for Path: $path"
    }
}

class FileIOHooks{

    internal var onErrorCallback: ((FileIOError) -> Unit)? = null
    fun onError(callback: (FileIOError) -> Unit){
        onErrorCallback = callback
    }
    fun triggerError(error:FileIOError): Boolean{
       return onErrorCallback?.let {
            it.invoke(error)
            true
        }?:false
    }
    internal var onSuccessCallback: ((ByteArray) -> ByteArray)? = null

    fun onSuccess(callback: (ByteArray) -> ByteArray): Unit{
        onSuccessCallback = callback
    }

    internal var onResultCallback: ((ByteArray) -> Any)? = null

    fun <R> onResult(callback: (ByteArray) -> ByteArray): Unit{
        onResultCallback = callback
    }

    fun triggerSuccess(array: ByteArray): Boolean{
        return onSuccessCallback?.let {
            it.invoke(array)
            true
        }?:false
    }
}

class WriteFileHooks<R: Any>{
    internal var onErrorCallback: ((Throwable) -> R?)? = null
    fun onError(callback: (Throwable) -> R?){
        onErrorCallback = callback
    }

    @PublishedApi
    internal fun triggerError(throwable: Throwable):R?{
        return  onErrorCallback?.invoke(throwable)
    }

    internal var success: ((LocalFile) -> R)? = null
    fun onSuccess(callback: (LocalFile) -> R){
        success = callback
    }
    @PublishedApi
    internal fun triggerSuccess(localFile : LocalFile):R?{
        return success?.invoke(localFile)
    }
}


private fun writeFileContents(
    relativePath: String,
    content: String,
    options: WriteOptions
):FileIOError?{

    return try {
        val pathToFile = Path(System.getProperty("user.dir"), relativePath)
        val directory = pathToFile.parent

        if (!directory.exists() && options.createSubfolders) {
            directory.createDirectories()
        }

        if (pathToFile.exists() && !options.overwriteExistent) {
            if(options.throwIfFileExists){
                throw IOException("File already exists: $relativePath")
            }else{
                return null
            }
        }

        Files.writeString(pathToFile, content)
        null
    } catch (th: Throwable) {
        FileIOError(th, relativePath)
    }
}

private fun writeFileContent(
    relativePath: String,
    byteArray: ByteArray,
    options: WriteOptions
):FileIOError?{
    return try {
        val pathToFile = Path(System.getProperty("user.dir"), relativePath)
        val directory = pathToFile.parent

        if (!directory.exists() && options.createSubfolders) {
            directory.createDirectories()
        }
        if (pathToFile.exists() && !options.throwIfFileExists) {
            if(options.throwIfFileExists) {
                throw IOException("File already exists: $relativePath")
            }else{
                return null
            }
        }
        Files.write(pathToFile, byteArray)
        null
    } catch (th: Throwable) {
        FileIOError(th, relativePath)
    }
}


/**
 * Writes raw [bytes] to a file located at [relativePath], rooted at the JVM working directory (`user.dir`).
 *
 * Behavior is controlled via [options], supporting:
 * - Automatic directory creation (`createSubfolders`)
 * - Overwrite protection (`overwriteExistent`)
 *
 * This variant throws on write failure, offering strict control typical for lower-level I/O operations.
 *
 * @param relativePath Path relative to the current working directory.
 * @param bytes The binary content to write.
 * @param options Defines write-safety behavior (overwrite, directory creation).
 *
 * @return [LocalFile] containing written bytes and file metadata.
 *
 * @throws IOException If the file already exists and overwriting is disabled.
 * @throws SecurityException If writing is denied by JVM security manager.
 *
 * @see writeFile for a safe, callback-based variant
 * @see ByteArray.writeToFile
 * @see String.writeToFile
 */
fun writeFile(
    relativePath: String,
    bytes: ByteArray,
    options: WriteOptions = WriteOptions()
): LocalFile{
        val file = File(System.getProperty("user.dir"), relativePath)
        val pathToFile = file.toPath()
        val directory = pathToFile.parent

        if (!directory.exists() && options.createSubfolders) {
            directory.createDirectories()
        }
        if (pathToFile.exists() && !options.overwriteExistent) {
            return  LocalFile(bytes, FileMeta(relativePath, file))
        }

        if (pathToFile.exists() && options.throwIfFileExists) {
            throw IOException("File already exists: $relativePath")
        }
        file.writeBytes(bytes)
        return  LocalFile(bytes, FileMeta(relativePath, file))
}

/**
 * Safe, hook-based variant of [writeFile], providing success and failure callbacks without throwing.
 *
 * The write result is forwarded to `onSuccess(LocalFile)`, while any exception triggers `onError(Throwable)`.
 * The return value is defined by the success callback or `null` if an error occurs.
 *
 * Usage example:
 * ```
 * writeFile("output/data.txt", dataBytes) {
 *     onSuccess { file -> println("Saved to: ${file.path}") }
 *     onError { err -> logger.error("Write failed", err) }
 * }
 * ```
 *
 * @param R Type returned by the success handler.
 * @param relativePath Path to the file relative to `user.dir`.
 * @param bytes Content to be written.
 * @param options Write behavior configuration (overwrite, folders).
 * @param writeFileHooks Lambda configuring hooks via [WriteFileHooks].
 *
 * @return Value returned by `onSuccess`, or `null` if an error occurs.
 *
 * @see WriteFileHooks
 * @see writeFile for throwing variant
 */
inline fun <R: Any> writeFile(
    relativePath: String,
    bytes: ByteArray,
    options: WriteOptions = WriteOptions(overwriteExistent = false, createSubfolders = true),
    writeFileHooks: WriteFileHooks<R>.() ->  Unit
): R? {
    val hooks = WriteFileHooks<R>().apply(writeFileHooks)
    try {
        val localFile = writeFile(relativePath, bytes, options)
        return  hooks.triggerSuccess(localFile)
    }catch (th: Throwable){
        return hooks.triggerError(th)
    }
}


/**
 * Writes this [ByteArray] to a file via [writeFile], returning a [LocalFile].
 *
 * This is the idiomatic extension when raw data is already prepared.
 *
 * @param relativePath Path to write to.
 * @param options Write behavior configuration.
 *
 * @return [LocalFile] of the written file.
 */
fun ByteArray.writeToFile(
    relativePath: String,
    options: WriteOptions = WriteOptions()
):LocalFile = writeFile(relativePath, this,  options)

/**
 * Hook-enabled variant of [ByteArray.writeToFile], returning a transformed result on success.
 *
 * @param R Return type provided by success callback.
 * @param relativePath Path to write to.
 * @param options Write behavior configuration.
 * @param writeFileHooks Hook configuration: `onSuccess(LocalFile)` / `onError(Throwable)`
 *
 * @return Success callback result, or `null` on failure.
 */
inline fun <R: Any> ByteArray.writeToFile(
    relativePath: String,
    options: WriteOptions = WriteOptions(),
    writeFileHooks: WriteFileHooks<R>.() ->  Unit
):R? = writeFile<R>(relativePath, this,  options, writeFileHooks)


/**
 * Converts this [String] into a byte stream using [charset] and writes it to [relativePath].
 *
 * @param relativePath File path relative to root.
 * @param charset Charset for encoding (UTF-8 by default).
 * @param options Write behavior configuration.
 *
 * @return [LocalFile] representation of the result.
 */
fun String.writeToFile(
    relativePath: String,
    options: WriteOptions = WriteOptions(),
    charset: Charset = Charsets.UTF_8,
):LocalFile = writeFile(relativePath, toByteArray(charset),  options)

/**
 * Hook-enabled variant of [String.writeToFile], allowing typed post-write results.
 *
 * @param R Return type defined via success hook.
 * @param relativePath Path to write to.
 * @param charset Charset for text encoding.
 * @param options Write behavior configuration.
 * @param writeFileHooks Hook setup via [WriteFileHooks].
 *
 * @return Transformed result from success hook, or `null` if write fails.
 */
inline fun <R: Any> String.writeToFile(
    relativePath: String,
    options: WriteOptions = WriteOptions(),
    charset: Charset = Charsets.UTF_8,
    writeFileHooks: WriteFileHooks<R>.() ->  Unit
):R? = writeFile<R>(relativePath, toByteArray(charset),  options, writeFileHooks)


/**
 * Writes this object [T] to a file by transforming it into raw [ByteArray] using [provider],
 * and returns a [SourcedFile] which provides both raw data and the originating source object.
 *
 * Use this when implementing custom binary serializers or exporting model snapshots.
 *
 * @param T Source object type.
 * @param relativePath Path to write to.
 * @param options Write behavior configuration.
 * @param provider Transformation function producing serialized bytes.
 *
 * @return A [SourcedFile] wrapping the written content and source reference.
 *
 * @see readSourced for reading counterparts
 */
inline fun <reified T: Any> T.writeSourced(
    relativePath: String,
    options: WriteOptions = WriteOptions(),
    provider: (T)-> ByteArray
):SourcedFile<T>{
    val file = writeFile(relativePath,  provider(this), options)
    return SourcedFile<T>(file, { this } )
}


/**
 * Variant of [writeSourced] for text-producing serializers.
 *
 * The [provider] returns a [String] which is encoded using [charset] before writing.
 *
 * @param charset Charset for encoding text.
 *
 * @see writeSourced for raw binary version
 */
inline fun <reified T: Any> T.writeSourced(
    relativePath: String,
    charset: Charset,
    options: WriteOptions = WriteOptions(),
    provider: (T)-> String
):SourcedFile<T>{
    val file = writeFile(relativePath, provider(this).toByteArray(charset), options)
    return SourcedFile<T>(file, { this } )
}




fun ByteArray.writeFileContent(
    relativePath: String,
    options: WriteOptions = WriteOptions(),
    withHooks: FileIOHooks.()-> Unit
): Boolean {
    val hooks = FileIOHooks()
    hooks.withHooks()
    return writeFileContent(relativePath, this, options)?.let { result ->
        hooks.onErrorCallback?.invoke(result)
        false
    } ?: true
}

fun ByteArray.writeFileContent(
    relativePath: String,
    options: WriteOptions = WriteOptions()
): Boolean {
    return writeFileContent(relativePath, this, options) == null
}







