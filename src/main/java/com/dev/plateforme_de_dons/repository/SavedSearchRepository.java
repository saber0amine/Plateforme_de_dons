package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.SavedSearch;
import com.dev.plateforme_de_dons.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(path = "saved-searches", collectionResourceRel = "savedSearches")
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    Page<SavedSearch> findByUser(User user, Pageable pageable);

    List<SavedSearch> findByUserAndNotificationsEnabledTrue(User user);

    List<SavedSearch> findByNotificationsEnabledTrue();

    long countByUser(User user);
}
