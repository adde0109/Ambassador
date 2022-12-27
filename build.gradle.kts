plugins {
    java
    idea
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.adde0109"
version = "0.5.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
    implementation("com.electronwill.night-config:toml:3.6.6")
    implementation("org.bstats:bstats-velocity:3.0.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    annotationProcessor("com.velocitypowered:velocity-api")
}

tasks {
    shadowJar {
        relocate("org.bstats", "org.adde0109.ambassador")
        archiveBaseName.set("Ambassador-Velocity")
    }
}
