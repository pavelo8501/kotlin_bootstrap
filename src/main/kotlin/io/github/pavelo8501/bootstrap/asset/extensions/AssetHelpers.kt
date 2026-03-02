package io.github.pavelo8501.bootstrap.asset.extensions

import io.github.pavelo8501.bootstrap.asset.registry.AssetRegistry


fun List<AssetRegistry>.first(
    category: String,
): AssetRegistry = first { it.equals(category)}

fun List<AssetRegistry>.first(
    category: Enum<*>,
): AssetRegistry = first { it.equals(category)}


fun List<AssetRegistry>.firstOrNull(
    category: String,
): AssetRegistry? = firstOrNull{ it.equals(category) }

fun List<AssetRegistry>.firstOrNull(
    category: Enum<*>,
): AssetRegistry? = firstOrNull{ it.equals(category) }

