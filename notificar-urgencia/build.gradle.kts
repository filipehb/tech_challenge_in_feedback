plugins {
    id("com.microsoft.azure.azurefunctions")
}

dependencies {
    implementation(project(":common"))
    implementation("com.microsoft.azure.functions:azure-functions-java-library:3.1.0")
}

azurefunctions {
    resourceGroup = "ReceberAvaliacao_group"
    appName = "NotificarUrgencia"
    pricingTier = "Consumption"
    region = "brazilsouth"
    runtime = com.microsoft.azure.gradle.configuration.GradleRuntimeConfig().apply {
        os("Linux")
        javaVersion("Java 21")
    }
    auth = com.microsoft.azure.gradle.auth.GradleAuthConfig().apply {
        type = "azure_cli"
    }
}
