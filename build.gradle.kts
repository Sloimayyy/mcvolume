

plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "com.github.sloimayyy"
version = "1.0.5"



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
            groupId = project.group as String
            artifactId = "mcvolume"
            version = project.version as String

            from(components["java"])
        }
    }
}


