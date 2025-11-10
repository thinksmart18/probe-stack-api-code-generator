package com.probe.stack.code.generator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
 * Service for file system operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileOperationsService {
    
    /**
     * Copies files from source directory to target directory, preserving structure
     *
     * @param sourceDir Source directory
     * @param targetDir Target directory
     * @return List of copied file paths
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
     * Creates a ZIP archive of the specified directory
     *
     * @param sourceDir Directory to archive
     * @param zipFilePath Path for the output ZIP file
     * @return Path to the created ZIP file
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
     * Creates a directory, creating parent directories if needed
     *
     * @param directory Path to create
     * @return Created directory path
     */
    public Path createDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.debug("Created directory: {}", directory);
        }
        return directory;
    }
    
    /**
     * Deletes a directory and all its contents
     *
     * @param directory Directory to delete
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
     * Deletes a single file
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
     * Cleans up old generated projects
     *
     * @param baseDir Base directory containing generated projects
     * @param hoursOld Delete projects older than this many hours
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