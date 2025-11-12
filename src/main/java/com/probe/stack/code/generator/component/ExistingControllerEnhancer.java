package com.probe.stack.code.generator.component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.MethodMetadata;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ParameterMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Modifies existing OpenAPI-generated controller classes to add service layer integration.
 * Uses traditional Spring annotations without Lombok.
 *
 * @author ProbeStack
 */
@Component
public class ExistingControllerEnhancer {

    private static final Logger log = LoggerFactory.getLogger(ExistingControllerEnhancer.class);
    private final JavaParser javaParser = new JavaParser();

    /**
     * Enhances an existing controller by adding service injection and implementations.
     */
    public void enhanceExistingController(
            ControllerMetadata metadata,
            File existingControllerFile,
            File outputDir
    ) throws IOException {

        log.info("Enhancing existing controller: {}", existingControllerFile.getName());

        // Parse existing controller
        CompilationUnit cu;
        try (FileInputStream in = new FileInputStream(existingControllerFile)) {
            cu = javaParser.parse(in).getResult()
                    .orElseThrow(() -> new IllegalArgumentException("Failed to parse controller file"));
        }

        // Find the controller class
        ClassOrInterfaceDeclaration controllerClass = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new IllegalArgumentException("No class found in controller file"));

        log.info("Found controller class: {}", controllerClass.getNameAsString());

        // Calculate service details FIRST (needed for imports)
        String serviceName = generateServiceName(metadata.getClassName());
        String servicePackage = calculateServicePackage(normalizePackageName(metadata.getPackageName()));
        String serviceFullyQualifiedName = servicePackage + "." + serviceName;

        log.info("Service details:");
        log.info("  Service Name: {}", serviceName);
        log.info("  Service Package: {}", servicePackage);
        log.info("  Service FQN: {}", serviceFullyQualifiedName);

        // Add necessary imports (including service import)
        addRequiredImports(cu, serviceFullyQualifiedName);

        // Remove Lombok imports if present
        removeLombokImports(cu);

        // Clean up unused imports
        cleanupUnusedImports(cu);

        // Remove Lombok annotations if present
        removeLombokAnnotations(controllerClass);

        // Add Logger field
        addLoggerField(controllerClass);

        // Add service field and constructor
        String serviceFieldName = addServiceFieldAndConstructor(controllerClass, metadata);

        // Remove getRequest() method that references non-existent request field
        removeGetRequestMethod(controllerClass);

        // Enhance or add methods
        enhanceMethods(controllerClass, metadata, serviceFieldName);

        // Write back to file
        writeEnhancedController(cu, existingControllerFile);

        log.info("Successfully enhanced controller: {}", existingControllerFile.getName());
    }

    /**
     * Adds required imports to the compilation unit.
     * NOW INCLUDES SERVICE IMPORT.
     */
    private void addRequiredImports(CompilationUnit cu, String serviceFullyQualifiedName) {
        // Standard Spring imports
        addImportIfNotPresent(cu, "org.springframework.beans.factory.annotation.Autowired");
        addImportIfNotPresent(cu, "org.springframework.http.HttpStatus");
        addImportIfNotPresent(cu, "org.springframework.http.ResponseEntity");

        // Logging imports
        addImportIfNotPresent(cu, "org.slf4j.Logger");
        addImportIfNotPresent(cu, "org.slf4j.LoggerFactory");

        // Service import - THIS IS THE KEY FIX
        addImportIfNotPresent(cu, serviceFullyQualifiedName);

        log.info("Added service import: {}", serviceFullyQualifiedName);
    }

    /**
     * Removes Lombok imports if present.
     */
    private void removeLombokImports(CompilationUnit cu) {
        cu.getImports().removeIf(imp ->
                imp.getNameAsString().startsWith("lombok.")
        );
        log.debug("Removed Lombok imports");
    }

    /**
     * Cleans up unused OpenAPI generated imports.
     */
    private void cleanupUnusedImports(CompilationUnit cu) {
        List<String> unusedImports = Arrays.asList(
                "org.springframework.web.context.request.NativeWebRequest",
                "org.springframework.web.multipart.MultipartFile",
                "org.springframework.web.bind.annotation.CookieValue",
                "org.springframework.web.bind.annotation.RequestHeader",
                "org.springframework.web.bind.annotation.RequestPart"
        );

        cu.getImports().removeIf(imp ->
                unusedImports.stream().anyMatch(unused -> imp.getNameAsString().equals(unused))
        );
        log.debug("Cleaned up unused imports");
    }

    /**
     * Removes Lombok annotations from the controller class.
     */
    private void removeLombokAnnotations(ClassOrInterfaceDeclaration controllerClass) {
        controllerClass.getAnnotations().removeIf(ann -> {
            String annName = ann.getNameAsString();
            boolean isLombok = annName.equals("Slf4j") ||
                    annName.equals("RequiredArgsConstructor") ||
                    annName.equals("Data") ||
                    annName.equals("Getter") ||
                    annName.equals("Setter");
            if (isLombok) {
                log.debug("Removed Lombok annotation: @{}", annName);
            }
            return isLombok;
        });
    }

    /**
     * Adds an import if it's not already present.
     */
    private void addImportIfNotPresent(CompilationUnit cu, String importName) {
        boolean hasImport = cu.getImports().stream()
                .anyMatch(imp -> imp.getNameAsString().equals(importName));

        if (!hasImport) {
            cu.addImport(importName);
            log.debug("Added import: {}", importName);
        }
    }

    /**
     * Adds Logger field to the controller class.
     */
    private void addLoggerField(ClassOrInterfaceDeclaration controllerClass) {
        String className = controllerClass.getNameAsString();

        // Check if logger already exists
        boolean hasLogger = controllerClass.getFields().stream()
                .anyMatch(f -> f.getVariables().stream()
                        .anyMatch(v -> v.getNameAsString().equals("log") ||
                                v.getNameAsString().equals("logger")));

        if (hasLogger) {
            log.debug("Logger field already exists");
            return;
        }

        // Create logger field: private static final Logger log = LoggerFactory.getLogger(ClassName.class);
        ClassOrInterfaceType loggerType = new ClassOrInterfaceType(null, "Logger");

        MethodCallExpr loggerInit = new MethodCallExpr(
                new NameExpr("LoggerFactory"),
                "getLogger",
                new NodeList<>(new ClassExpr(new ClassOrInterfaceType(null, className)))
        );

        VariableDeclarator loggerVar = new VariableDeclarator(
                loggerType,
                "log",
                loggerInit
        );

        // Create field with proper modifiers
        FieldDeclaration loggerField = new FieldDeclaration();
        loggerField.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
        loggerField.getVariables().add(loggerVar);

        // Add as first field
        controllerClass.getMembers().addFirst(loggerField);

        log.info("Added Logger field");
    }

    /**
     * Adds service field and @Autowired constructor.
     */
    private String addServiceFieldAndConstructor(
            ClassOrInterfaceDeclaration controllerClass,
            ControllerMetadata metadata
    ) {
        String serviceName = generateServiceName(metadata.getClassName());
        String serviceFieldName = toCamelCase(serviceName);

        log.info("Adding service field: {} {}", serviceName, serviceFieldName);

        // Check if service field already exists
        Optional<FieldDeclaration> existingField = controllerClass.getFields().stream()
                .filter(f -> f.getVariables().stream()
                        .anyMatch(v -> v.getNameAsString().equals(serviceFieldName)))
                .findFirst();

        if (existingField.isPresent()) {
            log.debug("Service field already exists: {}", serviceFieldName);
            return serviceFieldName;
        }

        // Remove NativeWebRequest field if present
        controllerClass.getFields().stream()
                .filter(f -> f.getVariables().stream()
                        .anyMatch(v -> v.getTypeAsString().contains("NativeWebRequest")))
                .findFirst()
                .ifPresent(field -> {
                    field.remove();
                    log.debug("Removed NativeWebRequest field");
                });

        // Add service field: private final ServiceType serviceFieldName;
        ClassOrInterfaceType serviceType = new ClassOrInterfaceType(null, serviceName);
        VariableDeclarator serviceVar = new VariableDeclarator(serviceType, serviceFieldName);

        FieldDeclaration serviceField = new FieldDeclaration();
        serviceField.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
        serviceField.getVariables().add(serviceVar);

        controllerClass.addMember(serviceField);
        log.info("Added service field: {} {}", serviceName, serviceFieldName);

        // Remove existing OpenAPI constructor if present
        controllerClass.getConstructors().stream()
                .filter(c -> c.getParameters().size() == 1)
                .filter(c -> c.getParameter(0).getTypeAsString().contains("NativeWebRequest"))
                .findFirst()
                .ifPresent(constructor -> {
                    constructor.remove();
                    log.debug("Removed existing OpenAPI constructor");
                });

        // Add @Autowired constructor
        ConstructorDeclaration constructor = new ConstructorDeclaration();
        constructor.setName(controllerClass.getNameAsString());
        constructor.setModifiers(Modifier.Keyword.PUBLIC);
        constructor.addAnnotation("Autowired");

        // Add parameter
        Parameter serviceParam = new Parameter(serviceType, serviceFieldName);
        constructor.addParameter(serviceParam);

        // Add constructor body: this.serviceFieldName = serviceFieldName;
        BlockStmt constructorBody = new BlockStmt();
        AssignExpr assignment = new AssignExpr(
                new FieldAccessExpr(new ThisExpr(), serviceFieldName),
                new NameExpr(serviceFieldName),
                AssignExpr.Operator.ASSIGN
        );
        constructorBody.addStatement(new ExpressionStmt(assignment));

        constructor.setBody(constructorBody);

        // Add constructor to class
        controllerClass.addMember(constructor);
        log.info("Added @Autowired constructor");

        return serviceFieldName;
    }

    /**
     * Removes the getRequest() method that references non-existent request field.
     */
    private void removeGetRequestMethod(ClassOrInterfaceDeclaration controllerClass) {
        controllerClass.getMethods().stream()
                .filter(m -> m.getNameAsString().equals("getRequest"))
                .filter(m -> m.getType().asString().contains("Optional"))
                .findFirst()
                .ifPresent(method -> {
                    method.remove();
                    log.debug("Removed getRequest() method");
                });
    }

    /**
     * Enhances or adds method implementations.
     */
    private void enhanceMethods(
            ClassOrInterfaceDeclaration controllerClass,
            ControllerMetadata metadata,
            String serviceFieldName
    ) {
        for (MethodMetadata methodMeta : metadata.getMethods()) {
            // Skip getRequest() utility method
            if (methodMeta.getMethodName().equals("getRequest")) {
                continue;
            }

            // Find existing method
            Optional<MethodDeclaration> existingMethod = controllerClass.getMethods().stream()
                    .filter(m -> m.getNameAsString().equals(methodMeta.getMethodName()))
                    .findFirst();

            if (existingMethod.isPresent()) {
                // Enhance existing method
                enhanceExistingMethod(existingMethod.get(), methodMeta, serviceFieldName);
                log.info("Enhanced existing method: {}", methodMeta.getMethodName());
            } else {
                // Add new method implementation
                addNewMethod(controllerClass, methodMeta, serviceFieldName);
                log.info("Added new method: {}", methodMeta.getMethodName());
            }
        }
    }

    /**
     * Enhances an existing method by replacing its body.
     */
    private void enhanceExistingMethod(
            MethodDeclaration method,
            MethodMetadata methodMeta,
            String serviceFieldName
    ) {
        // Only enhance if it has default implementation
        if (!hasDefaultImplementation(method)) {
            log.debug("Method {} already has custom implementation, skipping", method.getNameAsString());
            return;
        }

        // Add @Override annotation if not present
        if (!method.getAnnotationByName("Override").isPresent()) {
            method.addAnnotation("Override");
        }

        // Generate new method body
        BlockStmt newBody = generateMethodBody(methodMeta, serviceFieldName);
        method.setBody(newBody);

        log.debug("Replaced method body for: {}", method.getNameAsString());
    }

    /**
     * Checks if method has default OpenAPI implementation.
     */
    private boolean hasDefaultImplementation(MethodDeclaration method) {
        if (!method.getBody().isPresent()) {
            return true;
        }

        String bodyString = method.getBody().get().toString();

        return bodyString.contains("HttpStatus.NOT_IMPLEMENTED") ||
                bodyString.contains("ApiUtil.setExampleResponse") ||
                bodyString.contains("getRequest().ifPresent");
    }

    /**
     * Adds a new method to the controller class.
     */
    private void addNewMethod(
            ClassOrInterfaceDeclaration controllerClass,
            MethodMetadata methodMeta,
            String serviceFieldName
    ) {
        MethodDeclaration newMethod = new MethodDeclaration();
        newMethod.setName(methodMeta.getMethodName());
        newMethod.setModifiers(Modifier.Keyword.PUBLIC);
        newMethod.addAnnotation("Override");

        // Set return type
        newMethod.setType(methodMeta.getReturnType());

        // Add parameters
        for (ParameterMetadata paramMeta : methodMeta.getParameters()) {
            Parameter param = new Parameter();
            param.setName(paramMeta.getName());
            param.setType(paramMeta.getType());

            if (paramMeta.isRequestBody()) {
                param.addAnnotation("Valid");
                param.addAnnotation("RequestBody");
            }
            if (paramMeta.isPathVariable()) {
                param.addAnnotation("PathVariable");
            }
            if (paramMeta.isRequestParam()) {
                param.addAnnotation("RequestParam");
            }

            newMethod.addParameter(param);
        }

        // Set method body
        BlockStmt body = generateMethodBody(methodMeta, serviceFieldName);
        newMethod.setBody(body);

        controllerClass.addMember(newMethod);
    }

    /**
     * Generates the method body with service delegation and error handling.
     */
    private BlockStmt generateMethodBody(MethodMetadata methodMeta, String serviceFieldName) {
        BlockStmt body = new BlockStmt();

        // Add log.info statement
        MethodCallExpr logInfo = new MethodCallExpr(
                new NameExpr("log"),
                "info",
                new NodeList<>(new StringLiteralExpr("Processing " + methodMeta.getMethodName() + " request"))
        );
        body.addStatement(new ExpressionStmt(logInfo));

        // Create try-catch block
        BlockStmt tryBlock = new BlockStmt();
        BlockStmt catchBlock = new BlockStmt();

        // Build service method call expression
        MethodCallExpr serviceCall = buildServiceMethodCallExpression(methodMeta, serviceFieldName);

        // Add service call in try block
        if (methodMeta.getReturnType().contains("ResponseEntity")) {
            // Create variable declaration: var response = service.method(...)
            VariableDeclarator responseVar = new VariableDeclarator(
                    new ClassOrInterfaceType(null, "var"),
                    "response",
                    serviceCall
            );
            tryBlock.addStatement(new ExpressionStmt(
                    new VariableDeclarationExpr(responseVar)
            ));

            // Add success log
            MethodCallExpr successLog = new MethodCallExpr(
                    new NameExpr("log"),
                    "info",
                    new NodeList<>(new StringLiteralExpr(methodMeta.getMethodName() + " completed successfully"))
            );
            tryBlock.addStatement(new ExpressionStmt(successLog));

            // Return statement
            if (isCreateOperation(methodMeta.getMethodName())) {
                // return ResponseEntity.status(HttpStatus.CREATED).body(response.getBody());
                MethodCallExpr statusCall = new MethodCallExpr(
                        new NameExpr("ResponseEntity"),
                        "status",
                        new NodeList<>(new FieldAccessExpr(new NameExpr("HttpStatus"), "CREATED"))
                );
                MethodCallExpr bodyCall = new MethodCallExpr(
                        new NameExpr("response"),
                        "getBody"
                );
                MethodCallExpr returnExpr = new MethodCallExpr(
                        statusCall,
                        "body",
                        new NodeList<>(bodyCall)
                );
                tryBlock.addStatement(new com.github.javaparser.ast.stmt.ReturnStmt(returnExpr));
            } else {
                // return response;
                tryBlock.addStatement(new com.github.javaparser.ast.stmt.ReturnStmt(new NameExpr("response")));
            }
        } else {
            // return service.method(...);
            tryBlock.addStatement(new com.github.javaparser.ast.stmt.ReturnStmt(serviceCall));
        }

        // Build catch block
        // log.error("Failed to process X: {}", e.getMessage(), e);
        MethodCallExpr errorLog = new MethodCallExpr(
                new NameExpr("log"),
                "error",
                new NodeList<>(
                        new StringLiteralExpr("Failed to process " + methodMeta.getMethodName() + ": {}"),
                        new MethodCallExpr(new NameExpr("e"), "getMessage"),
                        new NameExpr("e")
                )
        );
        catchBlock.addStatement(new ExpressionStmt(errorLog));

        // throw e;
        catchBlock.addStatement(new com.github.javaparser.ast.stmt.ThrowStmt(new NameExpr("e")));

        // Create try-catch statement
        TryStmt tryStmt = new TryStmt();
        tryStmt.setTryBlock(tryBlock);

        CatchClause catchClause = new CatchClause();
        Parameter exceptionParam = new Parameter(
                new ClassOrInterfaceType(null, "Exception"),
                "e"
        );
        catchClause.setParameter(exceptionParam);
        catchClause.setBody(catchBlock);
        tryStmt.getCatchClauses().add(catchClause);

        body.addStatement(tryStmt);

        return body;
    }

    /**
     * Builds the service method call as a MethodCallExpr AST node.
     */
    private MethodCallExpr buildServiceMethodCallExpression(MethodMetadata methodMeta, String serviceFieldName) {
        // Build parameter list
        NodeList<Expression> arguments = new NodeList<>();
        for (ParameterMetadata param : methodMeta.getParameters()) {
            arguments.add(new NameExpr(param.getName()));
        }

        // Create method call: serviceFieldName.methodName(params...)
        return new MethodCallExpr(
                new NameExpr(serviceFieldName),
                methodMeta.getMethodName(),
                arguments
        );
    }

    /**
     * Checks if operation is a create operation.
     */
    private boolean isCreateOperation(String methodName) {
        String lowerName = methodName.toLowerCase();
        return lowerName.contains("create") ||
                lowerName.contains("add") ||
                lowerName.contains("register") ||
                lowerName.contains("save") ||
                lowerName.contains("post");
    }

    /**
     * Writes the enhanced controller back to file.
     */
    private void writeEnhancedController(CompilationUnit cu, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(cu.toString());
        }
        log.info("Written enhanced controller to: {}", outputFile.getAbsolutePath());
    }

    /**
     * Normalizes package/class name by removing incorrect prefixes.
     */
    private String normalizePackageName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        // Remove incorrect prefixes
        return name.replaceFirst("^(service|repository|model|controller|api)\\.", "");
    }

    /**
     * Calculates service package from base package.
     */
    private String calculateServicePackage(String basePackage) {
        String cleanPackage = basePackage.replaceAll("\\.api$", "");
        return cleanPackage + ".service";
    }

    /**
     * Generates service class name from API interface/controller name.
     * Handles both "Api" and "ApiController" suffixes correctly.
     * MUST match logic in ServiceClassGenerator.
     */
    private String generateServiceName(String className) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }

        // Remove "ApiController" suffix first (most specific)
        if (className.endsWith("ApiController")) {
            String baseName = className.substring(0, className.length() - "ApiController".length());
            return baseName + "Service";
        }

        // Remove "Api" suffix
        if (className.endsWith("Api")) {
            String baseName = className.substring(0, className.length() - "Api".length());
            return baseName + "Service";
        }

        // Remove "Controller" suffix
        if (className.endsWith("Controller")) {
            String baseName = className.substring(0, className.length() - "Controller".length());
            return baseName + "Service";
        }

        // No known suffix, just append Service
        return className + "Service";
    }

    /**
     * Converts string to camelCase.
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }
}