package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.LotDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Lot;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.repository.AnnonceRepository;
import com.dev.plateforme_de_dons.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LotService {

    private final LotRepository lotRepository;
    private final AnnonceRepository annonceRepository;

    public Lot createLot(LotDto dto, User creator) {
        Lot lot = new Lot();
        lot.setTitre(dto.getTitre());
        lot.setDescription(dto.getDescription());
        lot.setCreator(creator);
        lot.setActive(true);

        lot = lotRepository.save(lot);

        if (dto.getAnnonceIds() != null && !dto.getAnnonceIds().isEmpty()) {
            for (Long annonceId : dto.getAnnonceIds()) {
                addAnnonceToLot(lot.getId(), annonceId, creator);
            }
        }

        return lot;
    }

    public Lot updateLot(Long id, LotDto dto, User owner) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lot non trouvé"));

        if (!lot.getCreator().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce lot");
        }

        lot.setTitre(dto.getTitre());
        lot.setDescription(dto.getDescription());

        return lotRepository.save(lot);
    }

    public void addAnnonceToLot(Long lotId, Long annonceId, User owner) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot non trouvé"));

        if (!lot.getCreator().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce lot");
        }

        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        if (!annonce.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez ajouter que vos propres annonces à un lot");
        }

        if (annonce.getLot() != null) {
            throw new IllegalArgumentException("Cette annonce fait déjà partie d'un lot");
        }

        lot.addAnnonce(annonce);
        lotRepository.save(lot);
    }

    public void removeAnnonceFromLot(Long lotId, Long annonceId, User owner) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot non trouvé"));

        if (!lot.getCreator().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce lot");
        }

        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        lot.removeAnnonce(annonce);
        lotRepository.save(lot);
    }

    @Transactional(readOnly = true)
    public Optional<Lot> findById(Long id) {
        return lotRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Lot> findByCreator(User creator, Pageable pageable) {
        return lotRepository.findByCreator(creator, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Lot> findAllActive(Pageable pageable) {
        return lotRepository.findByActiveTrue(pageable);
    }

    public void deactivateLot(Long id, User owner) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lot non trouvé"));

        if (!lot.getCreator().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce lot");
        }

        for (Annonce annonce : lot.getAnnonces()) {
            annonce.setLot(null);
        }
        lot.getAnnonces().clear();
        lot.setActive(false);
        lotRepository.save(lot);
    }

    @Transactional(readOnly = true)
    public List<Annonce> getAvailableAnnoncesForLot(User owner) {
        return annonceRepository.findByOwnerAndActiveTrue(owner, Pageable.unpaged())
                .stream()
                .filter(a -> a.getLot() == null)
                .toList();
    }
}
