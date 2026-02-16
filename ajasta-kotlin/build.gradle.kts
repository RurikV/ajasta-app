// Root project build file
// This provides convenience tasks to run all tests from the root

tasks.register("test") {
    group = "verification"
    description = "Runs all tests in all subprojects"

    dependsOn(gradle.includedBuild("ajasta-be").task(":ajasta-api-v1-jackson:test"))
    dependsOn(gradle.includedBuild("ajasta-be").task(":ajasta-api-v1-mappers:test"))
}

tasks.register("build") {
    group = "build"
    description = "Builds all subprojects"

    dependsOn(gradle.includedBuild("ajasta-be").task(":ajasta-api-v1-jackson:build"))
    dependsOn(gradle.includedBuild("ajasta-be").task(":ajasta-common:build"))
    dependsOn(gradle.includedBuild("ajasta-be").task(":ajasta-api-v1-mappers:build"))
}
