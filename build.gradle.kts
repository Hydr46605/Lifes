plugins {
    `java-library`
}

val lifesVersion = providers.environmentVariable("LIFES_VERSION")
    .orElse(providers.gradleProperty("lifesVersion"))
    .get()

group = providers.gradleProperty("group").get()
version = lifesVersion
description = "Permadeath lives tracking with configurable death consequences."

base {
    archivesName = "Lifes"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

dependencies {
    val democracy = "vendor/democracy"

    compileOnly(files(
        "$democracy/democracy-annotations-0.7.0.jar",
        "$democracy/democracy-api-0.7.0.jar",
        "$democracy/democracy-core-0.7.0.jar",
        "$democracy/democracy-platform-paper-0.7.0.jar",
    ))
    annotationProcessor(files(
        "$democracy/democracy-annotations-0.7.0.jar",
        "$democracy/democracy-processor-0.7.0.jar",
    ))
    implementation(files(
        "$democracy/democracy-api-0.7.0.jar",
        "$democracy/democracy-core-0.7.0.jar",
        "$democracy/democracy-platform-paper-0.7.0.jar",
    ))

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.yaml:snakeyaml:2.7")
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(files("$democracy/democracy-testkit-0.7.0.jar"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
    // "processing" silences javac's complaint about third-party annotations
    // (Bukkit, JetBrains) that ship without a processor of their own.
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // Perf-contract tests are timing based; keep them quiet and serialized per class.
    systemProperty("java.util.logging.manager", "java.util.logging.LogManager")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as CoreJavadocOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

tasks.withType<ProcessResources>().configureEach {
    val version = providers.environmentVariable("LIFES_VERSION")
        .orElse(providers.gradleProperty("lifesVersion"))
    filesMatching("plugin.yml") {
        expand("version" to version.get())
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("democracy-") && it.extension == "jar" }
            .map { zipTree(it) }
    })
    manifest {
        attributes(
            "Implementation-Title" to base.archivesName.get(),
            "Implementation-Version" to lifesVersion,
            "Specification-Title" to "Lifes",
            "Specification-Version" to lifesVersion,
        )
    }
}

val validateVersion = tasks.register("validateVersion") {
    group = "verification"
    description = "Rejects build versions that are not valid Semantic Versioning 2.0.0 values."
    inputs.property("version", lifesVersion)
    doLast {
        val version = inputs.properties["version"] as String
        val pattern = Regex(
            """(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*)(?:\.(?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*))*))?(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?"""
        )
        require(pattern.matches(version)) {
            "Invalid Lifes version '$version'; expected a SemVer 2.0.0 value."
        }
    }
}

tasks.named("check") {
    dependsOn("validateVersion")
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the effective Lifes SemVer."
    val effectiveVersion = version.toString()
    doLast { println(effectiveVersion) }
}
