package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Service for handling file download operations and archive retrieval.
 * This service provides functionality to locate and retrieve generated project
 * archives (ZIP files) by generation ID, verify resource availability, and
 * manage file metadata like content length and filenames.
 *
 * <p>Primary responsibilities include:
 * <ul>
 *   <li>Finding and retrieving generated project archives by generation ID</li>
 *   <li>Validating generation directory existence</li>
 *   <li>Extracting file metadata (name, size) from resources</li>
 *   <li>Providing user-friendly error handling for missing or inaccessible files</li>
 * </ul>
 *
 * @author ProbeStack
 * @version 1.0
 * @since 1.0
 */
@Service
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    /**
     * Configuration for code generator including directory paths and default filenames
     */
    private final CodeGeneratorConfig config;

    /**
     * Constructs a new DownloadService with required configuration.
     *
     * @param config Configuration for code generator
     */
    @Autowired
    public DownloadService(CodeGeneratorConfig config) {
        this.config = config;
    }

    /**
     * Finds and returns the ZIP file resource for a generation ID.
     * Searches the generation directory for a ZIP archive and returns it as a Spring Resource.
     * Performs comprehensive validation including directory existence, file existence,
     * and readability checks.
     *
     * @param generationId The unique generation ID identifying the generated project
     * @return Optional containing the FileSystemResource if archive is found and readable,
     *         empty Optional if generation directory doesn't exist, no ZIP file found,
     *         or file is not readable
     * @throws CodeGenerationException if an I/O error occurs while accessing the file system
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
     * Gets the generation directory path for a given generation ID.
     * Constructs the full path by combining the output base directory with the generation ID.
     *
     * @param generationId The unique generation ID
     * @return Path object representing the generation directory location
     */
    private Path getGenerationDirectory(String generationId) {
        return Paths.get(config.getDirectories().getOutputBase(), generationId);
    }

    /**
     * Finds the ZIP file in the generation directory.
     * Searches for the first file with a .zip extension in the specified directory.
     *
     * @param generationDir The generation directory path to search
     * @return Path to the ZIP file if found, null if no ZIP file exists in the directory
     * @throws IOException if an I/O error occurs while listing directory contents
     */
    private Path findZipFile(Path generationDir) throws IOException {
        return Files.list(generationDir)
                .filter(path -> path.toString().endsWith(".zip"))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the filename from a Spring Resource.
     * Attempts to extract the filename from the underlying file. If extraction fails,
     * returns the default archive name from configuration.
     *
     * @param resource The Spring Resource object wrapping the file
     * @return The filename of the resource, or the default archive name if filename
     *         cannot be determined
     */
    public String getFilename(Resource resource) {
        try {
            return resource.getFile().getName();
        } catch (IOException e) {
            log.warn("Failed to get filename from resource, using default", e);
            return config.getFiles().getDefaultArchiveName();
        }
    }
    
    /**
     * Checks if a generation exists by verifying the generation directory.
     * This method is useful for validating generation IDs before attempting
     * to download or access generated projects.
     *
     * @param generationId The unique generation ID to check
     * @return true if the generation directory exists, false otherwise
     */
    public boolean generationExists(String generationId) {
        Path generationDir = getGenerationDirectory(generationId);
        boolean exists = Files.exists(generationDir);
        
        log.debug("Generation {} exists: {}", generationId, exists);
        return exists;
    }
    
    /**
     * Gets the content length of a Spring Resource in bytes.
     * This is useful for setting Content-Length headers in HTTP responses.
     *
     * @param resource The Spring Resource object to measure
     * @return The content length in bytes, or -1 if the length cannot be determined
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