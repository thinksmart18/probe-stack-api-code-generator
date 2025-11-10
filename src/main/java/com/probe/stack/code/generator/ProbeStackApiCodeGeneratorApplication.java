package com.probe.stack.code.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for OpenAPI Code Generator
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class ProbeStackApiCodeGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProbeStackApiCodeGeneratorApplication.class, args);
	}

}
