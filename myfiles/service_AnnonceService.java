package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.AnnonceDto;
import com.dev.plateforme_de_dons.dto.SearchCriteriaDto;
import com.dev.plateforme_de_dons.model.*;
import com.dev.plateforme_de_dons.repository.AnnonceRepository;
import com.dev.plateforme_de_dons.repository.FavoriteRepository;
import com.dev.plateforme_de_dons.repository.KeywordRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnonceService {

    private final AnnonceRepository annonceRepository;
    private final KeywordRepository keywordRepository;
    private final FavoriteRepository favoriteRepository;
    private final ImageService imageService;

    public Annonce createAnnonce(AnnonceDto dto, User owner) {
        Annonce annonce = new Annonce();
        annonce.setTitre(dto.getTitre());
        annonce.setDescription(dto.getDescription());
        annonce.setEtatObjet(dto.getEtatObjet());
        annonce.setZoneGeographique(dto.getZoneGeographique());
        annonce.setModeLivraison(dto.getModeLivraison());
        annonce.setOwner(owner);
        annonce.setActive(true);

        processKeywords(annonce, dto.getKeywordsInput());

        return annonceRepository.save(annonce);
    }

    public Annonce updateAnnonce(Long id, AnnonceDto dto, User owner) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        if (!annonce.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier cette annonce");
        }

        annonce.setTitre(dto.getTitre());
        annonce.setDescription(dto.getDescription());
        annonce.setEtatObjet(dto.getEtatObjet());
        annonce.setZoneGeographique(dto.getZoneGeographique());
        annonce.setModeLivraison(dto.getModeLivraison());

        annonce.getKeywords().clear();
        processKeywords(annonce, dto.getKeywordsInput());

        return annonceRepository.save(annonce);
    }

    private void processKeywords(Annonce annonce, String keywordsInput) {
        if (keywordsInput != null && !keywordsInput.isBlank()) {
            Arrays.stream(keywordsInput.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(k -> !k.isEmpty())
                    .distinct()
                    .forEach(keywordName -> {
                        Keyword keyword = keywordRepository.findByNameIgnoreCase(keywordName)
                                .orElseGet(() -> {
                                    Keyword newKeyword = new Keyword(keywordName);
                                    return keywordRepository.save(newKeyword);
                                });
                        annonce.addKeyword(keyword);
                    });
        }
    }

    @Transactional(readOnly = true)
    public Optional<Annonce> findById(Long id) {
        return annonceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Annonce> findAllActive(Pageable pageable) {
        return annonceRepository.findByActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Annonce> findByOwner(User owner, Pageable pageable) {
        return annonceRepository.findByOwner(owner, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Annonce> search(SearchCriteriaDto criteria, Pageable pageable) {
        Specification<Annonce> spec = buildSpecification(criteria);
        return annonceRepository.findAll(spec, pageable);
    }

    private Specification<Annonce> buildSpecification(SearchCriteriaDto criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
                String pattern = "%" + criteria.getQuery().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("titre")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            if (criteria.getZoneGeographique() != null && !criteria.getZoneGeographique().isBlank()) {
                String pattern = "%" + criteria.getZoneGeographique().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("zoneGeographique")), pattern));
            }

            if (criteria.getEtatObjet() != null) {
                predicates.add(cb.equal(root.get("etatObjet"), criteria.getEtatObjet()));
            }

            if (criteria.getModeLivraison() != null) {
                predicates.add(cb.equal(root.get("modeLivraison"), criteria.getModeLivraison()));
            }

            if (criteria.getKeywords() != null && !criteria.getKeywords().isEmpty()) {
                Join<Annonce, Keyword> keywordJoin = root.join("keywords", JoinType.INNER);
                predicates.add(keywordJoin.get("name").in(
                        criteria.getKeywords().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList())
                ));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public void deactivateAnnonce(Long id, User owner) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        if (!annonce.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer cette annonce");
        }

        annonce.setActive(false);
        annonceRepository.save(annonce);
    }

    public void markAsReserved(Long id, User owner) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        if (!annonce.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier cette annonce");
        }

        annonce.setReserved(true);
        annonceRepository.save(annonce);
    }

    public void markAsGiven(Long id, User owner) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        if (!annonce.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier cette annonce");
        }

        annonce.setGiven(true);
        annonce.setActive(false);
        annonceRepository.save(annonce);
    }

    @Transactional(readOnly = true)
    public List<Annonce> findNewMatchingAnnonces(SavedSearch savedSearch, LocalDateTime since) {
        List<String> keywords = null;
        if (savedSearch.getKeywords() != null && !savedSearch.getKeywords().isBlank()) {
            keywords = Arrays.stream(savedSearch.getKeywords().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }

        SearchCriteriaDto criteria = new SearchCriteriaDto();
        criteria.setQuery(savedSearch.getQuery());
        criteria.setZoneGeographique(savedSearch.getZoneGeographique());
        criteria.setEtatObjet(savedSearch.getEtatObjet());
        criteria.setModeLivraison(savedSearch.getModeLivraison());
        criteria.setKeywords(keywords);

        Specification<Annonce> spec = buildSpecification(criteria);
        spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("datePublication"), since));

        return annonceRepository.findAll(spec);
    }

    public AnnonceDto convertToDto(Annonce annonce) {
        AnnonceDto dto = new AnnonceDto();
        dto.setId(annonce.getId());
        dto.setTitre(annonce.getTitre());
        dto.setDescription(annonce.getDescription());
        dto.setEtatObjet(annonce.getEtatObjet());
        dto.setZoneGeographique(annonce.getZoneGeographique());
        dto.setModeLivraison(annonce.getModeLivraison());
        dto.setDatePublication(annonce.getDatePublication());
        dto.setOwnerUsername(annonce.getOwner().getUsername());
        dto.setOwnerId(annonce.getOwner().getId());
        dto.setActive(annonce.isActive());
        dto.setReserved(annonce.isReserved());
        dto.setGiven(annonce.isGiven());
        dto.setKeywords(annonce.getKeywords().stream()
                .map(Keyword::getName)
                .collect(Collectors.toSet()));
        dto.setKeywordsInput(String.join(", ", dto.getKeywords()));
        if (annonce.getLot() != null) {
            dto.setLotId(annonce.getLot().getId());
        }
        dto.setFavoriteCount((int) favoriteRepository.countByAnnonce(annonce));

         dto.setImages(imageService.convertToDtoList(annonce.getImages()));

         Image primaryImage = annonce.getPrimaryImage();
        if (primaryImage != null) {
            dto.setPrimaryImage(imageService.convertToDto(primaryImage));
            dto.setImageUrl("/api/images/" + primaryImage.getId());
        } else {
            dto.setImageUrl(null);
        }

        return dto;
    }
}