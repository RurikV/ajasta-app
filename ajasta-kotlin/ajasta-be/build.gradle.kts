plugins {
    id("build-jvm") apply false
    id("build-kmp") apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

group = "top.ajasta"
version = libs.versions.ajasta.get()

ext {
    val specDir = layout.projectDirectory.dir("../specs")
    set("spec-booking-v1", specDir.file("specs-booking-v1.yaml").toString())
}

subprojects {
    group = rootProject.group as String
    version = rootProject.version
}
