plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"

    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "expensetracker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(
        "org.springframework.boot:spring-boot-starter-data-jpa"
    )

    implementation(
        "org.springframework.boot:spring-boot-starter-web"
    )

    implementation(
        "org.springframework.boot:spring-boot-starter-validation"
    )

    implementation(
        "org.springframework.boot:spring-boot-starter-flyway"
    )

    implementation(
        "org.springframework.boot:spring-boot-h2console"
    )

    implementation(
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
    )

    implementation(
        "com.fasterxml.jackson.module:jackson-module-kotlin"
    )

    implementation(
        "org.jetbrains.kotlin:kotlin-reflect"
    )

    implementation(
        kotlin("stdlib")
    )

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("com.google.firebase:firebase-admin:9.10.0")
    runtimeOnly(
        "org.flywaydb:flyway-database-postgresql"
    )

    runtimeOnly(
        "org.postgresql:postgresql"
    )

    developmentOnly(
        "org.springframework.boot:spring-boot-devtools"
    )

    testImplementation(
        "org.springframework.boot:spring-boot-starter-webmvc-test"
    )

    testImplementation(
        "org.springframework.security:spring-security-test"
    )

    testImplementation(
        "org.springframework.boot:spring-boot-starter-validation-test"
    )

    testImplementation(
        "org.springframework.boot:spring-boot-starter-data-jpa-test"
    )

    testImplementation(
        "org.springframework.boot:spring-boot-testcontainers"
    )

    testImplementation(
        "org.testcontainers:testcontainers-junit-jupiter:2.0.5"
    )

    testImplementation(
        "org.testcontainers:testcontainers-postgresql:2.0.5"
    )

    testImplementation(
        "org.mockito.kotlin:mockito-kotlin:6.1.0"
    )

    testImplementation(
        "io.mockk:mockk:1.13.17"
    )

    testImplementation(
        "org.jetbrains.kotlin:kotlin-test-junit5"
    )

    testRuntimeOnly(
        "org.junit.platform:junit-platform-launcher"
    )
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}