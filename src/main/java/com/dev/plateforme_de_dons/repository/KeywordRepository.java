package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(path = "keywords", collectionResourceRel = "keywords")
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByNameIgnoreCase(String name);

    List<Keyword> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT k FROM Keyword k WHERE SIZE(k.annonces) > 0 ORDER BY SIZE(k.annonces) DESC")
    List<Keyword> findPopularKeywords();
}
