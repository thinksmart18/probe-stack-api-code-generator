package com.probe.stack.code.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the code generator
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "codegen")
public class CodeGeneratorConfig {

    @NotBlank
    private String outputBaseDir;

    @NotBlank
    private String templateConfigDir; // NEW: Main template directory

    @NotBlank
    private String tempDir;

    private int cleanupHours;

    private GeneratorConfig generator = new GeneratorConfig();

    private TemplateConfig templates = new TemplateConfig(); // NEW

    private PomConfig pom = new PomConfig();

    private Map<String, String> applicationProperties = new HashMap<>();

    @Data
    public static class GeneratorConfig {
        private String language = "spring";
        private String library = "spring-boot";
        private String apiPackageSuffix = "api";
        private String modelPackageSuffix = "model";
    }

    @Data
    public static class TemplateConfig {
        private String javaSourceDir = "java_code/src/main/java";
        private String resourcesDir = "java_code/src/main/resources";
        private String pomTemplate = "maven_config";
    }

    @Data
    public static class PomConfig {
        private Map<String, String> properties = new HashMap<>();
        private List<DependencyConfig> dependencies;
    }

    @Data
    public static class DependencyConfig {
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;
    }
}