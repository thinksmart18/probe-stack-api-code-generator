package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Service for handling file download operations
 */
@Service
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);
    private static final String GENERATED_PROJECTS_DIR = "./generated-projects";
    
    /**
     * Finds and returns the ZIP file resource for a generation ID
     *
     * @param generationId The generation ID
     * @return Optional containing the Resource if found, empty otherwise
     */
    public Optional<Resource> getGeneratedProjectArchive(String generationId) {
        log.info("Attempting to retrieve archive for generation ID: {}", generationId);
        
        try {
            Path generationDir = getGenerationDirectory(generationId);
            
            if (!Files.exists(generationDir)) {
                log.warn("Generation directory not found: {}", generationId);
                return Optional.empty();
            }
            
            Path zipFile = findZipFile(generationDir);
            
            if (zipFile == null) {
                log.warn("ZIP file not found for generation ID: {}", generationId);
                return Optional.empty();
            }
            
            Resource resource = new FileSystemResource(zipFile);
            
            if (!resource.exists() || !resource.isReadable()) {
                log.error("Resource exists but is not readable: {}", zipFile);
                return Optional.empty();
            }
            
            log.info("Successfully located archive: {}", zipFile.getFileName());
            return Optional.of(resource);
            
        } catch (IOException e) {
            log.error("Error while retrieving archive for generation ID: {}", generationId, e);
            throw new CodeGenerationException("Failed to retrieve generated project archive", e);
        }
    }
    
    /**
     * Gets the generation directory path
     *
     * @param generationId The generation ID
     * @return Path to the generation directory
     */
    private Path getGenerationDirectory(String generationId) {
        return Paths.get(GENERATED_PROJECTS_DIR, generationId);
    }
    
    /**
     * Finds the ZIP file in the generation directory
     *
     * @param generationDir The generation directory path
     * @return Path to the ZIP file, or null if not found
     * @throws IOException if an I/O error occurs
     */
    private Path findZipFile(Path generationDir) throws IOException {
        return Files.list(generationDir)
                .filter(path -> path.toString().endsWith(".zip"))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the filename from a resource
     *
     * @param resource The resource
     * @return The filename
     */
    public String getFilename(Resource resource) {
        try {
            return resource.getFile().getName();
        } catch (IOException e) {
            log.warn("Failed to get filename from resource, using default", e);
            return "generated-project.zip";
        }
    }
    
    /**
     * Checks if a generation exists
     *
     * @param generationId The generation ID
     * @return true if the generation directory exists
     */
    public boolean generationExists(String generationId) {
        Path generationDir = getGenerationDirectory(generationId);
        boolean exists = Files.exists(generationDir);
        
        log.debug("Generation {} exists: {}", generationId, exists);
        return exists;
    }
    
    /**
     * Gets the content length of a resource
     *
     * @param resource The resource
     * @return The content length in bytes
     */
    public long getContentLength(Resource resource) {
        try {
            return resource.contentLength();
        } catch (IOException e) {
            log.warn("Failed to get content length from resource", e);
            return -1;
        }
    }
}