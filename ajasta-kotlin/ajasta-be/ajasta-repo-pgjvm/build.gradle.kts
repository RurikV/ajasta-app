plugins {
    id("build-jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(project(":ajasta-common"))
    api(project(":ajasta-repo-common"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.uuid)

    implementation(libs.postgres.driver)
    implementation(libs.bundles.exposed)

    testImplementation(kotlin("test-junit"))
    testImplementation(project(":ajasta-repo-tests-resource"))
    testImplementation(project(":ajasta-repo-tests-booking"))
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.logback.classic)
    testImplementation(libs.kotlinx.coroutines.test)
}
