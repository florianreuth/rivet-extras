pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("base.settings")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.lenni0451.net/everything")
    }
}

rootProject.name = "rivet-extras"
