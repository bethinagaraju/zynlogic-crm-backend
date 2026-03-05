package com.example.crm.scheduler;

import com.example.crm.service.InstantlyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LeadSyncScheduler {

    private final InstantlyService instantlyService;

    @Autowired
    public LeadSyncScheduler(InstantlyService instantlyService) {
        this.instantlyService = instantlyService;
    }

    // Run every hour by default. Adjust cron or fixedRate as needed.
    @Scheduled(fixedRateString = "PT1H")
    public void hourlySync() {
        try {
            log.info("Scheduled lead sync started");
            instantlyService.syncAllLeads();
            log.info("Scheduled lead sync finished");
        } catch (Exception e) {
            log.error("Scheduled lead sync failed", e);
        }
    }

}
