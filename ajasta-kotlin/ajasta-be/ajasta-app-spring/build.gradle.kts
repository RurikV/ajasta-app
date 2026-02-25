plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependencies)
    alias(libs.plugins.kotlin.spring)
    id("build-jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(libs.spring.actuator)
    implementation(libs.spring.webflux)
    implementation(libs.spring.webflux.ui)
    implementation(libs.spring.security.webflux)
    implementation(libs.spring.security.oauth2)
    implementation(libs.jackson.module.kotlin)
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.coroutines.reactive)

    // Internal modules
    implementation(project(":ajasta-common"))
    implementation(project(":ajasta-app-common"))
    implementation(project(":ajasta-api-v1-jackson"))
    implementation(project(":ajasta-api-v1-mappers"))
    implementation(project(":ajasta-stubs"))
    implementation(project(":ajasta-biz"))
    implementation(project(":ajasta-repo-common"))
    implementation(project(":ajasta-repo-inmemory"))
    implementation(project(":ajasta-repo-pgjvm"))

    // Logging
    implementation(libs.logback.classic)

    // Tests
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.spring.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.spring.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootBuildImage {
    builder.set("paketobuildpacks/builder-jammy-base:latest")
}
