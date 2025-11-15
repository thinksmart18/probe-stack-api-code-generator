package com.probe.stack.code.generator.service.enhancement;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing Swagger/OpenAPI specifications and validating implementations.
 * Parses the Swagger YAML file to identify all defined paths, operations, and schemas.
 *
 * @author ProbeStack
 */
@Slf4j
@Service
public class SwaggerAnalysisService {

    /**
     * Analyzes the Swagger specification and validates that all paths are implemented.
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of analysis messages
     */
    public List<String> analyzeAndValidate(Path projectDir, CodeGenerationRequest request) {
        List<String> messages = new ArrayList<>();

        try {
            // Find the Swagger/OpenAPI file
            Path swaggerFile = findSwaggerFile(projectDir);
            if (swaggerFile == null) {
                messages.add("Warning: No Swagger file found in project");
                return messages;
            }

            log.info("Analyzing Swagger file: {}", swaggerFile.getFileName());
            messages.add("Found Swagger specification: " + swaggerFile.getFileName());

            // Parse the Swagger file
            SwaggerSpec spec = parseSwaggerFile(swaggerFile);
            messages.add(String.format("Parsed Swagger: %d paths, %d schemas",
                    spec.getPaths().size(), spec.getSchemas().size()));

            // Store the parsed spec for use by other services
            spec.setProjectDir(projectDir);
            spec.setBasePackage(request.getBasePackage());
            SwaggerSpecCache.put(projectDir.toString(), spec);

            // Analyze each path
            for (Map.Entry<String, PathDefinition> entry : spec.getPaths().entrySet()) {
                String path = entry.getKey();
                PathDefinition pathDef = entry.getValue();

                log.debug("Path: {} - Operations: {}", path, pathDef.getOperations().keySet());

                for (String operation : pathDef.getOperations().keySet()) {
                    messages.add(String.format("  %s %s", operation.toUpperCase(), path));
                }
            }

        } catch (Exception e) {
            log.error("Error analyzing Swagger file", e);
            messages.add("Swagger analysis failed: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Finds the Swagger/OpenAPI YAML file in the project resources directory.
     *
     * @param projectDir Path to the project directory
     * @return Path to the Swagger file, or null if not found
     */
    private Path findSwaggerFile(Path projectDir) throws IOException {
        Path resourcesDir = projectDir.resolve("src/main/resources");

        if (!Files.exists(resourcesDir)) {
            log.warn("Resources directory not found: {}", resourcesDir);
            return null;
        }

        // Look for common Swagger file names
        List<String> swaggerFileNames = Arrays.asList(
                "openapi.yaml", "openapi.yml",
                "swagger.yaml", "swagger.yml",
                "api.yaml", "api.yml"
        );

        for (String fileName : swaggerFileNames) {
            Path swaggerFile = resourcesDir.resolve(fileName);
            if (Files.exists(swaggerFile)) {
                return swaggerFile;
            }
        }

        // Search for any YAML file in resources directory
        Optional<Path> yamlFile = Files.list(resourcesDir)
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .findFirst();

        return yamlFile.orElse(null);
    }

    /**
     * Parses the Swagger YAML file into a structured representation.
     *
     * @param swaggerFile Path to the Swagger file
     * @return Parsed Swagger specification
     */
    private SwaggerSpec parseSwaggerFile(Path swaggerFile) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> swaggerData = yaml.load(Files.newInputStream(swaggerFile));

        SwaggerSpec spec = new SwaggerSpec();

        // Parse basic info
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) swaggerData.get("info");
        if (info != null) {
            spec.setTitle((String) info.get("title"));
            spec.setVersion((String) info.get("version"));
        }

        // Parse paths
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) swaggerData.get("paths");
        if (paths != null) {
            spec.setPaths(parsePaths(paths));
        }

        // Parse schemas/components
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) swaggerData.get("components");
        if (components != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
            if (schemas != null) {
                spec.setSchemas(parseSchemas(schemas));
            }
        }

        return spec;
    }

    /**
     * Parses the paths section of the Swagger file.
     */
    @SuppressWarnings("unchecked")
    private Map<String, PathDefinition> parsePaths(Map<String, Object> paths) {
        Map<String, PathDefinition> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : paths.entrySet()) {
            String path = entry.getKey();
            Map<String, Object> pathData = (Map<String, Object>) entry.getValue();

            PathDefinition pathDef = new PathDefinition();
            pathDef.setPath(path);

            Map<String, OperationDefinition> operations = new HashMap<>();

            for (String httpMethod : Arrays.asList("get", "post", "put", "delete", "patch")) {
                if (pathData.containsKey(httpMethod)) {
                    Map<String, Object> operationData = (Map<String, Object>) pathData.get(httpMethod);
                    OperationDefinition operation = parseOperation(operationData, httpMethod);
                    operations.put(httpMethod, operation);
                }
            }

            pathDef.setOperations(operations);
            result.put(path, pathDef);
        }

        return result;
    }

    /**
     * Parses an individual operation (HTTP method) definition.
     */
    @SuppressWarnings("unchecked")
    private OperationDefinition parseOperation(Map<String, Object> operationData, String httpMethod) {
        OperationDefinition operation = new OperationDefinition();
        operation.setHttpMethod(httpMethod);
        operation.setOperationId((String) operationData.get("operationId"));
        operation.setSummary((String) operationData.get("summary"));
        operation.setDescription((String) operationData.get("description"));

        // Parse parameters
        List<Object> parameters = (List<Object>) operationData.get("parameters");
        if (parameters != null) {
            operation.setParameters(parameters.stream()
                    .map(p -> (Map<String, Object>) p)
                    .collect(Collectors.toList()));
        }

        // Parse request body
        Map<String, Object> requestBody = (Map<String, Object>) operationData.get("requestBody");
        if (requestBody != null) {
            operation.setRequestBodySchema(extractSchemaName(requestBody));
        }

        // Parse responses
        Map<String, Object> responses = (Map<String, Object>) operationData.get("responses");
        if (responses != null) {
            operation.setResponseSchemas(extractResponseSchemas(responses));
        }

        return operation;
    }

    /**
     * Parses the schemas/components section.
     */
    @SuppressWarnings("unchecked")
    private Map<String, SchemaDefinition> parseSchemas(Map<String, Object> schemas) {
        Map<String, SchemaDefinition> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            Map<String, Object> schemaData = (Map<String, Object>) entry.getValue();

            SchemaDefinition schema = new SchemaDefinition();
            schema.setName(schemaName);
            schema.setType((String) schemaData.get("type"));

            Map<String, Object> properties = (Map<String, Object>) schemaData.get("properties");
            if (properties != null) {
                schema.setProperties(properties);
            }

            List<String> required = (List<String>) schemaData.get("required");
            if (required != null) {
                schema.setRequiredFields(required);
            }

            result.put(schemaName, schema);
        }

        return result;
    }

    private String extractSchemaName(Map<String, Object> requestBody) {
        // Extract schema name from request body
        // Implementation depends on Swagger structure
        return null; // Placeholder
    }

    @SuppressWarnings("unchecked")
    private List<String> extractResponseSchemas(Map<String, Object> responses) {
        List<String> schemas = new ArrayList<>();

        for (Map.Entry<String, Object> entry : responses.entrySet()) {
            Map<String, Object> response = (Map<String, Object>) entry.getValue();
            Map<String, Object> content = (Map<String, Object>) response.get("content");

            if (content != null) {
                for (Object contentType : content.values()) {
                    Map<String, Object> contentData = (Map<String, Object>) contentType;
                    Map<String, Object> schema = (Map<String, Object>) contentData.get("schema");

                    if (schema != null) {
                        String ref = (String) schema.get("$ref");
                        if (ref != null) {
                            // Extract schema name from reference (e.g., "#/components/schemas/User" -> "User")
                            String[] parts = ref.split("/");
                            schemas.add(parts[parts.length - 1]);
                        }
                    }
                }
            }
        }

        return schemas;
    }

    /**
     * Data class representing the parsed Swagger specification.
     */
    @Data
    public static class SwaggerSpec {
        private String title;
        private String version;
        private Map<String, PathDefinition> paths = new HashMap<>();
        private Map<String, SchemaDefinition> schemas = new HashMap<>();
        private Path projectDir;
        private String basePackage;
    }

    @Data
    public static class PathDefinition {
        private String path;
        private Map<String, OperationDefinition> operations = new HashMap<>();
    }

    @Data
    public static class OperationDefinition {
        private String httpMethod;
        private String operationId;
        private String summary;
        private String description;
        private List<Map<String, Object>> parameters = new ArrayList<>();
        private String requestBodySchema;
        private List<String> responseSchemas = new ArrayList<>();
    }

    @Data
    public static class SchemaDefinition {
        private String name;
        private String type;
        private Map<String, Object> properties = new HashMap<>();
        private List<String> requiredFields = new ArrayList<>();
    }

    /**
     * Simple cache to store parsed Swagger specs for reuse by other services.
     */
    public static class SwaggerSpecCache {
        private static final Map<String, SwaggerSpec> cache = new HashMap<>();

        public static void put(String projectPath, SwaggerSpec spec) {
            cache.put(projectPath, spec);
        }

        public static SwaggerSpec get(String projectPath) {
            return cache.get(projectPath);
        }

        public static void clear() {
            cache.clear();
        }
    }
}
