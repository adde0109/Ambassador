rootProject.name = "Ambassador"

val velocityPath = rootProject.projectDir.toPath().resolve("Velocity/");
    includeBuild("Velocity") {
        dependencySubstitution {
            if(java.nio.file.Files.isDirectory(velocityPath.resolve("proxy/"))) {
                substitute(module("com.velocitypowered:velocity-proxy")).using(project(":velocity-proxy"))
                substitute(module("com.velocitypowered:velocity-api")).using(project(":velocity-api"))
            } else {
                logger.warn("Git Submodule 'Velocity' not initialized!")
            }
        }
    }
include("ambassador-velocity")
include("ambassador-bungeecord")