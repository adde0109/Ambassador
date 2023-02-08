plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.adde0109"
version = "1.1.7-alpha-bungeecord"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } // This lets gradle find the BungeeCord files online
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    implementation("org.javassist:javassist:3.29.2-GA")
}