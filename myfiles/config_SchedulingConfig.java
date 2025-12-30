package com.dev.plateforme_de_dons.config;

import com.dev.plateforme_de_dons.service.SavedSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final SavedSearchService savedSearchService;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkNewAnnoncesForSavedSearches() {
        log.debug("Checking for new annonces matching saved searches...");
        try {
            savedSearchService.checkAndNotifyNewAnnonces();
        } catch (Exception e) {
            log.error("Error checking for new annonces: {}", e.getMessage(), e);
        }
    }
}
