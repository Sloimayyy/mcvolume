

plugins {
    kotlin("jvm") version "1.6.10"
    `maven-publish`
}

group = "com.sloimay"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("stdlib"))
    //testImplementation(kotlin("test"))
    implementation("com.sloimay:smath:1.0.0")

    implementation("com.github.Querz:NBT:6.1")
    //implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.beust:klaxon:5.5")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.sloimay"
            artifactId = "mcvolume"
            version = "1.0.0"

            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}


