package com.secondhand.coreservice.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.secondhand.coreservice.service.impl.SearchHistoryServiceImpl;
import com.secondhand.coreservice.service.impl.ViewHistoryServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HistoryCleanupScheduler {

    private final SearchHistoryServiceImpl searchHistoryService;
    private final ViewHistoryServiceImpl viewHistoryService;

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldHistories() {
        log.info("Starting history cleanup scheduler...");

        try {
            searchHistoryService.cleanupOldSearchHistory();

            viewHistoryService.cleanupOldViewHistory();

            log.info("History cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during history cleanup", e);
        }
    }
}
