package com.probe.stack.code.generator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for scanning and locating Spring Boot controller classes
 * within a generated project structure.
 *
 * @author ProbeStack
 */
@Slf4j
@Component
public class ControllerPathScanner {

    private static final String SRC_MAIN_JAVA = "src/main/java";
    private static final String CONTROLLER_SUFFIX = "Controller.java";


    public List<File> getGeneratedControllerClassFiles(String projectDirectory,
                                                       String basePackage,
                                                       String apiPackageName) throws IOException {
        Path apiPath = constructApiPackagePath(projectDirectory, basePackage, apiPackageName);
        return findControllerClasses(apiPath);
    }
    /**
     * Constructs the absolute path to the API package directory.
     *
     * @param projectDirectory the project root directory
     * @param basePackage the base package (dot-separated)
     * @param apiPackageName the API package name
     * @return Path object representing the API package directory
     */
    public Path constructApiPackagePath(
            String projectDirectory,
            String basePackage,
            String apiPackageName
    ) {
        // Convert package notation to directory structure
        // e.g., "com.probestack.onboarding" -> "com/probestack/onboarding"
        String packagePath = basePackage.replace('.', '/');

        // Construct full path:
        // projectDirectory/src/main/java/com/probestack/onboarding/api
        Path path = Paths.get(projectDirectory)
                .resolve(SRC_MAIN_JAVA)
                .resolve(packagePath)
                .resolve(apiPackageName);

        log.debug("Constructed API package path: {}", path.toAbsolutePath());
        return path;
    }

    /**
     * Alternative method: Constructs API package directory using Path internally,
     * then converts to File.
     *
     * @param projectDirectory the project root directory
     * @param basePackage the base package (dot-separated)
     * @return String path representing the API package directory
     */
    public File constructOutputPackageDirector(
            String projectDirectory,
            String basePackage
    ) {
        //String packagePath = basePackage.replace('.', '/');

        Path fullPath = Paths.get(projectDirectory)
                .resolve(SRC_MAIN_JAVA);
               // .resolve(packagePath);
        File apiPackageDir = fullPath.toFile();

        log.debug("Constructed API package directory from Path: {}", apiPackageDir.getAbsolutePath());

        return apiPackageDir;
    }



    /**
     * Validates that the directory exists and is readable.
     *
     * @param directoryPath the directory path to validate
     * @throws IllegalArgumentException if directory is invalid
     */
    private void validateDirectory(Path directoryPath) {
        if (!Files.exists(directoryPath)) {
            throw new IllegalArgumentException(
                    String.format("API package directory does not exist: %s", directoryPath.toAbsolutePath())
            );
        }

        if (!Files.isDirectory(directoryPath)) {
            throw new IllegalArgumentException(
                    String.format("Path is not a directory: %s", directoryPath.toAbsolutePath())
            );
        }

        if (!Files.isReadable(directoryPath)) {
            throw new IllegalArgumentException(
                    String.format("Directory is not readable: %s", directoryPath.toAbsolutePath())
            );
        }

        log.debug("Directory validation passed: {}", directoryPath);
    }

    /**
     * Finds all Java files ending with "Controller" suffix in the given directory.
     *
     * @param apiPackagePath the API package directory path
     * @return List of File objects for controller classes
     * @throws IOException if directory scanning fails
     */
    private List<File> findControllerClasses(Path apiPackagePath) throws IOException {
        try (Stream<Path> paths = Files.walk(apiPackagePath, 1)) {
            List<File> controllerFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(CONTROLLER_SUFFIX))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            // Log individual controller files found
            if (log.isDebugEnabled()) {
                controllerFiles.forEach(file ->
                        log.debug("Found controller: {}", file.getName())
                );
            }

            return controllerFiles;
        }
    }

    public Optional<File> findExistingController(
            String projectDirectory,
            String basePackage,
            String apiPackageName,
            String apiInterfaceName
    ) {
        // Generate expected controller name
        String controllerName = null;
        if(StringUtils.isNotEmpty(apiInterfaceName) && apiInterfaceName.endsWith("Controller")){
            controllerName = apiInterfaceName + ".java";
        } else {
            controllerName = apiInterfaceName + "Controller.java";
        }

        // Construct path
        String packagePath = basePackage.replace('.', '/');
        Path controllerPath = Paths.get(projectDirectory)
                .resolve(SRC_MAIN_JAVA)
                .resolve(packagePath)
                .resolve(apiPackageName)
                .resolve(controllerName);

        File controllerFile = controllerPath.toFile();

        if (controllerFile.exists() && controllerFile.isFile()) {
            log.info("Found existing controller: {}", controllerFile.getAbsolutePath());
            return Optional.of(controllerFile);
        }

        log.debug("No existing controller found at: {}", controllerPath);
        return Optional.empty();
    }

    /**
     * Checks if a controller already exists for an API interface.
     */
    public boolean controllerExists(
            String projectDirectory,
            String basePackage,
            String apiPackageName,
            String apiInterfaceName
    ) {
        return findExistingController(projectDirectory, basePackage, apiPackageName, apiInterfaceName)
                .isPresent();
    }


}