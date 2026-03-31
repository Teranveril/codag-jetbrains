import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

val pluginGroup: String by project
val pluginVersion: String by project
val platformType: String by project
val platformVersion: String by project
val platformSinceBuild: String by project
val platformUntilBuild: String by project
val pluginName: String by project
val vendorName: String by project
val vendorUrl: String by project
val vendorEmail: String by project
val javaVersion: String by project

group = pluginGroup
version = pluginVersion

kotlin {
    jvmToolchain(javaVersion.toInt())
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(platformType, platformVersion)
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        name = pluginName
        version = pluginVersion
        ideaVersion {
            sinceBuild = platformSinceBuild
            untilBuild = platformUntilBuild
        }
        vendor {
            name = vendorName
            url = vendorUrl
            email = vendorEmail
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "8.12"
    }

    test {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }

    buildSearchableOptions {
        enabled = false
    }
}
