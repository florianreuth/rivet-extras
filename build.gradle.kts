plugins {
    id("publishing.florianreuth")
}

val demo: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations.named(demo.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    api(libs.rivet.core)

    "demoImplementation"(libs.rivet.backend.awt)
}
