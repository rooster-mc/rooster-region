set shell := ["bash", "-uc"]

default:
    @just --list

# Assemble both modules
build:
    ./gradlew build

# Run the JUnit suite
test:
    ./gradlew test

# Format Kotlin sources with ktlint
format:
    ./gradlew ktlintFormat

# Publish both modules to mavenLocal
publish:
    ./gradlew publishToMavenLocal
