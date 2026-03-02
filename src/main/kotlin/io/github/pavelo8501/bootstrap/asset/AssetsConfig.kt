package io.github.pavelo8501.bootstrap.asset

import io.github.pavelo8501.logger.Verbosity


interface AssetsKeyConfig {
    var assetsPath: String
}

interface ConfigHolder{
    val config : AssetManager.ConfigData

    var basePath : String
        set(value) { config.basePath = value }
        get() = config.basePath

    var recreateKeysChanged : Boolean
        set(value) { config.recreateKeysChanged = value }
        get() = config.recreateKeysChanged

    var verbosity : Verbosity
        set(value) { config.verbosity = value }
        get() = config.verbosity

    var includeEmpty : Boolean
        set(value) { config.includeEmpty = value }
        get() = config.includeEmpty

    var clearEmptyOnInit : Boolean
        set(value) { config.clearEmptyOnInit = value }
        get() = config.clearEmptyOnInit

}