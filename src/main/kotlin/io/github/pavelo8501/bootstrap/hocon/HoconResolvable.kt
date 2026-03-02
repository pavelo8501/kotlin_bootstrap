package io.github.pavelo8501.bootstrap.hocon



/**
 * Public contract for types that can be populated from a HOCON configuration using a [HoconResolver].
 *
 * Implementations typically:
 * - Create a resolver bound to `this` instance (e.g. `HoconResolver(this)` or `resolver()` helper).
 * - Declare resolvable members using delegates such as [HoconProperty], [HoconObject], and [HoconList].
 * - Invoke `resolver.readConfig(config)` to parse and apply configuration into the instance.
 *
 * The resolver acts as the central registry and execution engine for all declared entries.
 *
 * ### Typical usage
 * ```
 * class AppConfig : HoconResolvable<AppConfig> {
 *     override val resolver = HoconResolver(this)
 *
 *     val port by HoconProperty(HoconInt)
 *     val nested by HoconObject(NestedConfig())
 *     val records: List<String> by HoconList()
 * }
 *
 * val cfg = ConfigFactory.load()
 * val config = AppConfig()
 * config.resolver.readConfig(cfg)
 * config.output()
 * ```
 *
 * Implementations should ensure the [resolver] instance is stable and bound to the same receiver
 * for the lifetime of the object.
 */
interface HoconResolvable<T>{

    /**
     * Resolver bound to this instance that holds and executes all registered HOCON entries.
     */
    val resolver: HoconResolver<T>

    /**
     * Outputs resolver diagnostics (receiver identity and registered entry statuses).
     * Intended for debugging and test output; the formatting is provided by the resolver.
     */
    fun status(){
        print(resolver.receiverStatus().styled)
    }
}

