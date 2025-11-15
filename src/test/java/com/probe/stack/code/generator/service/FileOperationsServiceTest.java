package com.probe.stack.code.generator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for FileOperationsService
 * Tests file operations, directory management, and cleanup functionality
 */
@ExtendWith(MockitoExtension.class)
class FileOperationsServiceTest {

    @InjectMocks
    private FileOperationsService fileOperationsService;

    @TempDir
    Path tempDir;

    private Path sourceDir;
    private Path targetDir;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = tempDir.resolve("source");
        targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir);
    }

    // ==================== Copy Directory Tests ====================

    @Test
    void testCopyDirectory_Success_CopiesAllFiles() throws IOException {
        // Arrange
        Path file1 = sourceDir.resolve("file1.txt");
        Path file2 = sourceDir.resolve("file2.txt");
        Path subDir = sourceDir.resolve("subdir");
        Path file3 = subDir.resolve("file3.txt");

        Files.createDirectories(subDir);
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");
        Files.writeString(file3, "content3");

        // Act
        List<String> copiedFiles = fileOperationsService.copyDirectory(sourceDir, targetDir);

        // Assert
        assertEquals(3, copiedFiles.size());
        assertTrue(Files.exists(targetDir.resolve("file1.txt")));
        assertTrue(Files.exists(targetDir.resolve("file2.txt")));
        assertTrue(Files.exists(targetDir.resolve("subdir/file3.txt")));
        assertEquals("content1", Files.readString(targetDir.resolve("file1.txt")));
        assertEquals("content2", Files.readString(targetDir.resolve("file2.txt")));
        assertEquals("content3", Files.readString(targetDir.resolve("subdir/file3.txt")));
    }

    @Test
    void testCopyDirectory_EmptySource_ReturnsEmptyList() throws IOException {
        // Act
        List<String> copiedFiles = fileOperationsService.copyDirectory(sourceDir, targetDir);

        // Assert
        assertTrue(copiedFiles.isEmpty());
        assertTrue(Files.exists(sourceDir));
    }

    @Test
    void testCopyDirectory_NonExistentSource_ReturnsEmptyList() throws IOException {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent");

        // Act
        List<String> copiedFiles = fileOperationsService.copyDirectory(nonExistent, targetDir);

        // Assert
        assertTrue(copiedFiles.isEmpty());
    }

    @Test
    void testCopyDirectory_OverwritesExistingFiles() throws IOException {
        // Arrange
        Path sourceFile = sourceDir.resolve("file.txt");
        Path targetFile = targetDir.resolve("file.txt");

        Files.writeString(sourceFile, "new content");
        Files.createDirectories(targetDir);
        Files.writeString(targetFile, "old content");

        // Act
        List<String> copiedFiles = fileOperationsService.copyDirectory(sourceDir, targetDir);

        // Assert
        assertEquals(1, copiedFiles.size());
        assertEquals("new content", Files.readString(targetFile));
    }

    @Test
    void testCopyDirectory_PreservesDirectoryStructure() throws IOException {
        // Arrange
        Path deepDir = sourceDir.resolve("level1/level2/level3");
        Files.createDirectories(deepDir);
        Path deepFile = deepDir.resolve("deep.txt");
        Files.writeString(deepFile, "deep content");

        // Act
        fileOperationsService.copyDirectory(sourceDir, targetDir);

        // Assert
        Path copiedDeepFile = targetDir.resolve("level1/level2/level3/deep.txt");
        assertTrue(Files.exists(copiedDeepFile));
        assertEquals("deep content", Files.readString(copiedDeepFile));
    }

    @Test
    void testCopyDirectory_HandlesSpecialCharacters() throws IOException {
        // Arrange
        Path specialFile = sourceDir.resolve("file with spaces & special@chars.txt");
        Files.writeString(specialFile, "special content");

        // Act
        List<String> copiedFiles = fileOperationsService.copyDirectory(sourceDir, targetDir);

        // Assert
        assertEquals(1, copiedFiles.size());
        assertTrue(Files.exists(targetDir.resolve("file with spaces & special@chars.txt")));
    }

    // ==================== Create Archive Tests ====================

    @Test
    void testCreateArchive_Success_CreatesZipFile() throws IOException {
        // Arrange
        Path file1 = sourceDir.resolve("file1.txt");
        Path file2 = sourceDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        Path zipPath = tempDir.resolve("archive.zip");

        // Act
        Path result = fileOperationsService.createArchive(sourceDir, zipPath);

        // Assert
        assertEquals(zipPath, result);
        assertTrue(Files.exists(zipPath));
        assertTrue(Files.size(zipPath) > 0);

        // Verify ZIP contents
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry1 = zis.getNextEntry();
            assertNotNull(entry1);
            ZipEntry entry2 = zis.getNextEntry();
            assertNotNull(entry2);
        }
    }

    @Test
    void testCreateArchive_WithSubdirectories_IncludesAllFiles() throws IOException {
        // Arrange
        Path subDir = sourceDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path file1 = sourceDir.resolve("root.txt");
        Path file2 = subDir.resolve("nested.txt");
        Files.writeString(file1, "root content");
        Files.writeString(file2, "nested content");

        Path zipPath = tempDir.resolve("archive.zip");

        // Act
        fileOperationsService.createArchive(sourceDir, zipPath);

        // Assert
        assertTrue(Files.exists(zipPath));

        // Verify ZIP structure
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                String name = entry.getName();
                assertTrue(name.equals("root.txt") || name.equals("subdir/nested.txt"));
            }
            assertEquals(2, entryCount);
        }
    }

    @Test
    void testCreateArchive_EmptyDirectory_CreatesEmptyZip() throws IOException {
        // Arrange
        Path zipPath = tempDir.resolve("empty.zip");

        // Act
        Path result = fileOperationsService.createArchive(sourceDir, zipPath);

        // Assert
        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) > 0); // ZIP header exists
    }

    @Test
    void testCreateArchive_LargeFile_Success() throws IOException {
        // Arrange
        Path largeFile = sourceDir.resolve("large.txt");
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        Files.write(largeFile, largeContent);

        Path zipPath = tempDir.resolve("large.zip");

        // Act
        Path result = fileOperationsService.createArchive(sourceDir, zipPath);

        // Assert
        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) < Files.size(largeFile)); // Should be compressed
    }

    // ==================== Create Directory Tests ====================

    @Test
    void testCreateDirectory_Success_CreatesDirectory() throws IOException {
        // Arrange
        Path newDir = tempDir.resolve("newdir");

        // Act
        Path result = fileOperationsService.createDirectory(newDir);

        // Assert
        assertEquals(newDir, result);
        assertTrue(Files.exists(newDir));
        assertTrue(Files.isDirectory(newDir));
    }

    @Test
    void testCreateDirectory_ExistingDirectory_DoesNotThrow() throws IOException {
        // Arrange
        Path existingDir = tempDir.resolve("existing");
        Files.createDirectories(existingDir);

        // Act & Assert
        assertDoesNotThrow(() -> fileOperationsService.createDirectory(existingDir));
        assertTrue(Files.exists(existingDir));
    }

    @Test
    void testCreateDirectory_CreatesParentDirectories() throws IOException {
        // Arrange
        Path deepDir = tempDir.resolve("parent/child/grandchild");

        // Act
        fileOperationsService.createDirectory(deepDir);

        // Assert
        assertTrue(Files.exists(deepDir));
        assertTrue(Files.exists(deepDir.getParent()));
        assertTrue(Files.exists(deepDir.getParent().getParent()));
    }

    // ==================== Delete Directory Tests ====================

    @Test
    void testDeleteDirectory_Success_DeletesDirectory() throws IOException {
        // Arrange
        Path dirToDelete = tempDir.resolve("todelete");
        Files.createDirectories(dirToDelete);
        Path file = dirToDelete.resolve("file.txt");
        Files.writeString(file, "content");

        // Act
        fileOperationsService.deleteDirectory(dirToDelete);

        // Assert
        assertFalse(Files.exists(dirToDelete));
    }

    @Test
    void testDeleteDirectory_WithNestedFiles_DeletesAll() throws IOException {
        // Arrange
        Path dirToDelete = tempDir.resolve("todelete");
        Path subDir = dirToDelete.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(dirToDelete.resolve("file1.txt"), "content1");
        Files.writeString(subDir.resolve("file2.txt"), "content2");

        // Act
        fileOperationsService.deleteDirectory(dirToDelete);

        // Assert
        assertFalse(Files.exists(dirToDelete));
        assertFalse(Files.exists(subDir));
    }

    @Test
    void testDeleteDirectory_NonExistentDirectory_DoesNotThrow() {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent");

        // Act & Assert
        assertDoesNotThrow(() -> fileOperationsService.deleteDirectory(nonExistent));
    }

    // ==================== Delete File Tests ====================

    @Test
    void testDeleteFile_Success_DeletesFile() throws IOException {
        // Arrange
        Path fileToDelete = tempDir.resolve("file.txt");
        Files.writeString(fileToDelete, "content");

        // Act
        fileOperationsService.deleteFile(fileToDelete);

        // Assert
        assertFalse(Files.exists(fileToDelete));
    }

    @Test
    void testDeleteFile_NonExistentFile_DoesNotThrow() {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        // Act & Assert
        assertDoesNotThrow(() -> fileOperationsService.deleteFile(nonExistent));
    }

    @Test
    void testDeleteFile_Directory_DoesNotDelete() throws IOException {
        // Arrange
        Path directory = tempDir.resolve("directory");
        Files.createDirectories(directory);

        // Act
        fileOperationsService.deleteFile(directory);

        // Assert
        assertTrue(Files.exists(directory)); // Should not delete directories
    }

    // ==================== Cleanup Old Projects Tests ====================

    @Test
    void testCleanupOldProjects_DeletesOldProjects() throws IOException, InterruptedException {
        // Arrange
        Path projectsBase = tempDir.resolve("projects");
        Files.createDirectories(projectsBase);

        Path oldProject = projectsBase.resolve("old");
        Path newProject = projectsBase.resolve("new");
        Files.createDirectories(oldProject);
        Files.createDirectories(newProject);

        // Set old project's last modified time to 2 hours ago
        Files.setLastModifiedTime(oldProject, FileTime.fromMillis(
                System.currentTimeMillis() - (2 * 3600000L + 1000L)));

        // Act
        fileOperationsService.cleanupOldProjects(projectsBase, 1);

        // Give some time for cleanup
        Thread.sleep(100);

        // Assert
        assertFalse(Files.exists(oldProject));
        assertTrue(Files.exists(newProject));
    }

    @Test
    void testCleanupOldProjects_ZeroHours_DoesNothing() throws IOException {
        // Arrange
        Path projectsBase = tempDir.resolve("projects");
        Files.createDirectories(projectsBase);
        Path project = projectsBase.resolve("project");
        Files.createDirectories(project);

        // Act
        fileOperationsService.cleanupOldProjects(projectsBase, 0);

        // Assert
        assertTrue(Files.exists(project));
    }

    @Test
    void testCleanupOldProjects_NegativeHours_DoesNothing() throws IOException {
        // Arrange
        Path projectsBase = tempDir.resolve("projects");
        Files.createDirectories(projectsBase);
        Path project = projectsBase.resolve("project");
        Files.createDirectories(project);

        // Act
        fileOperationsService.cleanupOldProjects(projectsBase, -1);

        // Assert
        assertTrue(Files.exists(project));
    }

    @Test
    void testCleanupOldProjects_NonExistentBase_DoesNotThrow() {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent");

        // Act & Assert
        assertDoesNotThrow(() -> fileOperationsService.cleanupOldProjects(nonExistent, 1));
    }

    @Test
    void testCleanupOldProjects_EmptyBase_DoesNotThrow() throws IOException {
        // Arrange
        Path emptyBase = tempDir.resolve("empty");
        Files.createDirectories(emptyBase);

        // Act & Assert
        assertDoesNotThrow(() -> fileOperationsService.cleanupOldProjects(emptyBase, 1));
    }

    @Test
    void testCleanupOldProjects_PreservesRecentProjects() throws IOException {
        // Arrange
        Path projectsBase = tempDir.resolve("projects");
        Files.createDirectories(projectsBase);

        Path recentProject = projectsBase.resolve("recent");
        Files.createDirectories(recentProject);

        // Act
        fileOperationsService.cleanupOldProjects(projectsBase, 24);

        // Assert
        assertTrue(Files.exists(recentProject)); // Should still exist
    }

    // ==================== Edge Cases ====================

    @Test
    void testCopyDirectory_HiddenFiles_CopiesSuccessfully() throws IOException {
        // Arrange
        Path hiddenFile = sourceDir.resolve(".hidden");
        Files.writeString(hiddenFile, "hidden content");

        // Act
        List<String> copiedFiles = fileOperationsService.copyDirectory(sourceDir, targetDir);

        // Assert
        assertEquals(1, copiedFiles.size());
        assertTrue(Files.exists(targetDir.resolve(".hidden")));
    }

    @Test
    void testCreateArchive_WindowsPathSeparators_HandledCorrectly() throws IOException {
        // Arrange
        Path subDir = sourceDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path file = subDir.resolve("file.txt");
        Files.writeString(file, "content");

        Path zipPath = tempDir.resolve("archive.zip");

        // Act
        fileOperationsService.createArchive(sourceDir, zipPath);

        // Assert - ZIP should use forward slashes
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                assertFalse(entry.getName().contains("\\"),
                        "ZIP entry should use forward slashes: " + entry.getName());
                entry = zis.getNextEntry();
            }
        }
    }
}
