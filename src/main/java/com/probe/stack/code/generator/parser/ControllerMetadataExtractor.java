package com.probe.stack.code.generator.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Extracts metadata from Spring Boot controller classes for code generation.
 *
 * @author ProbeStack
 */
@Slf4j
@Component
public class ControllerMetadataExtractor {

    private final JavaParser javaParser = new JavaParser();

    public ControllerMetadata extractMetadata(File controllerFile) throws Exception {
        try (FileInputStream in = new FileInputStream(controllerFile)) {
            CompilationUnit cu = javaParser.parse(in).getResult()
                    .orElseThrow(() -> new IllegalArgumentException("Failed to parse controller file"));

            ControllerMetadata metadata = new ControllerMetadata();

            // Extract package name
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            metadata.setPackageName(packageName);

            log.info("=".repeat(60));
            log.info("Extracting metadata from: {}", controllerFile.getName());
            log.info("Extracted package name: {}", packageName);
            log.info("Expected format: com.company.project.api");

            // Validate package format
            if (!packageName.contains(".")) {
                log.warn("Package name appears invalid: {}", packageName);
            }

            // Extract imports
            Map<String, String> importMap = extractImports(cu);
            metadata.setImportMap(importMap);

            // Find the primary class/interface
            ClassOrInterfaceDeclaration classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new IllegalArgumentException("No class or interface found"));

            metadata.setClassName(classDecl.getNameAsString());
            metadata.setInterface(classDecl.isInterface());

            log.info("Class/Interface: {}", metadata.getClassName());
            log.info("Is Interface: {}", metadata.isInterface());

            // Extract methods from the class or its interfaces
            List<MethodMetadata> methods = new ArrayList<>();

            if (classDecl.isInterface()) {
                methods = extractMethodsFromDeclaration(classDecl);
            } else {
                methods = extractMethodsFromClass(classDecl, controllerFile.getParentFile());
            }

            metadata.setMethods(methods);

            // Extract entity/model class from method signatures
            extractEntityClass(methods, metadata, cu);

            log.info("Entity Class: {}", metadata.getEntityClass());
            log.info("Methods Found: {}", methods.size());
            log.info("=".repeat(60));

            return metadata;
        }
    }

    private Map<String, String> extractImports(CompilationUnit cu) {
        Map<String, String> importMap = new HashMap<>();

        for (ImportDeclaration importDecl : cu.getImports()) {
            String fullName = importDecl.getNameAsString();
            String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
            importMap.put(simpleName, fullName);
        }

        return importMap;
    }

    private List<MethodMetadata> extractMethodsFromClass(
            ClassOrInterfaceDeclaration classDecl,
            File packageDir
    ) throws IOException {

        List<MethodMetadata> methods = new ArrayList<>();

        // First, extract methods directly from the class
        for (MethodDeclaration method : classDecl.getMethods()) {
            if (isApiMethod(method)) {
                methods.add(extractMethodMetadata(method));
            }
        }

        // If no methods found or few methods, check implemented interfaces
        if (methods.size() <= 1 && !classDecl.getImplementedTypes().isEmpty()) {
            log.debug("Checking implemented interfaces for more methods...");

            for (ClassOrInterfaceType implementedType : classDecl.getImplementedTypes()) {
                String interfaceName = implementedType.getNameAsString();
                File interfaceFile = new File(packageDir, interfaceName + ".java");

                if (interfaceFile.exists()) {
                    log.debug("Found interface file: {}", interfaceFile.getName());
                    methods.addAll(extractMethodsFromInterface(interfaceFile));
                }
            }
        }

        log.debug("Extracted {} methods from class/interfaces", methods.size());
        return methods;
    }

    private List<MethodMetadata> extractMethodsFromInterface(File interfaceFile) throws IOException {
        try (FileInputStream in = new FileInputStream(interfaceFile)) {
            CompilationUnit cu = javaParser.parse(in).getResult()
                    .orElseThrow(() -> new IllegalArgumentException("Failed to parse interface file"));

            ClassOrInterfaceDeclaration interfaceDecl = cu.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new IllegalArgumentException("No interface found"));

            return extractMethodsFromDeclaration(interfaceDecl);
        }
    }

    private List<MethodMetadata> extractMethodsFromDeclaration(ClassOrInterfaceDeclaration declaration) {
        List<MethodMetadata> methods = new ArrayList<>();

        for (MethodDeclaration method : declaration.getMethods()) {
            if (isApiMethod(method)) {
                MethodMetadata methodMeta = extractMethodMetadata(method);
                methods.add(methodMeta);
                log.debug("Extracted method: {}", methodMeta.getMethodName());
            }
        }

        return methods;
    }

    /**
     * FIXED: More lenient method detection logic.
     */
    private boolean isApiMethod(MethodDeclaration method) {
        String methodName = method.getNameAsString();

        // Skip utility methods
        if (methodName.equals("getRequest") ||
                methodName.equals("hashCode") ||
                methodName.equals("equals") ||
                methodName.equals("toString")) {
            log.debug("Skipping utility method: {}", methodName);
            return false;
        }

        // Include if has Spring mapping annotations
        boolean hasSpringAnnotation = method.getAnnotations().stream()
                .anyMatch(ann -> {
                    String annName = ann.getNameAsString();
                    return annName.endsWith("Mapping") ||
                            annName.equals("RequestMapping") ||
                            annName.equals("GetMapping") ||
                            annName.equals("PostMapping") ||
                            annName.equals("PutMapping") ||
                            annName.equals("DeleteMapping") ||
                            annName.equals("PatchMapping");
                });

        // Include if has OpenAPI/Swagger annotations
        boolean hasOperationAnnotation = method.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("Operation"));

        // Include if it's a default method in an interface (likely API method)
        boolean isDefaultMethod = method.isDefault();

        // Include if it's a public abstract method in an interface (API contract)
        boolean isPublicAbstract = method.isPublic() && method.isAbstract();

        boolean include = hasSpringAnnotation || hasOperationAnnotation || isDefaultMethod || isPublicAbstract;

        if (!include) {
            log.debug("Excluding method: {} (no API indicators found)", methodName);
        }

        return include;
    }

    private MethodMetadata extractMethodMetadata(MethodDeclaration method) {
        MethodMetadata methodMeta = new MethodMetadata();
        methodMeta.setMethodName(method.getNameAsString());
        methodMeta.setReturnType(method.getType().asString());

        // Extract parameters
        List<ParameterMetadata> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            ParameterMetadata paramMeta = new ParameterMetadata();
            paramMeta.setName(param.getNameAsString());
            paramMeta.setType(param.getType().asString());

            // Check for validation and request annotations
            for (AnnotationExpr annotation : param.getAnnotations()) {
                String annName = annotation.getNameAsString();
                if (annName.equals("Valid") || annName.equals("RequestBody")) {
                    paramMeta.setRequestBody(true);
                }
                if (annName.equals("PathVariable")) {
                    paramMeta.setPathVariable(true);
                }
                if (annName.equals("RequestParam")) {
                    paramMeta.setRequestParam(true);
                }
            }

            params.add(paramMeta);
        }

        methodMeta.setParameters(params);

        log.debug("Method: {} returns {} with {} parameters",
                methodMeta.getMethodName(),
                methodMeta.getReturnType(),
                params.size());

        return methodMeta;
    }

    private void extractEntityClass(
            List<MethodMetadata> methods,
            ControllerMetadata metadata,
            CompilationUnit cu
    ) {
        Set<String> candidateEntities = new HashSet<>();

        // Strategy 1: Look for @RequestBody parameters
        for (MethodMetadata method : methods) {
            for (ParameterMetadata param : method.getParameters()) {
                if (param.isRequestBody() && !isJavaLangType(param.getType())) {
                    String entityClass = extractSimpleClassName(param.getType());
                    if (!entityClass.equals("MultipartFile") &&
                            !entityClass.equals("String") &&
                            !entityClass.startsWith("List") &&
                            !entityClass.startsWith("Map")) {
                        candidateEntities.add(entityClass);
                        log.debug("Found candidate entity from @RequestBody: {}", entityClass);
                    }
                }
            }
        }

        // Strategy 2: Look at ResponseEntity generic types
        if (candidateEntities.isEmpty()) {
            for (MethodMetadata method : methods) {
                String returnType = method.getReturnType();
                if (returnType.contains("ResponseEntity<") && returnType.contains(">")) {
                    String genericType = extractGenericType(returnType);
                    if (!isJavaLangType(genericType)) {
                        String entityClass = extractSimpleClassName(genericType);
                        candidateEntities.add(entityClass);
                        log.debug("Found candidate entity from ResponseEntity: {}", entityClass);
                    }
                }
            }
        }

        // Strategy 3: Look at imports to find model classes
        if (candidateEntities.isEmpty()) {
            cu.getImports().forEach(importDecl -> {
                String importPath = importDecl.getNameAsString();
                if (importPath.contains(".model.") || importPath.contains(".entity.")) {
                    String className = importPath.substring(importPath.lastIndexOf('.') + 1);
                    candidateEntities.add(className);
                    log.debug("Found candidate entity from imports: {}", className);
                }
            });
        }

        // Strategy 4: Derive from controller name
        if (candidateEntities.isEmpty()) {
            String controllerName = metadata.getClassName();
            String derivedEntity = deriveEntityFromControllerName(controllerName);
            if (derivedEntity != null) {
                candidateEntities.add(derivedEntity);
                log.debug("Derived entity from controller name: {}", derivedEntity);
            }
        }

        // Prioritize Request types for the entity
        Optional<String> requestEntity = candidateEntities.stream()
                .filter(e -> e.contains("Request"))
                .findFirst();

        if (requestEntity.isPresent()) {
            metadata.setEntityClass(requestEntity.get());
        } else if (!candidateEntities.isEmpty()) {
            metadata.setEntityClass(candidateEntities.iterator().next());
        } else {
            log.warn("No entity class found for controller: {}", metadata.getClassName());
            String defaultEntity = deriveEntityFromControllerName(metadata.getClassName());
            metadata.setEntityClass(defaultEntity != null ? defaultEntity : "Entity");
        }

        log.info("Selected entity class: {}", metadata.getEntityClass());
    }

    private String deriveEntityFromControllerName(String controllerName) {
        if (controllerName == null || controllerName.isEmpty()) {
            return null;
        }

        String entityName = controllerName;

        if (entityName.endsWith("Controller")) {
            entityName = entityName.substring(0, entityName.length() - "Controller".length());
        } else if (entityName.endsWith("ApiController")) {
            entityName = entityName.substring(0, entityName.length() - "ApiController".length());
        } else if (entityName.endsWith("Api")) {
            entityName = entityName.substring(0, entityName.length() - "Api".length());
        }

        return entityName.isEmpty() ? null : entityName;
    }

    private String extractSimpleClassName(String type) {
        if (type == null || type.isEmpty()) {
            return type;
        }

        if (type.contains("<")) {
            type = type.substring(0, type.indexOf('<'));
        }

        type = type.replace("[]", "");

        if (type.contains(".")) {
            type = type.substring(type.lastIndexOf('.') + 1);
        }

        return type.trim();
    }

    private String extractGenericType(String type) {
        if (type.contains("<") && type.contains(">")) {
            int start = type.indexOf('<') + 1;
            int end = type.lastIndexOf('>');
            return type.substring(start, end).trim();
        }
        return type;
    }

    private boolean isJavaLangType(String type) {
        if (type == null) return true;

        String simpleType = extractSimpleClassName(type);

        return simpleType.equals("String") ||
                simpleType.equals("Integer") ||
                simpleType.equals("Long") ||
                simpleType.equals("Double") ||
                simpleType.equals("Boolean") ||
                simpleType.equals("Void") ||
                simpleType.equals("Object") ||
                simpleType.startsWith("java.");
    }

    @Data
    public static class ControllerMetadata {
        private String packageName;
        private String className;
        private boolean isInterface;
        private String entityClass;
        private List<MethodMetadata> methods;
        private Map<String, String> importMap;
    }

    @Data
    public static class MethodMetadata {
        private String methodName;
        private String returnType;
        private List<ParameterMetadata> parameters;
    }

    @Data
    public static class ParameterMetadata {
        private String name;
        private String type;
        private boolean isRequestBody;
        private boolean isPathVariable;
        private boolean isRequestParam;
    }
}