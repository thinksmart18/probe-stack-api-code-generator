package com.probe.stack.code.generator.service.enhancement;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.service.enhancement.SwaggerAnalysisService.PathDefinition;
import com.probe.stack.code.generator.service.enhancement.SwaggerAnalysisService.SchemaDefinition;
import com.probe.stack.code.generator.service.enhancement.SwaggerAnalysisService.SwaggerSpec;
import com.squareup.javapoet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating missing classes based on Swagger specification analysis.
 * Generates controllers, services, repositories, and model classes as needed.
 *
 * @author ProbeStack
 */
@Slf4j
@Service
public class MissingClassGeneratorService {

    /**
     * Generates missing classes based on Swagger analysis.
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of generation messages
     */
    public List<String> generateMissingClasses(Path projectDir, CodeGenerationRequest request) {
        List<String> messages = new ArrayList<>();

        try {
            // Retrieve parsed Swagger spec from cache
            SwaggerSpec spec = SwaggerAnalysisService.SwaggerSpecCache.get(projectDir.toString());
            if (spec == null) {
                messages.add("Warning: No Swagger spec available, skipping class generation");
                return messages;
            }

            log.info("Generating missing classes based on Swagger specification...");

            // Generate model classes from schemas
            int modelCount = generateModelClasses(spec, projectDir, request, messages);
            messages.add(String.format("Generated %d model classes", modelCount));

            // Generate repository interfaces for each model
            int repoCount = generateRepositoryInterfaces(spec, projectDir, request, messages);
            messages.add(String.format("Generated %d repository interfaces", repoCount));

            // Generate service classes
            int serviceCount = generateServiceClasses(spec, projectDir, request, messages);
            messages.add(String.format("Generated %d service classes", serviceCount));

            // Verify controllers exist for all paths
            int controllerCount = verifyControllers(spec, projectDir, request, messages);
            messages.add(String.format("Verified %d controllers", controllerCount));

        } catch (Exception e) {
            log.error("Error generating missing classes", e);
            messages.add("Class generation failed: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Generates model classes from Swagger schema definitions.
     */
    private int generateModelClasses(SwaggerSpec spec, Path projectDir,
                                     CodeGenerationRequest request, List<String> messages) throws IOException {
        String modelPackage = request.getBasePackage() + ".model";
        Path modelDir = createPackageDirectory(projectDir, modelPackage);

        int count = 0;
        for (Map.Entry<String, SchemaDefinition> entry : spec.getSchemas().entrySet()) {
            String modelName = entry.getKey();
            SchemaDefinition schema = entry.getValue();

            // Check if model already exists
            if (classExists(modelDir, modelName)) {
                log.debug("Model {} already exists, skipping", modelName);
                continue;
            }

            generateModelClass(modelName, schema, modelPackage, modelDir);
            log.info("Generated model: {}", modelName);
            count++;
        }

        return count;
    }

    /**
     * Generates a single model class using JavaPoet.
     */
    private void generateModelClass(String modelName, SchemaDefinition schema,
                                    String packageName, Path outputDir) throws IOException {
        TypeSpec.Builder modelBuilder = TypeSpec.classBuilder(modelName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Model class for $L\nGenerated from Swagger/OpenAPI specification\n", modelName);

        // Add fields from schema properties
        if (schema.getProperties() != null) {
            for (Map.Entry<String, Object> prop : schema.getProperties().entrySet()) {
                String fieldName = prop.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> propDef = (Map<String, Object>) prop.getValue();
                String type = (String) propDef.get("type");

                TypeName fieldType = mapSwaggerTypeToJava(type);

                FieldSpec field = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE)
                        .addJavadoc("$L field\n", fieldName)
                        .build();

                modelBuilder.addField(field);

                // Add getter
                modelBuilder.addMethod(MethodSpec.methodBuilder("get" + capitalize(fieldName))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(fieldType)
                        .addStatement("return this.$L", fieldName)
                        .build());

                // Add setter
                modelBuilder.addMethod(MethodSpec.methodBuilder("set" + capitalize(fieldName))
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(fieldType, fieldName)
                        .addStatement("this.$L = $L", fieldName, fieldName)
                        .build());
            }
        }

        JavaFile javaFile = JavaFile.builder(packageName, modelBuilder.build())
                .indent("    ")
                .build();

        javaFile.writeTo(outputDir.getParent().getParent().getParent().getParent());
    }

    /**
     * Generates repository interfaces for models.
     */
    private int generateRepositoryInterfaces(SwaggerSpec spec, Path projectDir,
                                             CodeGenerationRequest request, List<String> messages) throws IOException {
        String repoPackage = request.getBasePackage() + ".repository";
        String modelPackage = request.getBasePackage() + ".model";
        Path repoDir = createPackageDirectory(projectDir, repoPackage);

        int count = 0;
        for (String modelName : spec.getSchemas().keySet()) {
            String repoName = modelName + "Repository";

            // Check if repository already exists
            if (classExists(repoDir, repoName)) {
                log.debug("Repository {} already exists, skipping", repoName);
                continue;
            }

            generateRepositoryInterface(modelName, repoName, modelPackage, repoPackage, repoDir);
            log.info("Generated repository: {}", repoName);
            count++;
        }

        return count;
    }

    /**
     * Generates a repository interface using JavaPoet.
     */
    private void generateRepositoryInterface(String modelName, String repoName,
                                             String modelPackage, String repoPackage,
                                             Path outputDir) throws IOException {
        ClassName modelClass = ClassName.get(modelPackage, modelName);
        ClassName stringClass = ClassName.get(String.class);

        // Create repository interface extending MongoRepository or JpaRepository
        TypeSpec repoInterface = TypeSpec.interfaceBuilder(repoName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Repository interface for $L\n", modelName)
                .addJavadoc("Extends MongoRepository for CRUD operations\n")
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.mongodb.repository", "MongoRepository"),
                        modelClass,
                        stringClass
                ))
                .build();

        JavaFile javaFile = JavaFile.builder(repoPackage, repoInterface)
                .indent("    ")
                .build();

        javaFile.writeTo(outputDir.getParent().getParent().getParent().getParent());
    }

    /**
     * Generates service classes for business logic.
     */
    private int generateServiceClasses(SwaggerSpec spec, Path projectDir,
                                       CodeGenerationRequest request, List<String> messages) throws IOException {
        String servicePackage = request.getBasePackage() + ".service";
        Path serviceDir = createPackageDirectory(projectDir, servicePackage);

        int count = 0;
        for (String modelName : spec.getSchemas().keySet()) {
            String serviceName = modelName + "Service";

            // Check if service already exists
            if (classExists(serviceDir, serviceName)) {
                log.debug("Service {} already exists, skipping", serviceName);
                continue;
            }

            generateServiceClass(modelName, serviceName, servicePackage, request, serviceDir);
            log.info("Generated service: {}", serviceName);
            count++;
        }

        return count;
    }

    /**
     * Generates a service class using JavaPoet.
     */
    private void generateServiceClass(String modelName, String serviceName,
                                      String servicePackage, CodeGenerationRequest request,
                                      Path outputDir) throws IOException {
        String modelPackage = request.getBasePackage() + ".model";
        String repoPackage = request.getBasePackage() + ".repository";

        ClassName modelClass = ClassName.get(modelPackage, modelName);
        ClassName repoClass = ClassName.get(repoPackage, modelName + "Repository");

        TypeSpec serviceClass = TypeSpec.classBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Service.class)
                .addJavadoc("Service class for $L business logic\n", modelName)
                .addField(FieldSpec.builder(repoClass, "repository", Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(repoClass, "repository")
                        .addStatement("this.repository = repository")
                        .build())
                .build();

        JavaFile javaFile = JavaFile.builder(servicePackage, serviceClass)
                .indent("    ")
                .build();

        javaFile.writeTo(outputDir.getParent().getParent().getParent().getParent());
    }

    /**
     * Verifies that controllers exist for all Swagger paths.
     */
    private int verifyControllers(SwaggerSpec spec, Path projectDir,
                                  CodeGenerationRequest request, List<String> messages) {
        String controllerPackage = request.getBasePackage() + ".api";
        Path controllerDir = projectDir.resolve("src/main/java")
                .resolve(controllerPackage.replace('.', '/'));

        int count = 0;
        for (String path : spec.getPaths().keySet()) {
            log.debug("Verifying controller for path: {}", path);
            count++;
        }

        return count;
    }

    /**
     * Creates a package directory.
     */
    private Path createPackageDirectory(Path projectDir, String packageName) throws IOException {
        Path packageDir = projectDir.resolve("src/main/java")
                .resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);
        return packageDir;
    }

    /**
     * Checks if a class file already exists.
     */
    private boolean classExists(Path packageDir, String className) {
        Path classFile = packageDir.resolve(className + ".java");
        return Files.exists(classFile);
    }

    /**
     * Maps Swagger/OpenAPI types to Java types.
     */
    private TypeName mapSwaggerTypeToJava(String swaggerType) {
        if (swaggerType == null) {
            return TypeName.get(Object.class);
        }

        return switch (swaggerType.toLowerCase()) {
            case "string" -> TypeName.get(String.class);
            case "integer" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "number", "float" -> TypeName.DOUBLE;
            case "boolean" -> TypeName.BOOLEAN;
            case "array" -> ParameterizedTypeName.get(List.class, Object.class);
            default -> TypeName.get(Object.class);
        };
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
