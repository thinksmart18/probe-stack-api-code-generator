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
 * Maps to probe.stack.generator.* properties in application.yaml
 */
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

    public DirectoriesConfig getDirectories() {
        return directories;
    }

    public void setDirectories(DirectoriesConfig directories) {
        this.directories = directories;
    }

    public CleanupConfig getCleanup() {
        return cleanup;
    }

    public void setCleanup(CleanupConfig cleanup) {
        this.cleanup = cleanup;
    }

    public TemplateConfig getTemplates() {
        return templates;
    }

    public void setTemplates(TemplateConfig templates) {
        this.templates = templates;
    }

    public OpenApiConfig getOpenapi() {
        return openapi;
    }

    public void setOpenapi(OpenApiConfig openapi) {
        this.openapi = openapi;
    }

    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    public FilesConfig getFiles() {
        return files;
    }

    public void setFiles(FilesConfig files) {
        this.files = files;
    }

    public EstimationConfig getEstimation() {
        return estimation;
    }

    public void setEstimation(EstimationConfig estimation) {
        this.estimation = estimation;
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

    public DocumentationConfig getDocumentation() {
        return documentation;
    }

    public void setDocumentation(DocumentationConfig documentation) {
        this.documentation = documentation;
    }

    public static class DirectoriesConfig {
        @NotBlank
        private String outputBase;
        @NotBlank
        private String templateConfig;
        @NotBlank
        private String temp;

        public String getOutputBase() {
            return outputBase;
        }

        public void setOutputBase(String outputBase) {
            this.outputBase = outputBase;
        }

        public String getTemplateConfig() {
            return templateConfig;
        }

        public void setTemplateConfig(String templateConfig) {
            this.templateConfig = templateConfig;
        }

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }
    }

    public static class CleanupConfig {
        private int hours;
        private String cron;

        public int getHours() {
            return hours;
        }

        public void setHours(int hours) {
            this.hours = hours;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class HttpConfig {
        private int connectTimeoutMs = 30000;
        private int readTimeoutMs = 30000;
        private String userAgent = "OpenAPI-Code-Generator/1.0";

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }

    public static class FilesConfig {
        private String defaultArchiveName = "generated-project.zip";
        private List<String> textFileExtensions = new java.util.ArrayList<>();

        public String getDefaultArchiveName() {
            return defaultArchiveName;
        }

        public void setDefaultArchiveName(String defaultArchiveName) {
            this.defaultArchiveName = defaultArchiveName;
        }

        public List<String> getTextFileExtensions() {
            return textFileExtensions;
        }

        public void setTextFileExtensions(List<String> textFileExtensions) {
            this.textFileExtensions = textFileExtensions;
        }
    }

    public static class EstimationConfig {
        private double baseSizeMb = 5.0;
        private double perEndpointSizeMb = 0.05;

        public double getBaseSizeMb() {
            return baseSizeMb;
        }

        public void setBaseSizeMb(double baseSizeMb) {
            this.baseSizeMb = baseSizeMb;
        }

        public double getPerEndpointSizeMb() {
            return perEndpointSizeMb;
        }

        public void setPerEndpointSizeMb(double perEndpointSizeMb) {
            this.perEndpointSizeMb = perEndpointSizeMb;
        }
    }

    public static class DocumentationConfig {
        private ReadmeConfig readme = new ReadmeConfig();

        public ReadmeConfig getReadme() {
            return readme;
        }

        public void setReadme(ReadmeConfig readme) {
            this.readme = readme;
        }

        public static class ReadmeConfig {
            private int localPort = 8080;
            private String swaggerUiPath = "/swagger-ui.html";
            private String apiDocsPath = "/api-docs";

            public int getLocalPort() {
                return localPort;
            }

            public void setLocalPort(int localPort) {
                this.localPort = localPort;
            }

            public String getSwaggerUiPath() {
                return swaggerUiPath;
            }

            public void setSwaggerUiPath(String swaggerUiPath) {
                this.swaggerUiPath = swaggerUiPath;
            }

            public String getApiDocsPath() {
                return apiDocsPath;
            }

            public void setApiDocsPath(String apiDocsPath) {
                this.apiDocsPath = apiDocsPath;
            }
        }
    }

    public static class OpenApiConfig {
        private GeneratorConfig generator = new GeneratorConfig();

        public GeneratorConfig getGenerator() {
            return generator;
        }

        public void setGenerator(GeneratorConfig generator) {
            this.generator = generator;
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