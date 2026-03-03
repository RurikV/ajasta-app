plugins {
    id("build-kmp")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(projects.ajastaCommon)
                implementation(projects.ajastaRepoCommon)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.ajastaRepoTestsResource)
                implementation(projects.ajastaRepoTestsBooking)
            }
        }

        val jvmMain by getting

        val jvmTest by getting
    }
}
