

rootProject.name = "bootstrap"

pluginManagement {
    plugins {
        kotlin("plugin.serialization")  version("2.3.0") apply false
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }