package com.probe.stack.code.generator.service;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for file system operations including copying, archiving, and cleanup.
 * Provides high-level file management functionality for the code generation process
 * including directory copying, ZIP archive creation, directory/file deletion,
 * and scheduled cleanup of old projects.
 *
 * <p>Key features:
 * <ul>
 *   <li>Recursive directory copying with structure preservation</li>
 *   <li>ZIP archive creation from directory contents</li>
 *   <li>Safe directory and file deletion</li>
 *   <li>Time-based cleanup of old generated projects</li>
 * </ul>
 *
 * @author ProbeStack
 * @version 1.0
 * @since 1.0
 */
@Service
public class FileOperationsService {

    private static final Logger log = LoggerFactory.getLogger(FileOperationsService.class);

    /**
     * Copies files from source directory to target directory, preserving structure.
     * Recursively walks through the source directory and copies all files while
     * maintaining the directory hierarchy in the target location.
     *
     * @param sourceDir Source directory to copy from
     * @param targetDir Target directory to copy to
     * @return List of absolute paths of all copied files
     * @throws IOException if directory traversal or file copying fails
     */
    public List<String> copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        log.info("Copying directory from {} to {}", sourceDir, targetDir);
        
        List<String> copiedFiles = new ArrayList<>();
        
        if (!Files.exists(sourceDir)) {
            log.warn("Source directory does not exist: {}", sourceDir);
            return copiedFiles;
        }
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceDir.relativize(file);
                Path targetPath = targetDir.resolve(relativePath);
                
                Files.createDirectories(targetPath.getParent());
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                copiedFiles.add(targetPath.toString());
                log.debug("Copied file: {}", relativePath);
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        log.info("Copied {} files from source directory", copiedFiles.size());
        return copiedFiles;
    }
    
    /**
     * Creates a ZIP archive of the specified directory.
     * Recursively walks through the source directory and packages all files
     * into a ZIP archive, preserving the directory structure.
     *
     * @param sourceDir Directory to archive
     * @param zipFilePath Path for the output ZIP file (including filename)
     * @return Path to the created ZIP file
     * @throws IOException if archive creation or file access fails
     */
    public Path createArchive(Path sourceDir, Path zipFilePath) throws IOException {
        log.info("Creating archive: {}", zipFilePath);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = sourceDir.relativize(file);
                    ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace("\\", "/"));
                    zos.putNextEntry(zipEntry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        log.info("Archive created successfully: {}", zipFilePath);
        return zipFilePath;
    }
    
    /**
     * Creates a directory, creating parent directories if needed.
     * Uses createDirectories to ensure all parent directories exist.
     * If the directory already exists, this method does nothing.
     *
     * @param directory Path to create
     * @return The created (or existing) directory path
     * @throws IOException if directory creation fails
     */
    public Path createDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.debug("Created directory: {}", directory);
        }
        return directory;
    }
    
    /**
     * Deletes a directory and all its contents recursively.
     * Uses Apache Commons IO for safe recursive deletion.
     * Logs errors but does not throw exceptions, allowing the application to continue.
     *
     * @param directory Directory to delete (along with all subdirectories and files)
     */
    public void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                FileUtils.deleteDirectory(directory.toFile());
                log.info("Deleted directory: {}", directory);
            }
        } catch (IOException e) {
            log.error("Failed to delete directory: {}", directory, e);
        }
    }

    /**
     * Deletes a single file.
     * Only deletes if the path exists and is a regular file (not a directory).
     * Logs errors but does not throw exceptions.
     *
     * @param file File to delete
     */
    public void deleteFile(Path file) {
        try {
            if (Files.exists(file) && Files.isRegularFile(file)) {
                Files.delete(file);
                log.debug("Deleted file: {}", file);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", file, e);
        }
    }

    /**
     * Cleans up old generated projects based on last modified time.
     * Scans the base directory for projects and deletes those older than
     * the specified number of hours. This method is typically called by
     * a scheduled task to prevent disk space issues.
     *
     * @param baseDir Base directory containing generated project subdirectories
     * @param hoursOld Delete projects with last modified time older than this many hours.
     *                 If 0 or negative, no cleanup is performed
     */
    public void cleanupOldProjects(Path baseDir, int hoursOld) {
        if (hoursOld <= 0 || !Files.exists(baseDir)) {
            return;
        }
        
        log.info("Cleaning up projects older than {} hours in {}", hoursOld, baseDir);
        
        try {
            long cutoffTime = System.currentTimeMillis() - (hoursOld * 3600000L);
            
            Files.list(baseDir).forEach(path -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    if (attrs.lastModifiedTime().toMillis() < cutoffTime) {
                        deleteDirectory(path);
                        log.info("Cleaned up old project: {}", path.getFileName());
                    }
                } catch (IOException e) {
                    log.error("Failed to check/delete old project: {}", path, e);
                }
            });
            
        } catch (IOException e) {
            log.error("Failed to list projects for cleanup", e);
        }
    }
}