plugins {
    id("com.microsoft.azure.azurefunctions") version "1.14.0" apply false
}

allprojects {
    group = "com.filipearruda"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:6.0.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.mockito:mockito-core:5.14.2")
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.14.2")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}

tasks.register("azureFunctionsPackage") {
    dependsOn(
        ":receber-avaliacao:azureFunctionsPackage",
        ":notificar-urgencia:azureFunctionsPackage",
        ":relatorio-semanal:azureFunctionsPackage"
    )
}
