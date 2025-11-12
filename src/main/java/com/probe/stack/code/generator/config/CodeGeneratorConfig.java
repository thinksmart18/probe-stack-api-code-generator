package com.probe.stack.code.generator.config;

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

    // Getters and Setters
    public String getOutputBaseDir() {
        return outputBaseDir;
    }

    public void setOutputBaseDir(String outputBaseDir) {
        this.outputBaseDir = outputBaseDir;
    }

    public String getTemplateConfigDir() {
        return templateConfigDir;
    }

    public void setTemplateConfigDir(String templateConfigDir) {
        this.templateConfigDir = templateConfigDir;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public int getCleanupHours() {
        return cleanupHours;
    }

    public void setCleanupHours(int cleanupHours) {
        this.cleanupHours = cleanupHours;
    }

    public GeneratorConfig getGenerator() {
        return generator;
    }

    public void setGenerator(GeneratorConfig generator) {
        this.generator = generator;
    }

    public TemplateConfig getTemplates() {
        return templates;
    }

    public void setTemplates(TemplateConfig templates) {
        this.templates = templates;
    }

    public PomConfig getPom() {
        return pom;
    }

    public void setPom(PomConfig pom) {
        this.pom = pom;
    }

    public Map<String, String> getApplicationProperties() {
        return applicationProperties;
    }

    public void setApplicationProperties(Map<String, String> applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public static class GeneratorConfig {
        private String language = "spring";
        private String library = "spring-boot";
        private String apiPackageSuffix = "api";
        private String modelPackageSuffix = "model";

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(String library) {
            this.library = library;
        }

        public String getApiPackageSuffix() {
            return apiPackageSuffix;
        }

        public void setApiPackageSuffix(String apiPackageSuffix) {
            this.apiPackageSuffix = apiPackageSuffix;
        }

        public String getModelPackageSuffix() {
            return modelPackageSuffix;
        }

        public void setModelPackageSuffix(String modelPackageSuffix) {
            this.modelPackageSuffix = modelPackageSuffix;
        }
    }

    public static class TemplateConfig {
        private String javaSourceDir = "java_code/src/main/java";
        private String resourcesDir = "java_code/src/main/resources";
        private String pomTemplate = "maven_config";

        public String getJavaSourceDir() {
            return javaSourceDir;
        }

        public void setJavaSourceDir(String javaSourceDir) {
            this.javaSourceDir = javaSourceDir;
        }

        public String getResourcesDir() {
            return resourcesDir;
        }

        public void setResourcesDir(String resourcesDir) {
            this.resourcesDir = resourcesDir;
        }

        public String getPomTemplate() {
            return pomTemplate;
        }

        public void setPomTemplate(String pomTemplate) {
            this.pomTemplate = pomTemplate;
        }
    }

    public static class PomConfig {
        private Map<String, String> properties = new HashMap<>();
        private List<DependencyConfig> dependencies;

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public List<DependencyConfig> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<DependencyConfig> dependencies) {
            this.dependencies = dependencies;
        }
    }

    public static class DependencyConfig {
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
