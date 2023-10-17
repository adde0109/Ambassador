plugins {
    java
    idea
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.adde0109"
version = "1.4.3-beta"

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
    compileOnly("com.electronwill.night-config:toml:3.6.6")
    implementation("org.bstats:bstats-velocity:3.0.1")
    compileOnly("io.netty:netty-buffer:4.1.90.Final")
    compileOnly("io.netty:netty-transport:4.1.90.Final")
    compileOnly("io.netty:netty-codec:4.1.90.Final")
    compileOnly("io.netty:netty-handler:4.1.90.Final")
}

tasks {
    jar {
        dependsOn(shadowJar);
        enabled = false;
    }
    shadowJar {
        relocate("org.bstats", "org.adde0109.ambassador")
        archiveBaseName.set("Ambassador-Velocity")
    }
}
