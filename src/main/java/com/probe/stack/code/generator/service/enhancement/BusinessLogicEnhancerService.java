package com.probe.stack.code.generator.service.enhancement;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service for enhancing business logic in generated service classes.
 * Implements CRUD operations with database persistence logic:
 * - Create: Insert new records
 * - Read: Retrieve existing records
 * - Update: Update existing records or insert if not found
 * - Delete: Remove records
 *
 * @author ProbeStack
 */
@Slf4j
@Service
public class BusinessLogicEnhancerService {

    /**
     * Enhances business logic in service classes with CRUD operations.
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of enhancement messages
     */
    public List<String> enhanceBusinessLogic(Path projectDir, CodeGenerationRequest request) {
        List<String> messages = new ArrayList<>();

        try {
            String servicePackage = request.getBasePackage() + ".service";
            Path serviceDir = projectDir.resolve("src/main/java")
                    .resolve(servicePackage.replace('.', '/'));

            if (!Files.exists(serviceDir)) {
                messages.add("Warning: Service directory not found, skipping business logic enhancement");
                return messages;
            }

            log.info("Enhancing business logic in service classes...");

            // Find all service class files
            try (Stream<Path> files = Files.walk(serviceDir)) {
                List<Path> serviceFiles = files
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith("Service.java"))
                        .toList();

                int enhancedCount = 0;
                for (Path serviceFile : serviceFiles) {
                    if (enhanceServiceClass(serviceFile, request)) {
                        enhancedCount++;
                        log.info("Enhanced business logic in: {}", serviceFile.getFileName());
                    }
                }

                messages.add(String.format("Enhanced business logic in %d service classes", enhancedCount));
            }

        } catch (Exception e) {
            log.error("Error enhancing business logic", e);
            messages.add("Business logic enhancement failed: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Enhances a single service class with CRUD operations.
     *
     * @param serviceFile Path to the service class file
     * @param request Original code generation request
     * @return true if enhancements were made, false otherwise
     */
    private boolean enhanceServiceClass(Path serviceFile, CodeGenerationRequest request) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(serviceFile);

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classOpt.isEmpty()) {
            log.warn("No class declaration found in: {}", serviceFile.getFileName());
            return false;
        }

        ClassOrInterfaceDeclaration serviceClass = classOpt.get();

        // Find the repository field
        Optional<FieldDeclaration> repoFieldOpt = serviceClass.getFields().stream()
                .filter(f -> f.getVariables().stream()
                        .anyMatch(v -> v.getType().toString().endsWith("Repository")))
                .findFirst();

        if (repoFieldOpt.isEmpty()) {
            log.warn("No repository field found in: {}", serviceClass.getNameAsString());
            return false;
        }

        FieldDeclaration repoField = repoFieldOpt.get();
        String repoVarName = repoField.getVariable(0).getNameAsString();
        String repoType = repoField.getVariable(0).getType().toString();

        // Determine the entity type from the repository type (e.g., UserRepository -> User)
        String entityType = extractEntityType(repoType);
        if (entityType == null) {
            log.warn("Could not determine entity type from repository: {}", repoType);
            return false;
        }

        boolean modified = false;

        // Add CRUD methods if they don't already exist
        if (!hasMethod(serviceClass, "create")) {
            addCreateMethod(serviceClass, entityType, repoVarName);
            modified = true;
        }

        if (!hasMethod(serviceClass, "findById")) {
            addFindByIdMethod(serviceClass, entityType, repoVarName);
            modified = true;
        }

        if (!hasMethod(serviceClass, "findAll")) {
            addFindAllMethod(serviceClass, entityType, repoVarName);
            modified = true;
        }

        if (!hasMethod(serviceClass, "update")) {
            addUpdateMethod(serviceClass, entityType, repoVarName);
            modified = true;
        }

        if (!hasMethod(serviceClass, "delete")) {
            addDeleteMethod(serviceClass, entityType, repoVarName);
            modified = true;
        }

        // Add necessary imports
        addImports(cu, entityType, request.getBasePackage());

        if (modified) {
            // Save the modified file
            Files.writeString(serviceFile, cu.toString());
            return true;
        }

        return false;
    }

    /**
     * Adds a create method for inserting new entities.
     */
    private void addCreateMethod(ClassOrInterfaceDeclaration serviceClass,
                                  String entityType, String repoVarName) {
        MethodDeclaration method = serviceClass.addMethod("create", Modifier.Keyword.PUBLIC);
        method.setType(entityType);
        method.addParameter(entityType, "entity");
        method.setJavadocComment("Creates a new " + entityType + " and persists it to the database.\n" +
                "@param entity The entity to create\n" +
                "@return The created entity");

        BlockStmt body = new BlockStmt();
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("log.info(\"Creating new %s: {}\", entity);", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("return %s.save(entity);", repoVarName)));

        method.setBody(body);
    }

    /**
     * Adds a findById method for retrieving entities by ID.
     */
    private void addFindByIdMethod(ClassOrInterfaceDeclaration serviceClass,
                                    String entityType, String repoVarName) {
        MethodDeclaration method = serviceClass.addMethod("findById", Modifier.Keyword.PUBLIC);
        method.setType(new ClassOrInterfaceType(null, "Optional")
                .setTypeArguments(new ClassOrInterfaceType(null, entityType)));
        method.addParameter("String", "id");
        method.setJavadocComment("Finds a " + entityType + " by its ID.\n" +
                "@param id The entity ID\n" +
                "@return Optional containing the entity if found");

        BlockStmt body = new BlockStmt();
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("log.debug(\"Finding %s by ID: {}\", id);", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("return %s.findById(id);", repoVarName)));

        method.setBody(body);
    }

    /**
     * Adds a findAll method for retrieving all entities.
     */
    private void addFindAllMethod(ClassOrInterfaceDeclaration serviceClass,
                                   String entityType, String repoVarName) {
        MethodDeclaration method = serviceClass.addMethod("findAll", Modifier.Keyword.PUBLIC);
        method.setType(new ClassOrInterfaceType(null, "List")
                .setTypeArguments(new ClassOrInterfaceType(null, entityType)));
        method.setJavadocComment("Retrieves all " + entityType + " entities.\n" +
                "@return List of all entities");

        BlockStmt body = new BlockStmt();
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("log.debug(\"Finding all %s entities\");", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("return %s.findAll();", repoVarName)));

        method.setBody(body);
    }

    /**
     * Adds an update method for updating existing entities or inserting new ones.
     */
    private void addUpdateMethod(ClassOrInterfaceDeclaration serviceClass,
                                  String entityType, String repoVarName) {
        MethodDeclaration method = serviceClass.addMethod("update", Modifier.Keyword.PUBLIC);
        method.setType(entityType);
        method.addParameter("String", "id");
        method.addParameter(entityType, "entity");
        method.setJavadocComment("Updates an existing " + entityType + " or creates it if not found.\n" +
                "@param id The entity ID\n" +
                "@param entity The updated entity data\n" +
                "@return The updated or created entity");

        BlockStmt body = new BlockStmt();
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("log.info(\"Updating %s with ID: {}\", id);", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("return %s.findById(id).map(existing -> {", repoVarName)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("    log.debug(\"Found existing %s, updating...\");", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("    return %s.save(entity);", repoVarName)));
        body.addStatement(StaticJavaParser.parseStatement("}).orElseGet(() -> {"));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("    log.info(\"%s not found, creating new one\");", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("    return %s.save(entity);", repoVarName)));
        body.addStatement(StaticJavaParser.parseStatement("});"));

        method.setBody(body);
    }

    /**
     * Adds a delete method for removing entities.
     */
    private void addDeleteMethod(ClassOrInterfaceDeclaration serviceClass,
                                  String entityType, String repoVarName) {
        MethodDeclaration method = serviceClass.addMethod("delete", Modifier.Keyword.PUBLIC);
        method.setType("void");
        method.addParameter("String", "id");
        method.setJavadocComment("Deletes a " + entityType + " by its ID.\n" +
                "@param id The entity ID to delete");

        BlockStmt body = new BlockStmt();
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("log.info(\"Deleting %s with ID: {}\", id);", entityType)));
        body.addStatement(StaticJavaParser.parseStatement(
                String.format("%s.deleteById(id);", repoVarName)));

        method.setBody(body);
    }

    /**
     * Checks if a method with the given name exists in the class.
     */
    private boolean hasMethod(ClassOrInterfaceDeclaration classDecl, String methodName) {
        return classDecl.getMethods().stream()
                .anyMatch(m -> m.getNameAsString().equals(methodName));
    }

    /**
     * Extracts the entity type from a repository type name.
     * For example: UserRepository -> User
     */
    private String extractEntityType(String repoType) {
        if (repoType.endsWith("Repository")) {
            return repoType.substring(0, repoType.length() - "Repository".length());
        }
        return null;
    }

    /**
     * Adds necessary imports to the compilation unit.
     */
    private void addImports(CompilationUnit cu, String entityType, String basePackage) {
        cu.addImport("java.util.List");
        cu.addImport("java.util.Optional");
        cu.addImport("lombok.extern.slf4j.Slf4j");

        // Add entity import if not in same package
        String modelPackage = basePackage + ".model";
        cu.addImport(modelPackage + "." + entityType);

        // Add annotation to class if not present
        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        classOpt.ifPresent(cls -> {
            boolean hasSlf4j = cls.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals("Slf4j"));
            if (!hasSlf4j) {
                cls.addAnnotation("Slf4j");
            }
        });
    }
}
