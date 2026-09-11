plugins {
    kotlin("jvm")
    `java-library`
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
}

val artifactName = "rooster-region"

base {
    archivesName.set(artifactName)
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    api("org.joml:joml:1.10.9")
    compileOnly(kotlin("stdlib"))

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.45.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("verifyCoreDependencies") {
    group = "verification"
    description =
        "Asserts the published core POM and runtime classpath are joml-only and the compile classpath has no framework/ORM/WorldEdit leaks."
    dependsOn(tasks.named("generatePomFileForMavenPublication"))

    val pomFile = layout.buildDirectory.file("publications/maven/pom-default.xml")
    val runtimeClasspath = configurations.named("runtimeClasspath")
    val compileClasspath = configurations.named("compileClasspath")

    doLast {
        val pomText = pomFile.get().asFile.readText()
        val dependencyPattern =
            Regex(
                "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>",
            )
        val dependencies =
            dependencyPattern
                .findAll(pomText)
                .map { "${it.groupValues[1]}:${it.groupValues[2]}" }
                .toList()
        check(dependencies == listOf("org.joml:joml")) {
            "rooster-region POM must declare only joml but declares $dependencies"
        }

        // allComponents includes this project's own component, which is not a
        // dependency and must not be compared against the expected lists.
        fun externalModuleNames(
            configuration: org.gradle.api.artifacts.Configuration
        ): List<String> =
            configuration.incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .map { "${it.group}:${it.name}" }
                .filterNot { it == "dev.rooster.region:core" }
                .sorted()

        val runtimeModules = externalModuleNames(runtimeClasspath.get())
        check(runtimeModules == listOf("org.joml:joml")) {
            "core runtimeClasspath must be joml-only but contains $runtimeModules"
        }

        val forbidden =
            listOf(
                "com.sk89q.worldedit",
                "com.fastasyncworldedit",
                "org.jetbrains.exposed",
                "dev.rooster",
            )
        val compileLeaks =
            externalModuleNames(compileClasspath.get())
                .filter { name -> forbidden.any { name.startsWith(it) } }
        check(compileLeaks.isEmpty()) {
            "core compileClasspath leaks forbidden modules: $compileLeaks"
        }
    }
}

tasks.named("check") {
    dependsOn("verifyCoreDependencies")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = artifactName
        }
    }
}
