import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.gpomares"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")

        // Bundled plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("Git4Idea")  // Essential for Git integration

        // Testing
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.gpomares.git-commit-tracker"
        name = "Git Commit Tracker"
        version = project.version.toString()
        description = "Track commits across multiple Git repositories in your workspace"
        changeNotes = """
            Initial release:
            - Auto-detect Git repositories
            - Filter commits by author, date, and repository
            - Tool window integration
        """.trimIndent()

        ideaVersion {
            sinceBuild = "241"  // 2024.1
            untilBuild = provider { null }  // Compatible with future versions
        }
    }

    signing {
        // Configure for plugin marketplace if needed
    }

    publishing {
        // Configure for plugin marketplace if needed
    }
}

kotlin {
    jvmToolchain(21)  // Use Java 21
}

tasks {
    wrapper {
        gradleVersion = "8.13"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("")
    }

    // Disable buildSearchableOptions for WSL compatibility
    buildSearchableOptions {
        enabled = false
    }
}
