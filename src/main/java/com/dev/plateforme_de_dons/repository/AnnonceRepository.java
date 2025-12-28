package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
import com.dev.plateforme_de_dons.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RepositoryRestResource(path = "annonces", collectionResourceRel = "annonces")
public interface AnnonceRepository extends JpaRepository<Annonce, Long>, JpaSpecificationExecutor<Annonce> {

    Page<Annonce> findByActiveTrue(Pageable pageable);

    Page<Annonce> findByOwner(User owner, Pageable pageable);

    Page<Annonce> findByOwnerAndActiveTrue(User owner, Pageable pageable);

    Page<Annonce> findByZoneGeographiqueContainingIgnoreCaseAndActiveTrue(String zone, Pageable pageable);

    Page<Annonce> findByEtatObjetAndActiveTrue(EtatObjet etatObjet, Pageable pageable);

    Page<Annonce> findByModeLivraisonAndActiveTrue(ModeLivraison modeLivraison, Pageable pageable);

    @Query("SELECT a FROM Annonce a WHERE a.active = true AND " +
           "(LOWER(a.titre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Annonce> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Annonce a JOIN a.keywords k WHERE a.active = true AND k.name IN :keywords")
    Page<Annonce> findByKeywords(@Param("keywords") List<String> keywords, Pageable pageable);

    @Query("SELECT a FROM Annonce a WHERE a.active = true AND a.datePublication > :since")
    List<Annonce> findNewAnnoncesSince(@Param("since") LocalDateTime since);

    @Query("SELECT DISTINCT a FROM Annonce a JOIN a.keywords k WHERE a.active = true " +
           "AND a.datePublication > :since " +
           "AND (:zone IS NULL OR LOWER(a.zoneGeographique) LIKE LOWER(CONCAT('%', :zone, '%'))) " +
           "AND (:etat IS NULL OR a.etatObjet = :etat) " +
           "AND (:mode IS NULL OR a.modeLivraison = :mode) " +
           "AND (COALESCE(:keywords, NULL) IS NULL OR k.name IN :keywords)")
    List<Annonce> findMatchingAnnonces(
        @Param("since") LocalDateTime since,
        @Param("zone") String zone,
        @Param("etat") EtatObjet etat,
        @Param("mode") ModeLivraison mode,
        @Param("keywords") List<String> keywords
    );

    long countByOwnerAndActiveTrue(User owner);
}
