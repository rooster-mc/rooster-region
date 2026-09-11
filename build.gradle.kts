plugins {
    kotlin("jvm") version "2.4.20" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

allprojects {
    group = "dev.rooster.region"
    version = "1.0-SNAPSHOT"
}

tasks.register("verifyPublishedArtifacts") {
    group = "verification"
    description =
        "Publishes both modules to mavenLocal and asserts the installed POMs declare the expected dependencies."
    dependsOn(":core:publishToMavenLocal", ":worldedit:publishToMavenLocal")

    val projectVersion = version.toString()
    val localMavenRepo =
        System.getProperty("maven.repo.local")?.let { file(it) }
            ?: File(System.getProperty("user.home"), ".m2/repository")

    doLast {
        fun pomDependencies(pom: File): List<String> {
            check(pom.isFile) { "missing published POM: $pom" }
            val dependencyPattern =
                Regex(
                    "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>",
                )
            return dependencyPattern
                .findAll(pom.readText())
                .map { "${it.groupValues[1]}:${it.groupValues[2]}" }
                .toList()
        }

        fun installedPom(artifactId: String) =
            localMavenRepo.resolve(
                "dev/rooster/region/$artifactId/$projectVersion/$artifactId-$projectVersion.pom",
            )

        val coreDependencies = pomDependencies(installedPom("rooster-region"))
        check(coreDependencies == listOf("org.joml:joml")) {
            "published rooster-region POM must declare only joml but declares $coreDependencies"
        }

        val worldEditDependencies = pomDependencies(installedPom("rooster-region-worldedit"))
        check(worldEditDependencies == listOf("dev.rooster.region:rooster-region")) {
            "published rooster-region-worldedit POM must declare only core but declares " +
                "$worldEditDependencies"
        }
    }
}
