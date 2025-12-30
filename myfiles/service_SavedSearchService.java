package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.SavedSearchDto;
import com.dev.plateforme_de_dons.dto.SearchCriteriaDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.SavedSearch;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.repository.SavedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final AnnonceService annonceService;
    private final NotificationService notificationService;

    public SavedSearch saveSearch(SavedSearchDto dto, User user) {
        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setName(dto.getName());
        savedSearch.setUser(user);
        savedSearch.setQuery(dto.getQuery());
        savedSearch.setZoneGeographique(dto.getZoneGeographique());
        savedSearch.setEtatObjet(dto.getEtatObjet());
        savedSearch.setModeLivraison(dto.getModeLivraison());
        savedSearch.setKeywords(dto.getKeywords());
        savedSearch.setNotificationsEnabled(dto.isNotificationsEnabled());
        savedSearch.setLastNotificationAt(LocalDateTime.now());

        return savedSearchRepository.save(savedSearch);
    }

    public SavedSearch saveSearchFromCriteria(String name, SearchCriteriaDto criteria, User user, boolean notificationsEnabled) {
        SavedSearchDto dto = new SavedSearchDto();
        dto.setName(name);
        dto.setQuery(criteria.getQuery());
        dto.setZoneGeographique(criteria.getZoneGeographique());
        dto.setEtatObjet(criteria.getEtatObjet());
        dto.setModeLivraison(criteria.getModeLivraison());
        if (criteria.getKeywords() != null && !criteria.getKeywords().isEmpty()) {
            dto.setKeywords(String.join(",", criteria.getKeywords()));
        }
        dto.setNotificationsEnabled(notificationsEnabled);

        return saveSearch(dto, user);
    }

    public SavedSearch updateSavedSearch(Long id, SavedSearchDto dto, User user) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recherche sauvegardée non trouvée"));

        if (!savedSearch.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier cette recherche");
        }

        savedSearch.setName(dto.getName());
        savedSearch.setQuery(dto.getQuery());
        savedSearch.setZoneGeographique(dto.getZoneGeographique());
        savedSearch.setEtatObjet(dto.getEtatObjet());
        savedSearch.setModeLivraison(dto.getModeLivraison());
        savedSearch.setKeywords(dto.getKeywords());
        savedSearch.setNotificationsEnabled(dto.isNotificationsEnabled());

        return savedSearchRepository.save(savedSearch);
    }

    public void toggleNotifications(Long id, User user) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recherche sauvegardée non trouvée"));

        if (!savedSearch.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier cette recherche");
        }

        savedSearch.setNotificationsEnabled(!savedSearch.isNotificationsEnabled());
        savedSearchRepository.save(savedSearch);
    }

    @Transactional(readOnly = true)
    public Optional<SavedSearch> findById(Long id) {
        return savedSearchRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<SavedSearch> findByUser(User user, Pageable pageable) {
        return savedSearchRepository.findByUser(user, pageable);
    }

    public void deleteSavedSearch(Long id, User user) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recherche sauvegardée non trouvée"));

        if (!savedSearch.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer cette recherche");
        }

        savedSearchRepository.delete(savedSearch);
    }

    public void checkAndNotifyNewAnnonces() {
        List<SavedSearch> savedSearches = savedSearchRepository.findByNotificationsEnabledTrue();

        for (SavedSearch savedSearch : savedSearches) {
            LocalDateTime since = savedSearch.getLastNotificationAt() != null
                    ? savedSearch.getLastNotificationAt()
                    : savedSearch.getCreatedAt();

            List<Annonce> newAnnonces = annonceService.findNewMatchingAnnonces(savedSearch, since);

            if (!newAnnonces.isEmpty()) {
                for (Annonce annonce : newAnnonces) {
                    notificationService.createNewAnnonceNotification(
                            savedSearch.getUser(),
                            annonce,
                            savedSearch
                    );
                }
                savedSearch.setLastNotificationAt(LocalDateTime.now());
                savedSearchRepository.save(savedSearch);
            }
        }
    }

    public SearchCriteriaDto convertToSearchCriteria(SavedSearch savedSearch) {
        SearchCriteriaDto criteria = new SearchCriteriaDto();
        criteria.setQuery(savedSearch.getQuery());
        criteria.setZoneGeographique(savedSearch.getZoneGeographique());
        criteria.setEtatObjet(savedSearch.getEtatObjet());
        criteria.setModeLivraison(savedSearch.getModeLivraison());
        if (savedSearch.getKeywords() != null && !savedSearch.getKeywords().isBlank()) {
            criteria.setKeywords(
                    Arrays.stream(savedSearch.getKeywords().split(","))
                            .map(String::trim)
                            .collect(Collectors.toList())
            );
        }
        return criteria;
    }

    public SavedSearchDto convertToDto(SavedSearch savedSearch) {
        SavedSearchDto dto = new SavedSearchDto();
        dto.setId(savedSearch.getId());
        dto.setName(savedSearch.getName());
        dto.setQuery(savedSearch.getQuery());
        dto.setZoneGeographique(savedSearch.getZoneGeographique());
        dto.setEtatObjet(savedSearch.getEtatObjet());
        dto.setModeLivraison(savedSearch.getModeLivraison());
        dto.setKeywords(savedSearch.getKeywords());
        dto.setNotificationsEnabled(savedSearch.isNotificationsEnabled());
        dto.setCreatedAt(savedSearch.getCreatedAt());
        dto.setLastNotificationAt(savedSearch.getLastNotificationAt());
        return dto;
    }
}
