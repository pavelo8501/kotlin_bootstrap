package io.github.pavelo8501.bootstrap.asset

import io.github.pavelo8501.bootstrap.asset.registry.AssetRegistry
import io.github.pavelo8501.io.FileMetaData
import io.github.pavelo8501.io.LocalFile
import io.github.pavelo8501.io.readFile
import io.github.pavelo8501.io.stripFileExtension
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



interface RegistryAsset{
    val name: String
    val filePath: String
    val fileId: String?
    val description: String?
}

interface AssetPayload : NamedAsset {
    override val name: String
    val filePath: String
}

@Serializable
data class AssetSource(
    override val name: String,
    @SerialName("file_path")
    override val filePath: String,
    @SerialName("file_id")
    override var fileId: String? = null,
    override var description: String? = null
): AssetPayload, RegistryAsset

class Asset(
    val registry: AssetRegistry,
    val source : AssetSource,
):RegistryAsset{

    enum class State{
        InSync,
        Updated,
        MarkedDelete,
        MarkedDeleteWithFile
    }
    private var fileBacking: LocalFile? = null

    constructor(registry: AssetRegistry, payload: AssetPayload):this(registry, AssetSource(payload.name, payload.filePath))
    constructor(registry: AssetRegistry, localFile: LocalFile, name: String):this(registry, AssetSource(name, localFile.relativePath)){
        fileBacking = localFile
    }

    val assetContent: String = encodeSource(source)

    override val name: String get() = source.name
    override val filePath: String get() = source.filePath

    var errorHandler: (Asset.(Throwable)-> Unit)? = null

    val file: LocalFile get() = fileBacking?:run {
            val file = readFile(filePath)
            fileBacking = file
            file
        }

    internal var updated: ((Asset)-> Unit)? = null

    val updatePending: Boolean get() =  state != State.InSync

    var state: State = State.InSync
        internal  set(value){
            if(value != field){
                field = value
                updated?.invoke(this)
            }
        }

    override var fileId: String? = source.fileId
        set(value) {
            if(value  != source.fileId){
                source.fileId = value
                state = State.Updated
            }
        }

    override var description: String? = source.description
        set(value) {
            if(value  != source.description){
                source.description = value
                state = State.Updated
            }
        }

    private fun encodeSource(assetSource: AssetSource): String{
        return registry.json.encodeToString(assetSource)
    }

    fun onUpdated(callback: (Asset)-> Unit){
        updated = callback
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Asset -> {
                name == other.name && filePath == other.filePath && fileId == other.fileId
            }
            is LocalFile -> {
                val bytesEqual = file.bytes.contentEquals(other.bytes)
                bytesEqual && name == other.fileName.stripFileExtension() && filePath == other.relativePath
            }
            is FileMetaData -> {
                name == other.fileName.stripFileExtension() && filePath == other.relativePath
            }
            else -> false
        }
    }
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + (fileId?.hashCode() ?: 0)
        result = 31 * result + file.bytes.hashCode()
        return result
    }
}