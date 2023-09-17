plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
}

group = "it.unibo.collektive"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.bundles.kotlin.testing)
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
                freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        showCauses = true
        showStackTraces = true
        events(*org.gradle.api.tasks.testing.logging.TestLogEvent.values())
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

publishOnCentral {
    projectLongName = "Collektive-Alchemist integration"
    projectDescription = "Integration of the Collektive DSL into the Alchemist simulator"
    licenseName = "MIT License"
    licenseUrl = "https://opensource.org/license/mit/"
    publishing {
        publications {
            withType<MavenPublication> {
                if ("OSSRH" !in name) {
                    artifact(tasks.javadocJar)
                }
                scmConnection = "git:git@github.com:Collektive/${rootProject.name}"
                projectUrl = "https://github.com/Collektive/${rootProject.name}"
                pom {
                    developers {
                        developer {
                            name = "Elisa Tronetti"
                            email = "elisa.tronetti@studio.unibo.it"
                            url = "https://github.com/ElisaTronetti"
                        }
                        developer {
                            name = "Nicolas Farabegoli"
                            email = "nicolas.farabegoli@unibo.it"
                            url = "https://nicolasfarabegoli.it"
                        }
                    }
                }
            }
        }
    }
}

// Enforce the use of the Kotlin version
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(rootProject.libs.versions.kotlin.get())
        }
    }
}
