rootProject.name = "Ambassador"
includeBuild("Velocity") {
    dependencySubstitution {
        substitute(module("com.velocitypowered:velocity-api")).using(project(":velocity-api"))
        substitute(module("com.velocitypowered:velocity-proxy")).using(project(":velocity-proxy"))
    }
}