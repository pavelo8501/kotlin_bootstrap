package io.github.pavelo8501.bootstrap.hocon.extensions

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.bootstrap.hocon.models.ParserResult

/**
 * Loads the application's default HOCON configuration and returns the sub-config located at [path].
 *
 * This is a convenience wrapper around [ConfigFactory.load] + [Config.getConfig].
 *
 * @param path HOCON path to a config object (e.g. `"my.service"`).
 * @return the config object stored under the provided [path].
 * @throws com.typesafe.config.ConfigException.Missing if the path does not exist.
 * @throws com.typesafe.config.ConfigException.WrongType if the value at path is not an object.
 */
fun getTypeSafeConfig(path:String): Config = ConfigFactory.load().getConfig(path)

/**
 * Applies the provided HOCON [factory] to this resolvable instance by delegating to its [io.github.pavelo8501.bootstrap.hocon.HoconResolver].
 *
 * The resolver parses all registered entries (properties/objects/lists) and applies the results
 * to the current receiver instance.
 *
 * @param factory config source to read values from.
 * @return a [ParserResult] containing the receiver instance and parsing metrics.
 * @throws Throwable rethrows any parsing/configuration error produced by the resolver.
 */
fun <T: HoconResolvable<T>> T.applyConfig(factory:  Config): ParserResult<T> {
    return  resolver.readConfig(factory)
}