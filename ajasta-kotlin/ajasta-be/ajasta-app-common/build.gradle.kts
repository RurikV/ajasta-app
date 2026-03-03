plugins {
    id("build-jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(project(":ajasta-common"))
    implementation(project(":ajasta-api-v1-jackson"))
    implementation(project(":ajasta-api-v1-mappers"))
    implementation(project(":ajasta-stubs"))
    implementation(project(":ajasta-biz"))
    implementation(project(":ajasta-repo-common"))
    implementation(project(":ajasta-repo-inmemory"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)
    implementation(libs.jackson.module.kotlin)
}
