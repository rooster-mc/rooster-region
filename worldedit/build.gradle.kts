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
        fun moduleNames(configuration: org.gradle.api.artifacts.Configuration): List<String> =
            configuration.incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .map { "${it.group}:${it.name}" }
                .filterNot { it == "dev.rooster.region:worldedit" }
                .sorted()

        val compileModules = moduleNames(compileClasspath.get())
        check(compileModules.contains("dev.rooster.region:core")) {
            "worldedit compileClasspath must include the core project: $compileModules"
        }
        check(compileModules.contains("com.fastasyncworldedit:FastAsyncWorldEdit-Core")) {
            "worldedit compileClasspath must include the WorldEdit API: $compileModules"
        }

        val coreJarName = "FastAsyncWorldEdit-Core"
        val faweCore =
            compileClasspath.get().files.firstOrNull { it.name.startsWith(coreJarName) }
                ?: error("worldedit compileClasspath must resolve the FAWE-Core jar")
        val apiClass =
            ZipFile(faweCore).use { zip ->
                zip.getEntry("com/sk89q/worldedit/regions/CuboidRegion.class")
            }
        check(apiClass != null) {
            "FAWE-Core jar must ship the com.sk89q.worldedit API: ${faweCore.name}"
        }

        val runtimeModules = moduleNames(runtimeClasspath.get())
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
