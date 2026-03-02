package io.github.pavelo8501.io

fun String.toSafePathName(fallbackName: String? = null): String{
    val asString = toString()
    val converted = asString
        .lowercase()
        .replace("[^a-z0-9_-]".toRegex(), "_")
        .replace("_+".toRegex(), "_")
        .trim('_')

    return converted.ifBlank {
        fallbackName ?: run {
            val failMessage = "$asString can not be conferted to safe file name. And no fallback name provided"
            throw IllegalArgumentException(failMessage)
        }
    }
}

fun Enum<*>.toSafePathName(fallbackName: String? = null): String = name.toSafePathName()

fun buildRelativePath(vararg pathChunks: String): String{
    return pathChunks.joinToString(separator = "/") {
        it.toSafePathName()
    }
}

fun String.stripFileExtension(): String{
    return  trim().replaceAfter('.', "").toSafePathName()
}
