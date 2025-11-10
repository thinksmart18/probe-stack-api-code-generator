package com.probe.stack.code.generator.scheduler;

import com.probe.stack.code.generator.service.CodeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic cleanup of old generated projects
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {
    
    private final CodeGenerationService codeGenerationService;
    
    /**
     * Runs cleanup every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldProjects() {
        log.info("Starting scheduled cleanup of old projects");
        codeGenerationService.scheduledCleanup();
        log.info("Scheduled cleanup completed");
    }
}