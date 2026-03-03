plugins {
    application
    id("build-jvm")
    alias(libs.plugins.shadow.jar)
}

group = rootProject.group
version = rootProject.version

application {
    mainClass.set("top.ajasta.app.kafka.MainKt")
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Internal modules
    implementation(project(":ajasta-common"))
    implementation(project(":ajasta-app-common"))
    implementation(project(":ajasta-api-v1-jackson"))
    implementation(project(":ajasta-api-v1-mappers"))
    implementation(project(":ajasta-stubs"))
    implementation(project(":ajasta-biz"))
    implementation(project(":ajasta-repo-common"))
    implementation(project(":ajasta-repo-inmemory"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks {
    shadowJar {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass.get()))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
