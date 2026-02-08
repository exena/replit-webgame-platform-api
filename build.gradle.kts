plugins {
	java
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm")
}

group = "com.arcadex"
version = "0.0.1-SNAPSHOT"
description = "Vibe coding project for webgame platform api"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2025.1.0"
extra["awsMysqlJdbcVersion"] = "1.1.11"
extra["logstashLogbackEncoderVersion"] = "7.4"
extra["openapiVersion"] = "2.3.0"
extra["gsonVersion"] = "2.10.1"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // OpenFeign
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Log
    implementation("net.logstash.logback:logstash-logback-encoder:${property("logstashLogbackEncoderVersion")}")

    // JDBC
    // implementation("software.aws.rds:aws-mysql-jdbc:${property("awsMysqlJdbcVersion")}")
    implementation("org.postgresql:postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("openapiVersion")}")

    // Gson
    implementation("com.google.code.gson:gson:${property("gsonVersion")}")

    // Test
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
//	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
//	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
//	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
//	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
//	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
//	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
//	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
