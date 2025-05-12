

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "com.github.sloimayyy"
version = "1.0.15"



repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io/")
}

dependencies {
    //implementation(kotlin("stdlib"))
    implementation("com.github.sloimayyy:smath:1.1.4")
    implementation("com.github.Querz:NBT:6.1")

    //implementation("it.unimi.dsi:fastutil:8.5.12")
}




publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar.get())
            groupId = "com.sloimay"
            artifactId = "mcvolumedev"
            version = project.version as String

            //from(components["java"])
        }
    }
}


tasks {
    shadowJar {
        exclude("it/unimi/dsi/fastutil/booleans/**")
        exclude("it/unimi/dsi/fastutil/bytes/**")
        exclude("it/unimi/dsi/fastutil/chars/**")
        exclude("it/unimi/dsi/fastutil/doubles/**")
        exclude("it/unimi/dsi/fastutil/floats/**")
        exclude("it/unimi/dsi/fastutil/io/**")
        exclude("it/unimi/dsi/fastutil/longs/**")

        // Your shadow configurations
        archiveClassifier.set("") // Remove the default "all" classifier
    }

    // Replace the default JAR with the shadow JAR
    jar {
        enabled = false // Disable the regular JAR
    }

    // Make sure the shadowJar task runs during the build
    build {
        dependsOn(shadowJar)
    }
}


