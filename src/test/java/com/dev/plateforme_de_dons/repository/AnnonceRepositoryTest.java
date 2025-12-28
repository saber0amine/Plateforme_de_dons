package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AnnonceRepositoryTest {

    @Autowired
    private AnnonceRepository annonceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password123");
        testUser = userRepository.save(testUser);
    }

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveAnnonces() {
        Annonce active = createAnnonce("Active", true);
        Annonce inactive = createAnnonce("Inactive", false);

        Page<Annonce> result = annonceRepository.findByActiveTrue(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Active", result.getContent().get(0).getTitre());
    }

    @Test
    void findByOwner_ShouldReturnUserAnnonces() {
        User otherUser = userRepository.save(new User("other", "other@test.com", "password123"));

        createAnnonce("My Annonce", true);
        Annonce otherAnnonce = new Annonce();
        otherAnnonce.setTitre("Other Annonce");
        otherAnnonce.setDescription("Description test");
        otherAnnonce.setEtatObjet(EtatObjet.NEUF);
        otherAnnonce.setZoneGeographique("Lyon");
        otherAnnonce.setModeLivraison(ModeLivraison.ENVOI);
        otherAnnonce.setOwner(otherUser);
        otherAnnonce.setActive(true);
        annonceRepository.save(otherAnnonce);

        Page<Annonce> result = annonceRepository.findByOwner(testUser, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("My Annonce", result.getContent().get(0).getTitre());
    }

    @Test
    void findByZoneGeographiqueContainingIgnoreCaseAndActiveTrue_ShouldFindByPartialZone() {
        createAnnonceWithZone("Paris Annonce", "Paris 15e");
        createAnnonceWithZone("Lyon Annonce", "Lyon");

        Page<Annonce> result = annonceRepository
                .findByZoneGeographiqueContainingIgnoreCaseAndActiveTrue("paris", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Paris Annonce", result.getContent().get(0).getTitre());
    }

    @Test
    void findByEtatObjetAndActiveTrue_ShouldFilterByEtat() {
        createAnnonceWithEtat("Neuf Annonce", EtatObjet.NEUF);
        createAnnonceWithEtat("Bon Etat Annonce", EtatObjet.BON_ETAT);

        Page<Annonce> result = annonceRepository
                .findByEtatObjetAndActiveTrue(EtatObjet.NEUF, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Neuf Annonce", result.getContent().get(0).getTitre());
    }

    @Test
    void searchByQuery_ShouldSearchInTitleAndDescription() {
        Annonce annonce = createAnnonce("Canapé vintage", true);
        annonce.setDescription("Magnifique canapé en cuir");
        annonceRepository.save(annonce);

        createAnnonce("Table basse", true);

        Page<Annonce> resultByTitle = annonceRepository.searchByQuery("canapé", PageRequest.of(0, 10));
        Page<Annonce> resultByDesc = annonceRepository.searchByQuery("cuir", PageRequest.of(0, 10));

        assertEquals(1, resultByTitle.getTotalElements());
        assertEquals(1, resultByDesc.getTotalElements());
    }

    @Test
    void findByKeywords_ShouldFindByKeywordNames() {
        Keyword meubles = keywordRepository.save(new Keyword("meubles"));
        Keyword deco = keywordRepository.save(new Keyword("deco"));

        Annonce annonceWithMeubles = createAnnonce("Annonce Meubles", true);
        annonceWithMeubles.addKeyword(meubles);
        annonceRepository.save(annonceWithMeubles);

        Annonce annonceWithDeco = createAnnonce("Annonce Deco", true);
        annonceWithDeco.addKeyword(deco);
        annonceRepository.save(annonceWithDeco);

        Page<Annonce> result = annonceRepository.findByKeywords(List.of("meubles"), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Annonce Meubles", result.getContent().get(0).getTitre());
    }

    private Annonce createAnnonce(String titre, boolean active) {
        Annonce annonce = new Annonce();
        annonce.setTitre(titre);
        annonce.setDescription("Description de test pour " + titre);
        annonce.setEtatObjet(EtatObjet.BON_ETAT);
        annonce.setZoneGeographique("Paris");
        annonce.setModeLivraison(ModeLivraison.MAIN_PROPRE);
        annonce.setOwner(testUser);
        annonce.setActive(active);
        return annonceRepository.save(annonce);
    }

    private Annonce createAnnonceWithZone(String titre, String zone) {
        Annonce annonce = new Annonce();
        annonce.setTitre(titre);
        annonce.setDescription("Description de test");
        annonce.setEtatObjet(EtatObjet.BON_ETAT);
        annonce.setZoneGeographique(zone);
        annonce.setModeLivraison(ModeLivraison.MAIN_PROPRE);
        annonce.setOwner(testUser);
        annonce.setActive(true);
        return annonceRepository.save(annonce);
    }

    private Annonce createAnnonceWithEtat(String titre, EtatObjet etat) {
        Annonce annonce = new Annonce();
        annonce.setTitre(titre);
        annonce.setDescription("Description de test");
        annonce.setEtatObjet(etat);
        annonce.setZoneGeographique("Paris");
        annonce.setModeLivraison(ModeLivraison.MAIN_PROPRE);
        annonce.setOwner(testUser);
        annonce.setActive(true);
        return annonceRepository.save(annonce);
    }
}
