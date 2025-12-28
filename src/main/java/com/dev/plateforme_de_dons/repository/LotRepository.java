package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.Lot;
import com.dev.plateforme_de_dons.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(path = "lots", collectionResourceRel = "lots")
public interface LotRepository extends JpaRepository<Lot, Long> {

    Page<Lot> findByCreator(User creator, Pageable pageable);

    Page<Lot> findByCreatorAndActiveTrue(User creator, Pageable pageable);

    Page<Lot> findByActiveTrue(Pageable pageable);

    long countByCreatorAndActiveTrue(User creator);
}
