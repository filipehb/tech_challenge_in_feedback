plugins {
    id("java")
    id("com.microsoft.azure.azurefunctions") version "1.14.0"
}

group = "com.filipearruda"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Azure Functions Java Library
    implementation("com.microsoft.azure.functions:azure-functions-java-library:3.1.0")

    // PostgreSQL JDBC Driver
    implementation("org.postgresql:postgresql:42.7.4")

    // JSON support
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // Testing
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

azurefunctions {
    resourceGroup = "ReceberAvaliacao_group"
    appName = "ReceberAvaliacao"
    pricingTier = "Consumption"
    region = "brazilsouth"
    runtime = com.microsoft.azure.gradle.configuration.GradleRuntimeConfig().apply {
        os("Linux")
        javaVersion("Java 21")
    }
    auth = com.microsoft.azure.gradle.auth.GradleAuthConfig().apply {
        type = "azure_cli"
    }
    localDebug = "transport=dt_socket,server=y,suspend=n,address=5005"
}