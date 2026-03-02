
val kotlinVersion: String by project
val junitVersion: String by project
val kotlinReflectVersion: String by project
val serializationVersion:String by project
val typesafeVersion: String by project
val kotlinSerializationVersion: String by project

val lognotiyVersion: String by project
val exceptionsVersion: String by project
val coreVersion: String by project
val libVersion :String by project

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
}

group = "io.github.pavelo8501"
version = libVersion


repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    api("io.github.pavelo8501:core:${coreVersion}")
    api("io.github.pavelo8501:exceptions:${exceptionsVersion}")
    api("io.github.pavelo8501:lognotify:${lognotiyVersion}")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinSerializationVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinReflectVersion}")
    api("com.typesafe:config:${typesafeVersion}")


    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "bootstrap"
            version = libVersion
        }
    }
}

tasks.withType<Javadoc> {
    isFailOnError = false
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}