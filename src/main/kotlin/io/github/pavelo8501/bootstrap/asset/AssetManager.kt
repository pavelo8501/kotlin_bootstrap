package io.github.pavelo8501.bootstrap.asset

import io.github.pavelo8501.bootstrap.asset.registry.AssetRegistry
import io.github.pavelo8501.component.Component
import io.github.pavelo8501.io.deleteFile
import io.github.pavelo8501.io.toSafePathName
import io.github.pavelo8501.logger.Topic
import io.github.pavelo8501.logger.Verbosity
import io.github.pavelo8501.style.SpecialChars
import io.github.pavelo8501.style.TextStyler
import kotlinx.serialization.json.Json

import io.github.pavelo8501.bootstrap.asset.registry.RegistrySource
import kotlin.enums.enumEntries


/**
 * Defines builder utilities for creating and configuring [AssetRegistry] instances
 * within an [AssetManager] context.
 *
 * The `AssetBuilder` sealed interface provides convenient DSL-style methods
 * for constructing asset registries dynamically based on categories, enums,
 * or lists of identifiers. Each created registry is automatically associated
 * with the parent [AssetManager] and filtered according to its configuration.
 *
 * Implementations of this interface must also implement [ConfigHolder],
 * providing access to [AssetManager.ConfigData] via the [config] property.
 *
 * Example usage:
 * ```
 * class MyManager(...) : AssetManager(...), AssetBuilder {
 *     fun initRegistries() {
 *         buildRegistry("images") {
 *             addAsset("files/logo.png", "Logo")
 *             addAsset("files/photo.png", "Photo")
 *         }
 *
 *         buildFromEnum<MyCategories> {
 *             addAsset("assets/${it.name.lowercase()}.png", it.name)
 *         }
 *     }
 * }
 * ```
 */
sealed interface AssetBuilder: ConfigHolder {
    private val selfie: AssetManager get() = this as AssetManager

    /** Provides configuration data for this builder, usually derived from the [AssetManager]. */
    override val config: AssetManager.ConfigData

    /**
     * Builds a new [AssetRegistry] using a string category name.
     *
     * The provided [builder] lambda is executed in the context of the new [AssetRegistry],
     * allowing assets to be registered directly. Once built, the registry is automatically
     * linked to the [AssetManager] and filtered according to the current [config].
     *
     * @param category The name of the asset category (e.g., `"images"`, `"sounds"`).
     * @param builder The configuration block to populate the registry.
     */
    fun buildRegistry(category: String, builder: AssetRegistry.(String)-> Unit){
       val registry = AssetRegistry(selfie, category)
       registry.builder(category)
       selfie.addFilteringByConfig(config, registry)
    }

    /**
     * Builds a new [AssetRegistry] based on an enum category.
     *
     * The provided [builder] lambda receives the enum value corresponding to
     * the created registry, allowing per-enum configuration.
     *
     * The function returns the result of the builder block, allowing fluent use.
     *
     * @param category The enum value representing this registry’s category.
     * @param builder The configuration block executed for the registry.
     * @return The result returned by the [builder] block.
     */
    fun <R> buildRegistry(category: Enum<*>, builder: AssetRegistry.(Enum<*>)-> R):R{
        val registry = AssetRegistry(selfie, category.name)
        val lambdaResult = registry.builder(category)
        selfie.addFilteringByConfig(config, registry)
        return lambdaResult
    }

    /**
     * Builds multiple [AssetRegistry] instances from a list of category names.
     *
     * Each element in the list is treated as a category name and passed to the [builder] block.
     * All created registries are registered under the same [AssetManager] and filtered
     * according to the builder’s [config].
     *
     * @param builder The block executed for each created [AssetRegistry].
     */
    fun List<String>.buildFromList(builder: AssetRegistry.(String)-> Unit){
        forEach {
            val registry = AssetRegistry(selfie, it)
            registry.builder(it)
            selfie.addFilteringByConfig(config, registry)
        }
    }
}

/**
 * Builds one [AssetRegistry] for each entry of the given enum type [E].
 *
 * This helper is a generic extension for creating a series of registries based
 * on all enum constants of [E]. The [builder] lambda is executed for each enum
 * entry and receives both the current registry and the enum value itself.
 *
 * Each created registry inherits this builder’s [config] and is automatically
 * linked to the owning [AssetManager].
 *
 * @param builder The block to configure each created registry.
 * @return The list of all created [AssetRegistry] instances.
 */
inline fun <reified E: Enum<E>> AssetBuilder.buildFromEnum(
    noinline builder: AssetRegistry.(E)-> Unit
): List<AssetRegistry> {
    val selfie = this as AssetManager
    enumEntries<E>().forEach {
        val registry = AssetRegistry(selfie, it.name)
        registry.config = config
        registry.builder(it)
        addFilteringByConfig(registry.config, registry)
    }
    return selfie.registriesBacking
}


/**
 * @param basePath relative path to the root folder
 */
class AssetManager(
    basePath: String,
    val jsonEncoder: Json,
): Component, AssetBuilder, TextStyler {

    data class ConfigData(
        var basePath: String,
        var recreateKeysChanged: Boolean = true,
        var verbosity: Verbosity = Verbosity.Info,
        var includeEmpty: Boolean = false,
        var clearEmptyOnInit: Boolean = false,
    ){
        fun setBasePath(relativePath: String):ConfigData{
            basePath = relativePath
            return this
        }
    }

    override var config: ConfigData = ConfigData(basePath = basePath)

    @PublishedApi
    internal val registriesBacking: MutableList<AssetRegistry> = mutableListOf()

    constructor(
        basePath: String,
        json: Json,
        configBuilder: AssetBuilder.()-> Unit
    ):this(basePath, json){
        configBuilder()
    }

    constructor(
        assetsConfig: AssetsKeyConfig,
        json: Json,
        configBuilder: AssetBuilder.()-> Unit
    ):this(
        assetsConfig.assetsPath,
        json
    ){
        configBuilder()
    }

    override var verbosity: Verbosity
        set(value) {  config.verbosity = value }
        get() =  config.verbosity


    val registries : List<AssetRegistry> get() = registriesBacking

    private val registriesText: String get() = registries.joinToString(separator = SpecialChars.NEW_LINE) {
        it.toString()
    }

    private val notFoundMessage : String get() = "Not found"

    @PublishedApi
    internal fun addFilteringByConfig(config: ConfigData, registry: AssetRegistry){
        if(config.includeEmpty){
            registriesBacking.add(registry)
        }else{
            if(registry.assets.isNotEmpty()){
                registriesBacking.add(registry)
            }else{
                "Registry $registry filtered out by config includeEmpty: ${config.includeEmpty}"
            }
        }
        if(config.clearEmptyOnInit){
            if(registry.assets.isEmpty()) {
                registry.purge()

                deleteFile(registry.registryPath)
                notify("Registry $registry cleared out by config clearEmptyOnInit: ${config.clearEmptyOnInit}", Topic.Info)
            }
        }
    }

    fun addRegistry(assetRegistry: AssetRegistry):AssetRegistry{
        assetRegistry.assetManager = this
        return assetRegistry
    }

    fun addRegistry(assetRegistry: RegistrySource):AssetRegistry {
       val registry = AssetRegistry(assetRegistry.name, config.basePath, jsonEncoder)
       return addRegistry(registry)
    }

    fun commitChanges(): Int{
        var commits = 0
        registries.forEach {
            if(it.commitChanges()){
                commits ++
            }
        }
        if(commits > 0){
            notify( "Commited $commits registry changes", Topic.Info)
        }else{
            notify( "No changes in registries nothing to commit", Topic.Info)
        }
        return commits
    }

    fun getRegistry(category: String): AssetRegistry?{
          return registries.firstOrNull{ it.category == category }
    }
    fun getRegistry(category: Enum<*>): AssetRegistry? = getRegistry(category.name)

    fun withRegistry(category: Enum<*>, block: AssetRegistry.()-> Unit){

        val registry = getRegistry(category.name)
        if(registry == null){
            notify("Operations. Not found", Topic.Warning)
        }else{
            registry.block()
        }
    }

    fun getAsset(category: String, assetName: NamedAsset): Asset {
        TODO("getAsset")
        // getRegistry(category).assets(assetName)
    }
    fun getAsset(category: Enum<*>, assetName: NamedAsset): Asset? = getAsset(category.name, assetName)

    fun getAsset(category: String, assetName: String): Asset {
        TODO("getAsset")
    }
    fun purge(category: String) : Boolean{
       return registries.firstOrNull{ it.category ==  category}?.let {
            it.purge()
            true
        }?:false
    }
    fun purge(registry: AssetRegistry): Boolean = purge(registry.category)

    companion object{

        fun toAssetsPath(basePath: String, categoryName: String): String{
           return "$basePath/${categoryName.toSafePathName()}.json"
        }

        fun toAssetsPath(basePath: String, category: Enum<*>): String = toAssetsPath(basePath, category.name)
    }

}