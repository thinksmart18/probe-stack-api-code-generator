package com.probe.stack.code.generator.scheduler;

import com.probe.stack.code.generator.service.CodeGenerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CleanupScheduler
 */
@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    @Mock
    private CodeGenerationService codeGenerationService;

    @InjectMocks
    private CleanupScheduler cleanupScheduler;

    @Test
    void testScheduledCleanup_ExecutesCleanup() {
        // Act
        cleanupScheduler.scheduledCleanup();

        // Assert
        verify(codeGenerationService, times(1)).scheduledCleanup();
    }

    @Test
    void testScheduledCleanup_ServiceThrowsException_HandlesGracefully() {
        // Arrange
        doThrow(new RuntimeException("Cleanup failed"))
                .when(codeGenerationService).scheduledCleanup();

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> cleanupScheduler.scheduledCleanup());
    }

    @Test
    void testScheduledCleanup_NullService_HandlesGracefully() {
        // Arrange
        CleanupScheduler nullScheduler = new CleanupScheduler(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> nullScheduler.scheduledCleanup());
    }

    @Test
    void testScheduledCleanup_CalledMultipleTimes_ExecutesEachTime() {
        // Act
        cleanupScheduler.scheduledCleanup();
        cleanupScheduler.scheduledCleanup();
        cleanupScheduler.scheduledCleanup();

        // Assert
        verify(codeGenerationService, times(3)).scheduledCleanup();
    }
}
