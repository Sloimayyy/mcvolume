

plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "com.github.sloimayyy"
version = "1.0.6"



repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.sloimayyy:smath:v1.0.4")
    implementation("com.github.Querz:NBT:6.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.sloimay"
            artifactId = "mcvolumedev"
            version = project.version as String

            from(components["java"])
        }
    }
}


