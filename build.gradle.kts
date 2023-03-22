plugins {
    java
    idea
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    implementation("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    implementation("com.electronwill.night-config:toml:3.6.6")
    implementation("org.bstats:bstats-velocity:3.0.1")
    implementation("org.apache.commons:commons-collections4:4.4")
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    shadowJar {
        relocate("org.bstats", "org.adde0109.ambassador")
        archiveBaseName.set("Ambassador-Velocity")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
