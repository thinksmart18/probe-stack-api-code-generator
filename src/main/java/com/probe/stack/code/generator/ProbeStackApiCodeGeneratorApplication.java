package com.probe.stack.code.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for OpenAPI Code Generator.
 * Enhanced with Smart Agent functionality for intelligent request tracking and persistence.
 *
 * Features:
 * - OpenAPI code generation from specifications
 * - Smart Agent request tracking and persistence in MongoDB
 * - Scheduled cleanup of old projects
 * - GitHub integration for automatic repository creation
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
@EnableMongoRepositories(basePackages = "com.probe.stack.code.generator.repository")
public class ProbeStackApiCodeGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProbeStackApiCodeGeneratorApplication.class, args);
	}

}
