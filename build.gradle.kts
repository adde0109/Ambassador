plugins {
    java
    idea
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.adde0109"
version = "1.1.7-alpha"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api")
    compileOnly("com.velocitypowered:velocity-proxy")
    annotationProcessor("com.velocitypowered:velocity-api")
    implementation("com.electronwill.night-config:toml:3.6.6")
    implementation("org.bstats:bstats-velocity:3.0.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    compileOnly("io.netty:netty-buffer:4.1.86.Final")
    compileOnly("io.netty:netty-transport:4.1.86.Final")
}

tasks {
    shadowJar {
        relocate("org.bstats", "org.adde0109.ambassador")
        archiveBaseName.set("Ambassador-Velocity")
    }
}
