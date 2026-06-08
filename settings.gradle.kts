pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "microservices"

include(
    "common",
    "service-registry",
    "api-gateway",
    "user-service",
    "booking-service"
)
