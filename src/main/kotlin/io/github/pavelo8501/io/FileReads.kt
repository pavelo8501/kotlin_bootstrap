package io.github.pavelo8501.io

import io.github.pavelo8501.common.getOrThrow
import io.github.pavelo8501.console.output
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import javax.tools.FileObject


class ReadFileHooks<R: Any>{

    internal var onErrorCallback: ((Throwable) -> Unit)? = null
    fun onError(callback: (Throwable) -> Unit){
        onErrorCallback = callback
    }

    @PublishedApi
    internal fun triggerError(throwable: Throwable){
        onErrorCallback?.invoke(throwable)
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


fun fileExists(relativePath: String): FileMeta?{
    val file = File(relativePath)
    return try {
        if (file.exists() && file.isFile) {
            FileMeta(
                relativePath,
                file
            )
        } else null
    } catch (e: SecurityException) {
        e.output()
        null
    }
}



fun FilePath.readByteArray():ByteArray = openInputStream().readAllBytes()




fun readFileContent(
    relativePath: String,
):ByteArray{
    val path = Paths.get(System.getProperty("user.dir")).resolve(relativePath)
    return Files.readAllBytes(path)
}


/**
 * Reads a file located at [relativePath] (relative to the current working directory)
 * and returns it as a [LocalFile], containing raw bytes and metadata.
 *
 * This method does **not handle exceptions**. If the file does not exist or cannot be read,
 * the exception is propagated to the caller.
 *
 * @param relativePath Path to the file relative to `user.dir`.
 * @return A [LocalFile] wrapper containing the file bytes and metadata such as name and path.
 *
 * @throws java.io.IOException If the file cannot be found or accessed.
 * @throws SecurityException If the JVM security manager denies read access.
 *
 * @see readFile for a safe, callback-based variant
 */
fun readFile(relativePath: String): LocalFile{
    val file = File(System.getProperty("user.dir"), relativePath)
    return  LocalFile(file.readBytes(), FileMeta(relativePath, file))
}


/**
 * Safely reads a file from [relativePath] and exposes result handling through [ReadFileHooks].
 *
 * Unlike the raw `readFile(relativePath)` overload, this variant **never throws exceptions**.
 * Instead, it forwards successful reads to `hooks.onSuccess(LocalFile)` and errors to
 * `hooks.onError(Throwable)`. The final return value is the one produced inside `onSuccess`,
 * or `null` if an error occurs.
 *
 * Typical usage:
 * ```
 * readFile("config.json") {
 *     onSuccess { file -> parseConfig(file.bytes) }
 *     onError { println("Failed to read file: $it") }
 * }
 * ```
 *
 * @param R The return type expected from the success hook.
 * @param relativePath Path to the file relative to `user.dir`.
 * @param withHooks Lambda configuring callbacks in [ReadFileHooks].
 * @return The result of `onSuccess`, or `null` if `onError` was triggered.
 *
 * @see ReadFileHooks
 * @see readFile for a throwing variant
 */

inline fun <reified R: Any> readFile(
    relativePath: String,
    withHooks:  ReadFileHooks<R>.()-> Unit
):R{
    val localFile =  readFile(relativePath)
    val hooks =  ReadFileHooks<R>()
    try {
        withHooks.invoke(hooks)
        val result = hooks.triggerSuccess(localFile)
        return result.getOrThrow<R>()
    } catch (th: Throwable) {
        hooks.triggerError(th)
        throw th
    }
}


/**
 * Reads a file and wraps it into a [SourcedFile], allowing the raw bytes to be lazily
 * transformed into a strongly-typed object [T] using the provided [provider].
 *
 * This overload is ideal when dealing with binary or custom-encoded files, where the
 * interpretation of bytes does not involve character decoding.
 *
 * @param T The final mapped type returned via [SourcedFile.source].
 * @param relativePath Path to the file relative to `user.dir`.
 * @param provider Function converting raw file bytes into type [T].
 * @return A [SourcedFile] that holds both the raw data and a typed representation.
 *
 * @see readSourced for a string-based provider
 */
fun <T: Any> readSourced(
    relativePath: String,
    provider: (ByteArray)-> T
):SourcedFile<T>{
   val file = readFile(relativePath)
   return SourcedFile<T>(file, provider)
}


/**
 * Reads a text-based file and wraps it into a [SourcedFile], using the specified [charset]
 * and a string-mapping [provider] to produce a typed object [T].
 *
 * The file is first decoded into a `String`, then passed to [provider] for transformation
 * into the target type.
 *
 * Example:
 * ```
 * val config = readSourced("config.json", Charsets.UTF_8) { json.decodeFromString<Config>(it) }
 * println(config.source)
 * ```
 *
 * @param T The final mapped type returned via [SourcedFile.source].
 * @param relativePath Path to the file relative to `user.dir`.
 * @param charset Charset used to decode file content into a string.
 * @param provider Function mapping decoded text into type [T].
 * @return A [SourcedFile] that holds both the raw data and a lazily decoded [T].
 *
 * @see SourcedFile
 */
inline fun <reified T: Any> readSourced(
    relativePath: String,
    charset: Charset,
    noinline provider: (String)-> T
):SourcedFile<T>{
    val file = readFile(relativePath)
    return SourcedFile<T>(file, { bytes-> provider.invoke(bytes.readToString(charset))  } )
}


fun readResourceContent(path: String): String {
    return object {}.javaClass.classLoader.getResource(path)
        ?.readText()
        ?: error("Resource not found: $path")
}

fun ByteArray.readToString(charset: Charset = Charsets.UTF_8): String = toString(charset)

