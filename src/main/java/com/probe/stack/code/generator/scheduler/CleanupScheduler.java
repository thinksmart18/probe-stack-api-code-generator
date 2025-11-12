package com.probe.stack.code.generator.scheduler;

import com.probe.stack.code.generator.service.CodeGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic cleanup of old generated projects
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
     * Runs cleanup every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldProjects() {
        log.info("Starting scheduled cleanup of old projects");
        codeGenerationService.scheduledCleanup();
        log.info("Scheduled cleanup completed");
    }
}