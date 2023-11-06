import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
    alias(libs.plugins.collektive)
}

group = "it.unibo.collektive"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.collektive)
    implementation(libs.bundles.alchemist)
    testImplementation(libs.bundles.kotlin.testing)
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
    target {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = false
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

multiJvm {
    jvmVersionForCompilation.set(17)
}

plugins.withType<DetektPlugin> {
    val check by tasks.getting
    val detektAll by tasks.creating { group = "verification" }
    tasks.withType<Detekt>()
        .matching { task ->
            task.name.let { it.endsWith("Main") || it.endsWith("Test") } && !task.name.contains("Baseline")
        }
        .all {
            check.dependsOn(this)
            detektAll.dependsOn(this)
        }
}

// Enforce the use of the Kotlin version
configurations.matching { it.name != "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(rootProject.libs.versions.kotlin.get())
        }
    }
}
