plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.dokka") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("plugin.allopen") version "1.9.25"
}

group = "com.chatapp"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Validation
    implementation("commons-validator:commons-validator:1.7")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.25")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Forcer Jackson 2.15.3 UNIQUEMENT pour les configurations Dokka
configurations.matching { it.name.contains("dokka", ignoreCase = true) }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.fasterxml.jackson.core" ||
            requested.group == "com.fasterxml.jackson.module" ||
            requested.group == "com.fasterxml.jackson.dataformat") {
            useVersion("2.15.3")
            because("Dokka 1.9.20 requires Jackson 2.15.x")
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.dokkaHtml.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka").get().asFile)
}