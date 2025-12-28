package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.AnnonceDto;
import com.dev.plateforme_de_dons.dto.SearchCriteriaDto;
import com.dev.plateforme_de_dons.model.*;
import com.dev.plateforme_de_dons.repository.AnnonceRepository;
import com.dev.plateforme_de_dons.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnonceServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @InjectMocks
    private AnnonceService annonceService;

    private User owner;
    private AnnonceDto validDto;

    @BeforeEach
    void setUp() {
        owner = new User("testuser", "test@example.com", "password");
        owner.setId(1L);

        validDto = new AnnonceDto();
        validDto.setTitre("Test Annonce");
        validDto.setDescription("Description de test pour l'annonce");
        validDto.setEtatObjet(EtatObjet.BON_ETAT);
        validDto.setZoneGeographique("Paris");
        validDto.setModeLivraison(ModeLivraison.MAIN_PROPRE);
        validDto.setKeywordsInput("meubles, salon");
    }

    @Test
    void createAnnonce_ShouldCreateSuccessfully() {
        when(keywordRepository.findByNameIgnoreCase("meubles")).thenReturn(Optional.empty());
        when(keywordRepository.findByNameIgnoreCase("salon")).thenReturn(Optional.empty());
        when(keywordRepository.save(any(Keyword.class))).thenAnswer(i -> i.getArgument(0));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(i -> {
            Annonce a = i.getArgument(0);
            a.setId(1L);
            return a;
        });

        Annonce result = annonceService.createAnnonce(validDto, owner);

        assertNotNull(result);
        assertEquals("Test Annonce", result.getTitre());
        assertEquals(EtatObjet.BON_ETAT, result.getEtatObjet());
        assertEquals(owner, result.getOwner());
        assertTrue(result.isActive());

        verify(annonceRepository).save(any(Annonce.class));
    }

    @Test
    void findAllActive_ShouldReturnActiveAnnonces() {
        Annonce annonce = new Annonce();
        annonce.setId(1L);
        annonce.setTitre("Test");
        annonce.setActive(true);
        annonce.setOwner(owner);

        Page<Annonce> page = new PageImpl<>(List.of(annonce));
        Pageable pageable = PageRequest.of(0, 10);

        when(annonceRepository.findByActiveTrue(pageable)).thenReturn(page);

        Page<Annonce> result = annonceService.findAllActive(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateAnnonce_ShouldUpdate_WhenOwnerMatches() {
        Annonce existingAnnonce = new Annonce();
        existingAnnonce.setId(1L);
        existingAnnonce.setOwner(owner);
        existingAnnonce.setKeywords(new java.util.HashSet<>());

        when(annonceRepository.findById(1L)).thenReturn(Optional.of(existingAnnonce));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(i -> i.getArgument(0));

        validDto.setTitre("Updated Title");
        validDto.setKeywordsInput(null);

        Annonce result = annonceService.updateAnnonce(1L, validDto, owner);

        assertEquals("Updated Title", result.getTitre());
        verify(annonceRepository).save(any(Annonce.class));
    }

    @Test
    void updateAnnonce_ShouldThrow_WhenNotOwner() {
        User otherUser = new User("other", "other@test.com", "pass");
        otherUser.setId(2L);

        Annonce existingAnnonce = new Annonce();
        existingAnnonce.setId(1L);
        existingAnnonce.setOwner(otherUser);

        when(annonceRepository.findById(1L)).thenReturn(Optional.of(existingAnnonce));

        assertThrows(IllegalArgumentException.class,
                () -> annonceService.updateAnnonce(1L, validDto, owner));

        verify(annonceRepository, never()).save(any(Annonce.class));
    }

    @Test
    void deactivateAnnonce_ShouldDeactivate_WhenOwnerMatches() {
        Annonce existingAnnonce = new Annonce();
        existingAnnonce.setId(1L);
        existingAnnonce.setOwner(owner);
        existingAnnonce.setActive(true);

        when(annonceRepository.findById(1L)).thenReturn(Optional.of(existingAnnonce));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(i -> i.getArgument(0));

        annonceService.deactivateAnnonce(1L, owner);

        assertFalse(existingAnnonce.isActive());
        verify(annonceRepository).save(existingAnnonce);
    }

    @Test
    void markAsGiven_ShouldMarkAsGivenAndDeactivate() {
        Annonce existingAnnonce = new Annonce();
        existingAnnonce.setId(1L);
        existingAnnonce.setOwner(owner);
        existingAnnonce.setActive(true);
        existingAnnonce.setGiven(false);

        when(annonceRepository.findById(1L)).thenReturn(Optional.of(existingAnnonce));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(i -> i.getArgument(0));

        annonceService.markAsGiven(1L, owner);

        assertTrue(existingAnnonce.isGiven());
        assertFalse(existingAnnonce.isActive());
    }

    @Test
    void convertToDto_ShouldConvertCorrectly() {
        Annonce annonce = new Annonce();
        annonce.setId(1L);
        annonce.setTitre("Test");
        annonce.setDescription("Description");
        annonce.setEtatObjet(EtatObjet.NEUF);
        annonce.setZoneGeographique("Lyon");
        annonce.setModeLivraison(ModeLivraison.ENVOI);
        annonce.setOwner(owner);
        annonce.setActive(true);

        AnnonceDto dto = annonceService.convertToDto(annonce);

        assertEquals(1L, dto.getId());
        assertEquals("Test", dto.getTitre());
        assertEquals("testuser", dto.getOwnerUsername());
        assertEquals(EtatObjet.NEUF, dto.getEtatObjet());
    }
}
