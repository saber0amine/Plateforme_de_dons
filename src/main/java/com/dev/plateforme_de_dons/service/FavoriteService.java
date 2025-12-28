package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Favorite;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.repository.AnnonceRepository;
import com.dev.plateforme_de_dons.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AnnonceRepository annonceRepository;

    public Favorite addToFavorites(User user, Long annonceId) {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        if (favoriteRepository.existsByUserAndAnnonce(user, annonce)) {
            throw new IllegalArgumentException("Cette annonce est déjà dans vos favoris");
        }

        Favorite favorite = new Favorite(user, annonce);
        return favoriteRepository.save(favorite);
    }

    public void removeFromFavorites(User user, Long annonceId) {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));

        favoriteRepository.deleteByUserAndAnnonce(user, annonce);
    }

    @Transactional(readOnly = true)
    public Page<Favorite> getFavorites(User user, Pageable pageable) {
        return favoriteRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(User user, Annonce annonce) {
        return favoriteRepository.existsByUserAndAnnonce(user, annonce);
    }

    @Transactional(readOnly = true)
    public long countFavorites(User user) {
        return favoriteRepository.countByUser(user);
    }
}
