import java.util.zip.ZipFile

plugins {
    kotlin("jvm")
    `java-library`
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
}

val artifactName = "rooster-region-worldedit"

base {
    archivesName.set(artifactName)
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    api(project(":core"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // compileOnly so the BOM only constrains the compile classpath; the consumer
    // chooses the WorldEdit implementation at runtime.
    compileOnly(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    // The Bukkit adapter drags the server jar and FAWE plugins; only its own API
    // is needed to compile against com.sk89q.worldedit.bukkit.
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
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

tasks.register("verifyWorldEditClasspath") {
    group = "verification"
    description = "Asserts the WorldEdit API is on the compile classpath only."

    val compileClasspath = configurations.named("compileClasspath")
    val runtimeClasspath = configurations.named("runtimeClasspath")

    doLast {
        // allComponents includes this project's own component, which is not a
        // dependency and must not be compared against the expected lists.
        fun externalModuleNames(
            configuration: org.gradle.api.artifacts.Configuration
        ): List<String> =
            configuration.incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .map { "${it.group}:${it.name}" }
                .filterNot { it == "dev.rooster.region:worldedit" }
                .sorted()

        fun jarShips(jarNamePrefix: String, entry: String): Boolean {
            val jar =
                compileClasspath.get().files.firstOrNull { it.name.startsWith(jarNamePrefix) }
                    ?: error("worldedit compileClasspath must resolve the $jarNamePrefix jar")
            return ZipFile(jar).use { it.getEntry(entry) != null }
        }

        val compileModules = externalModuleNames(compileClasspath.get())
        check(compileModules.contains("dev.rooster.region:core")) {
            "worldedit compileClasspath must include the core project: $compileModules"
        }
        check(compileModules.contains("com.fastasyncworldedit:FastAsyncWorldEdit-Core")) {
            "worldedit compileClasspath must include the WorldEdit API: $compileModules"
        }

        val coreEntry = "com/sk89q/worldedit/regions/CuboidRegion.class"
        check(jarShips("FastAsyncWorldEdit-Core", coreEntry)) {
            "FAWE-Core jar must ship the WorldEdit core API"
        }
        val bukkitEntry = "com/sk89q/worldedit/bukkit/BukkitAdapter.class"
        check(jarShips("FastAsyncWorldEdit-Bukkit", bukkitEntry)) {
            "FAWE-Bukkit jar must ship the WorldEdit Bukkit adapter API"
        }

        val runtimeModules = externalModuleNames(runtimeClasspath.get())
        val leaked =
            runtimeModules.filter {
                it.startsWith("com.fastasyncworldedit") || it.startsWith("com.sk89q.worldedit")
            }
        check(leaked.isEmpty()) {
            "worldedit runtimeClasspath must not include WorldEdit: $leaked"
        }
    }
}

tasks.named("check") {
    dependsOn("verifyWorldEditClasspath")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = artifactName
        }
    }
}
