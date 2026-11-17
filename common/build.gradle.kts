plugins {
    `java-library`
}

dependencies {
    api("com.microsoft.sqlserver:mssql-jdbc:13.5.1.jre8-preview")
    api("com.fasterxml.jackson.core:jackson-databind:2.22.4")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.4")
    implementation("com.azure:azure-communication-email:1.1.0")
}
