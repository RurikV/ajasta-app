plugins {
    id("build-jvm")
    alias(libs.plugins.openapi.generator)
}

val openapiGroup = "${rootProject.group}.api.v1"

openApiGenerate {
    generatorName.set("kotlin")
    packageName.set(openapiGroup)
    apiPackage.set("$openapiGroup.api")
    modelPackage.set("$openapiGroup.models")
    inputSpec.set(rootProject.ext["spec-booking-v1"] as String)

    globalProperties.apply {
        put("models", "")
        put("modelDocs", "false")
        put("apis", "false")
    }

    configOptions.set(
        mapOf(
            "dateLibrary" to "string",
            "enumPropertyNaming" to "UPPERCASE",
            "serializationLibrary" to "jackson",
            "collectionType" to "list"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.file("generate-resources/main/src/main/kotlin"))
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}

dependencies {
    implementation(libs.bundles.jackson)
    testImplementation(libs.bundles.testing)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
