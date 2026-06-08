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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    runtimeOnly("com.h2database:h2")
}
