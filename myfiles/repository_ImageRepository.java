package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Image;
import com.dev.plateforme_de_dons.model.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByAnnonce(Annonce annonce);

    List<Image> findByLot(Lot lot);

    Optional<Image> findByAnnonceAndIsPrimaryTrue(Annonce annonce);

    Optional<Image> findByLotAndIsPrimaryTrue(Lot lot);

    Optional<Image> findByFilename(String filename);

    void deleteByAnnonce(Annonce annonce);

    void deleteByLot(Lot lot);
}