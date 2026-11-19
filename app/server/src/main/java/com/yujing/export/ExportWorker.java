package com.yujing.export;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExportWorker {

    private final ExportService exportService;

    public ExportWorker(ExportService exportService) {
        this.exportService = exportService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 5000)
    public void processPending() {
        exportService.processPending(3);
    }

    @Scheduled(cron = "0 15 * * * *")
    public void expireOld() {
        exportService.expireOld(20);
    }
}
