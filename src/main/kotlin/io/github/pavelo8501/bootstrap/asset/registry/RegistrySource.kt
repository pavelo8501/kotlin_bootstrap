package io.github.pavelo8501.bootstrap.asset.registry

import io.github.pavelo8501.bootstrap.asset.AssetPayload

interface RegistrySource {
    val name: String
    val assets: Array<out AssetPayload>
}