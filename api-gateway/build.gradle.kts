plugins {
    java
    id("org.springframework.boot")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

dependencies {
    implementation(project(":common"))
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
