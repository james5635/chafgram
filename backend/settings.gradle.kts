plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "chafgram"

// The Spring Boot application lives in the `api` subproject. Additional
// backend modules (workers, jobs, …) can be added here as the project grows.
include("api")
include("seed")
