plugins {
    id("build-kmp")
}

kotlin {
    sourceSets {
        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(projects.ajastaLibCor)
                implementation(projects.ajastaCommon)
                implementation(projects.ajastaStubs)
                implementation(projects.ajastaRepoCommon)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val jvmMain by getting

        val jvmTest by getting
    }
}
