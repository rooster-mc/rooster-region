plugins {
    kotlin("jvm") version "2.4.20" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

allprojects {
    group = "dev.rooster.region"
    version = "1.0-SNAPSHOT"
}
