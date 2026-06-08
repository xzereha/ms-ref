plugins {
    java
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.microservices"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

tasks.withType<JavaCompile> {
    options.release = 25
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(26)
        }
    }

    tasks.withType<JavaCompile> {
        options.release = 25
    }

    repositories {
        mavenCentral()
    }

    extra["springCloudVersion"] = "2025.1.1"

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
