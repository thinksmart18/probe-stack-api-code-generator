package com.probe.stack.code.generator.scheduler;

import com.probe.stack.code.generator.service.CodeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic cleanup of old generated projects
 * The cron expression is configured via probe.stack.generator.cleanup.cron
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final CodeGenerationService codeGenerationService;

    /**
     * Runs cleanup based on configured cron expression
     * Default: every hour at minute 0 (0 0 * * * *)
     */
    @Scheduled(cron = "${probe.stack.generator.cleanup.cron:0 0 * * * *}")
    public void cleanupOldProjects() {
        log.info("Starting scheduled cleanup of old projects");
        codeGenerationService.scheduledCleanup();
        log.info("Scheduled cleanup completed");
    }
}