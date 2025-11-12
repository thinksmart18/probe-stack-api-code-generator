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
 * Maps to probe.stack.generator.* properties in application.yaml
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "probe.stack.generator")
public class CodeGeneratorConfig {

    private DirectoriesConfig directories = new DirectoriesConfig();
    private CleanupConfig cleanup = new CleanupConfig();
    private TemplateConfig templates = new TemplateConfig();
    private OpenApiConfig openapi = new OpenApiConfig();
    private HttpConfig http = new HttpConfig();
    private FilesConfig files = new FilesConfig();
    private EstimationConfig estimation = new EstimationConfig();
    private PomConfig pom = new PomConfig();
    private Map<String, String> applicationProperties = new HashMap<>();
    private DocumentationConfig documentation = new DocumentationConfig();

    @Data
    public static class DirectoriesConfig {
        @NotBlank
        private String outputBase;
        @NotBlank
        private String templateConfig;
        @NotBlank
        private String temp;
    }

    @Data
    public static class CleanupConfig {
        private int hours;
        private String cron;
    }

    @Data
    public static class HttpConfig {
        private int connectTimeoutMs = 30000;
        private int readTimeoutMs = 30000;
        private String userAgent = "OpenAPI-Code-Generator/1.0";
    }

    @Data
    public static class FilesConfig {
        private String defaultArchiveName = "generated-project.zip";
        private List<String> textFileExtensions = new java.util.ArrayList<>();
    }

    @Data
    public static class EstimationConfig {
        private double baseSizeMb = 5.0;
        private double perEndpointSizeMb = 0.05;
    }

    @Data
    public static class DocumentationConfig {
        private ReadmeConfig readme = new ReadmeConfig();

        @Data
        public static class ReadmeConfig {
            private int localPort = 8080;
            private String swaggerUiPath = "/swagger-ui.html";
            private String apiDocsPath = "/api-docs";
        }
    }

    @Data
    public static class OpenApiConfig {
        private GeneratorConfig generator = new GeneratorConfig();

        @Data
        public static class GeneratorConfig {
            private String language = "spring";
            private String library = "spring-boot";
            private String apiPackageSuffix = "api";
            private String modelPackageSuffix = "model";
        }
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