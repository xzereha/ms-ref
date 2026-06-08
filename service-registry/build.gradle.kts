plugins {
    java
    id("org.springframework.boot")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
