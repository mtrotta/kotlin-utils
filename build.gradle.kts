import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
}

group = "org.matteo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion: String by extra("5.4.2")

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    implementation("org.slf4j:slf4j-api:1.7.26")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

afterEvaluate {
    tasks.withType(KotlinCompile::class)
        .forEach {
            it.kotlinOptions { freeCompilerArgs = listOf("-Xnew-inference") }
        }
}