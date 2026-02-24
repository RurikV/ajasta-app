rootProject.name = "ajasta-be"

pluginManagement {
    includeBuild("../build-plugin")
    plugins {
        id("build-jvm") apply false
        id("build-kmp") apply false
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Lib Modules
include(":ajasta-lib-cor")

// API Modules
include(":ajasta-api-v1-jackson")
include(":ajasta-api-v1-mappers")

// Core Modules
include(":ajasta-common")
include(":ajasta-stubs")

// Business Logic Module
include(":ajasta-biz")

// Repository Modules
include(":ajasta-repo-common")
include(":ajasta-repo-inmemory")
include(":ajasta-repo-tests-resource")
include(":ajasta-repo-tests-booking")
include(":ajasta-repo-pgjvm")

// App Modules
include(":ajasta-app-common")
include(":ajasta-app-spring")
include(":ajasta-app-kafka")
