// https://github.com/florianreuth/BaseProject

plugins {
    id("java-library")
}

group = property("project_group") as String
version = property("project_version") as String
(findProperty("project_description") as String?)?.let {
    description = it
}

if (project == rootProject) {
    (findProperty("project_name") as String?)?.let {
        base.archivesName = it
    }
}

val jvmVersion = (property("jvm_version") as String).toInt()
java {
    toolchain.languageVersion = JavaLanguageVersion.of(jvmVersion)
    sourceCompatibility = JavaVersion.toVersion(jvmVersion)
    targetCompatibility = JavaVersion.toVersion(jvmVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:-unchecked", "-Xlint:-deprecation"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("Xdoclint:none", true)
        quiet()
    }
}

tasks.jar {
    val projectName = project.name
    from("LICENSE") {
        rename {
            "LICENSE_$projectName"
        }
    }
}
