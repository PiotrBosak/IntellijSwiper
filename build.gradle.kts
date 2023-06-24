plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0-Beta"
    id("org.jetbrains.intellij") version "1.14.1"
}

group = "myFindAction"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.1")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("232.*")
    }
}
