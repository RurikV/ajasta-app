plugins {
    id("build-jvm")
}

dependencies {
    implementation(projects.ajastaApiV1Jackson)
    implementation(projects.ajastaCommon)

    testImplementation(libs.bundles.testing)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
