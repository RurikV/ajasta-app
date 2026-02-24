plugins {
    id("build-kmp")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))

                implementation(projects.ajastaCommon)
                implementation(projects.ajastaRepoCommon)
                // Booking tests need Resource types for resourceId reference
                implementation(projects.ajastaRepoTestsResource)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(kotlin("test-junit"))
            }
        }
    }
}
