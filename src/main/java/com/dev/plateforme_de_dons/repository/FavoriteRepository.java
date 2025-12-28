package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Favorite;
import com.dev.plateforme_de_dons.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RepositoryRestResource(path = "favorites", collectionResourceRel = "favorites")
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Page<Favorite> findByUser(User user, Pageable pageable);

    Optional<Favorite> findByUserAndAnnonce(User user, Annonce annonce);

    boolean existsByUserAndAnnonce(User user, Annonce annonce);

    void deleteByUserAndAnnonce(User user, Annonce annonce);

    long countByUser(User user);

    long countByAnnonce(Annonce annonce);
}
