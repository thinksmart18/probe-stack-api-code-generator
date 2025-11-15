package com.probe.stack.code.generator.service.enhancement;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for detecting and fixing compile-time errors in generated microservices.
 * Uses Maven compilation to identify errors and applies automated fixes.
 *
 * @author ProbeStack
 */
@Slf4j
@Service
public class CompileErrorDetectionService {

    // Pattern to match Maven compilation errors
    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "\\[ERROR\\]\\s+(.+?):\\[([0-9]+),([0-9]+)\\]\\s+(.+)"
    );

    /**
     * Detects and attempts to fix compile errors in the project.
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of messages about detected errors and fixes
     */
    public List<String> detectAndFixErrors(Path projectDir, CodeGenerationRequest request) {
        List<String> messages = new ArrayList<>();

        try {
            log.info("Running Maven compile to detect errors...");

            // Run Maven compile
            CompilationResult result = runMavenCompile(projectDir);

            if (result.isSuccess()) {
                messages.add("✓ No compile errors detected - project compiles successfully");
                log.info("Project compiled successfully with no errors");
                return messages;
            }

            log.warn("Compilation failed with {} errors", result.getErrors().size());
            messages.add(String.format("Found %d compile errors", result.getErrors().size()));

            // Analyze and fix errors
            for (CompileError error : result.getErrors()) {
                log.debug("Error: {} at {}:{}:{}", error.getMessage(),
                        error.getFilePath(), error.getLine(), error.getColumn());

                String fix = attemptAutoFix(error, projectDir, request);
                if (fix != null) {
                    messages.add(String.format("  Fixed: %s", fix));
                } else {
                    messages.add(String.format("  Manual fix required: %s", error.getMessage()));
                }
            }

            // Run compile again to verify fixes
            CompilationResult verifyResult = runMavenCompile(projectDir);
            if (verifyResult.isSuccess()) {
                messages.add("✓ All errors fixed - project now compiles successfully");
            } else {
                messages.add(String.format("⚠ %d errors remain after auto-fix",
                        verifyResult.getErrors().size()));
            }

        } catch (Exception e) {
            log.error("Error during compile error detection", e);
            messages.add("Compile error detection failed: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Runs Maven compile and parses the output for errors.
     *
     * @param projectDir Path to the project directory
     * @return Compilation result with any errors found
     */
    private CompilationResult runMavenCompile(Path projectDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(projectDir.toFile());

        // Use mvnw if available, otherwise use mvn
        Path mvnwPath = projectDir.resolve("mvnw");
        String mvnCommand = mvnwPath.toFile().exists() ? "./mvnw" : "mvn";

        pb.command(mvnCommand, "clean", "compile", "-DskipTests", "-q");

        Process process = pb.start();

        List<CompileError> errors = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        // Read output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");

                // Parse error lines
                Matcher matcher = ERROR_PATTERN.matcher(line);
                if (matcher.find()) {
                    CompileError error = new CompileError();
                    error.setFilePath(matcher.group(1));
                    error.setLine(Integer.parseInt(matcher.group(2)));
                    error.setColumn(Integer.parseInt(matcher.group(3)));
                    error.setMessage(matcher.group(4));
                    errors.add(error);
                }
            }
        }

        int exitCode = process.waitFor();

        CompilationResult result = new CompilationResult();
        result.setSuccess(exitCode == 0);
        result.setErrors(errors);
        result.setOutput(output.toString());

        return result;
    }

    /**
     * Attempts to automatically fix common compile errors.
     *
     * @param error The compile error to fix
     * @param projectDir Path to the project directory
     * @param request Original code generation request
     * @return Description of the fix applied, or null if no fix available
     */
    private String attemptAutoFix(CompileError error, Path projectDir, CodeGenerationRequest request) {
        String errorMsg = error.getMessage().toLowerCase();

        // Common error patterns and fixes
        if (errorMsg.contains("cannot find symbol")) {
            return fixMissingImport(error, projectDir);
        } else if (errorMsg.contains("package does not exist")) {
            return fixMissingPackage(error, projectDir);
        } else if (errorMsg.contains("incompatible types")) {
            return fixIncompatibleTypes(error, projectDir);
        } else if (errorMsg.contains("method does not override")) {
            return fixOverrideAnnotation(error, projectDir);
        }

        return null; // No automatic fix available
    }

    private String fixMissingImport(CompileError error, Path projectDir) {
        // Placeholder for missing import fix logic
        log.debug("Attempting to fix missing import: {}", error.getMessage());
        return null;
    }

    private String fixMissingPackage(CompileError error, Path projectDir) {
        // Placeholder for missing package fix logic
        log.debug("Attempting to fix missing package: {}", error.getMessage());
        return null;
    }

    private String fixIncompatibleTypes(CompileError error, Path projectDir) {
        // Placeholder for incompatible types fix logic
        log.debug("Attempting to fix incompatible types: {}", error.getMessage());
        return null;
    }

    private String fixOverrideAnnotation(CompileError error, Path projectDir) {
        // Placeholder for override annotation fix logic
        log.debug("Attempting to fix override annotation: {}", error.getMessage());
        return null;
    }

    /**
     * Data class representing a compilation result.
     */
    @Data
    public static class CompilationResult {
        private boolean success;
        private List<CompileError> errors = new ArrayList<>();
        private String output;
    }

    /**
     * Data class representing a compile error.
     */
    @Data
    public static class CompileError {
        private String filePath;
        private int line;
        private int column;
        private String message;
    }
}
