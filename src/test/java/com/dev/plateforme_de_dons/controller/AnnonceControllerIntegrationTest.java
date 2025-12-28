package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnnonceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password"));
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void listAnnonces_ShouldReturnHtml_WhenAcceptHtml() throws Exception {
        mockMvc.perform(get("/annonces")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("annonces/list"));
    }

    @Test
    void listAnnonces_ShouldReturnJson_WhenAcceptJson() throws Exception {
        mockMvc.perform(get("/annonces")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.annonces").isArray());
    }

    @Test
    @WithMockUser(username = "testuser")
    void showCreateForm_ShouldReturnForm_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/annonces/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("annonces/form"))
                .andExpect(model().attributeExists("annonce"))
                .andExpect(model().attributeExists("etats"))
                .andExpect(model().attributeExists("modes"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createAnnonce_ShouldCreateAndRedirect_WhenValidData() throws Exception {
        mockMvc.perform(post("/annonces")
                        .with(csrf())
                        .param("titre", "Canapé à donner")
                        .param("description", "Beau canapé en bon état à donner gratuitement")
                        .param("etatObjet", EtatObjet.BON_ETAT.name())
                        .param("zoneGeographique", "Paris 15e")
                        .param("modeLivraison", ModeLivraison.MAIN_PROPRE.name())
                        .param("keywordsInput", "meubles, canapé"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/annonces/*"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createAnnonce_ShouldReturnForm_WhenInvalidData() throws Exception {
        mockMvc.perform(post("/annonces")
                        .with(csrf())
                        .param("titre", "Ab")  // Too short
                        .param("description", "Short"))  // Too short
                .andExpect(status().isOk())
                .andExpect(view().name("annonces/form"));
    }

    @Test
    void viewAnnonce_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/annonces/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_ShouldReturnResults_WhenCriteriaProvided() throws Exception {
        mockMvc.perform(get("/search")
                        .param("zone", "Paris")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("search/results"))
                .andExpect(model().attributeExists("annonces"))
                .andExpect(model().attributeExists("criteria"));
    }

    @Test
    void search_ShouldReturnJson_WhenAcceptJson() throws Exception {
        mockMvc.perform(get("/search")
                        .param("zone", "Paris")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.annonces").isArray());
    }
}
