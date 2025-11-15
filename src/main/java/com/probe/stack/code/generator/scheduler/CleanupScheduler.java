package com.probe.stack.code.generator.scheduler;

import com.probe.stack.code.generator.service.CodeGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic cleanup of old generated projects
 * The cron expression is configured via probe.stack.generator.cleanup.cron
 */
@Component
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final CodeGenerationService codeGenerationService;

    @Autowired
    public CleanupScheduler(CodeGenerationService codeGenerationService) {
        this.codeGenerationService = codeGenerationService;
    }

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