package io.github.pavelo8501.bootstrap.asset.extensions

import kotlinx.serialization.json.Json
import io.github.pavelo8501.bootstrap.asset.Asset
import io.github.pavelo8501.bootstrap.asset.AssetManager
import io.github.pavelo8501.bootstrap.asset.NamedAsset
import io.github.pavelo8501.bootstrap.asset.registry.AssetRegistry
import io.github.pavelo8501.io.readFile
import io.github.pavelo8501.bootstrap.asset.registry.RegistrySource


/**
 * Defines common helper functions for creating and managing [AssetRegistry] instances
 * and their contained [Asset]s.
 *
 * Implementations of this interface provide high-level APIs for registry creation,
 * asset registration, and simplified interaction with the [AssetManager] system.
 *
 * This interface is typically mixed into contexts that already implement [TraceableContext],
 * enabling unified logging or tracing during asset operations.
 */
interface AssetHandler {

    /**
     * Creates and registers a new [Asset] within the current [AssetRegistry].
     *
     * Reads the file located at [relativePath], wraps it in an [Asset] instance with
     * the given [name], and adds it to the registry. Once added, all changes are
     * committed to persistent storage.
     *
     * @param relativePath The relative file path of the asset to load.
     * @param name The logical name of the asset (used as its registry key).
     * @param onChange Optional callback triggered whenever this asset is modified.
     *
     * @return The created [Asset] instance that has been added to the registry.
     */
    fun AssetRegistry.createAsset(
        relativePath: String,
        name: String,
        onChange: (AssetRegistry.(Asset)-> Unit)? = null
    ): Asset {
       val registry = this@createAsset
       val asset = registry.addAsset(readFile(relativePath), name)
       registry.commitChanges()
       return asset
    }

    /**
     * Creates and registers a new [Asset] using a [NamedAsset] reference.
     *
     * This is a convenience overload of [createAsset] that uses the
     * [NamedAsset.name] property as the asset name.
     *
     * @param relativePath The relative path of the asset file to register.
     * @param assetName A [NamedAsset] reference whose [NamedAsset.name] will be used.
     * @param onChange Optional callback that can be invoked when the asset changes.
     *
     * @return The newly created [Asset] instance.
     */
    fun AssetRegistry.createAsset(
        relativePath: String,
        assetName: NamedAsset,
        onChange: (AssetRegistry.(Asset)-> Unit)? = null
    ): Asset = this@createAsset.createAsset(relativePath, assetName.name, onChange)


    /**
     * Creates a new [AssetRegistry] instance from a [RegistrySource].
     *
     * This function reads all assets defined in the given [RegistrySource]
     * and registers them under a new [AssetRegistry] at the specified base path.
     *
     * @param registrySource The [RegistrySource] that defines the registry name and assets.
     * @param basePath The base directory where registry data should be stored.
     * @param json The JSON serializer used for reading/writing registry metadata.
     *
     * @return A fully constructed and populated [AssetRegistry].
     */
    fun createRegistry(registrySource: RegistrySource, basePath:String,  json: Json): AssetRegistry{
        val name = registrySource.name
        val registry = AssetRegistry(name, AssetManager.toAssetsPath(basePath, name), json)
        registrySource.assets.forEach {
            registry.addAsset(it)
        }
        return registry
    }

    /**
     * Creates and attaches a new [AssetRegistry] to this [AssetManager],
     * using the provided [RegistrySource] definition.
     *
     * Similar to [createRegistry], but automatically registers the new
     * [AssetRegistry] within the current [AssetManager] instance.
     *
     * @param registrySource The [RegistrySource] defining the registry and its assets.
     *
     * @return The newly added and populated [AssetRegistry].
     */
    fun AssetManager.createRegistry(registrySource: RegistrySource):AssetRegistry{
       val registry = addRegistry(registrySource)
        registrySource.assets.forEach {
            registry.addAsset(it)
        }
        return registry
    }

}