plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

import java.sql.Driver
import java.sql.DriverManager
import java.net.URLClassLoader
import java.util.Properties

group = "com.ragdoc"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    implementation(platform("software.amazon.awssdk:bom:2.29.45"))
    implementation("software.amazon.awssdk:s3")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.30")
    implementation("org.apache.pdfbox:pdfbox:3.0.5")
    implementation("com.pgvector:pgvector:0.1.6")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("createDatabase") {
    group = "database"
    description = "Creates the ragdoc PostgreSQL database if it does not exist."
    dependsOn("classes")

    doLast {
        val jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
        val username = "postgres"
        val password = project.findProperty("dbPassword")?.toString()
            ?: System.getenv("DB_PASSWORD")
            ?: throw GradleException("Set DB_PASSWORD env var or pass -PdbPassword=... for createDatabase task.")

        val classpath = configurations.getByName("runtimeClasspath")
        val loader = URLClassLoader(
            classpath.map { it.toURI().toURL() }.toTypedArray(),
            this::class.java.classLoader,
        )
        Thread.currentThread().contextClassLoader = loader
        val driverClass = Class.forName("org.postgresql.Driver", true, loader)
        val driver = driverClass.getDeclaredConstructor().newInstance() as Driver
        val properties = Properties().apply {
            setProperty("user", username)
            setProperty("password", password)
        }

        val connection = driver.connect(jdbcUrl, properties)
            ?: throw IllegalStateException("Failed to connect to PostgreSQL at $jdbcUrl")
        connection.use { conn ->
            conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?").use { statement ->
                statement.setString(1, "ragdoc")
                val exists = statement.executeQuery().next()

                if (exists) {
                    logger.lifecycle("Database 'ragdoc' already exists.")
                } else {
                    conn.createStatement().execute("CREATE DATABASE ragdoc")
                    logger.lifecycle("Database 'ragdoc' created successfully.")
                }
            }
        }
    }
}
