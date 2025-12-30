package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.dto.AnnonceDto;
import com.dev.plateforme_de_dons.dto.SearchCriteriaDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.FavoriteService;
import com.dev.plateforme_de_dons.service.ImageService;
import com.dev.plateforme_de_dons.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/annonces")
@RequiredArgsConstructor
public class AnnonceController {

    private final AnnonceService annonceService;
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final ImageService imageService;

    @GetMapping
    public String listAnnonces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublication"));
        Page<Annonce> annoncesPage = annonceService.findAllActive(pageable);

        model.addAttribute("annonces", annoncesPage.map(annonceService::convertToDto));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", annoncesPage.getTotalPages());

        return "annonces/list";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listAnnoncesJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublication"));
        Page<Annonce> annoncesPage = annonceService.findAllActive(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("annonces", annoncesPage.map(annonceService::convertToDto).getContent());
        response.put("currentPage", page);
        response.put("totalPages", annoncesPage.getTotalPages());
        response.put("totalElements", annoncesPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public String viewAnnonce(@PathVariable Long id, Model model, Authentication authentication) {
        Annonce annonce = annonceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce non trouvée"));

        AnnonceDto dto = annonceService.convertToDto(annonce);
        model.addAttribute("annonce", dto);

        if (authentication != null) {
            userService.findByUsername(authentication.getName()).ifPresent(user -> {
                model.addAttribute("isFavorite", favoriteService.isFavorite(user, annonce));
                model.addAttribute("isOwner", annonce.getOwner().getId().equals(user.getId()));
            });
        }

        return "annonces/view";
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AnnonceDto> viewAnnonceJson(@PathVariable Long id) {
        return annonceService.findById(id)
                .map(annonceService::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("annonce", new AnnonceDto());
        model.addAttribute("etats", EtatObjet.values());
        model.addAttribute("modes", ModeLivraison.values());
        model.addAttribute("editing", false);
        return "annonces/form";
    }

    @PostMapping
    public String createAnnonce(
            @Valid @ModelAttribute("annonce") AnnonceDto annonceDto,
            BindingResult result,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("etats", EtatObjet.values());
            model.addAttribute("modes", ModeLivraison.values());
            model.addAttribute("editing", false);
            return "annonces/form";
        }

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            // Créer l'annonce
            Annonce annonce = annonceService.createAnnonce(annonceDto, user);

            // Uploader les images si présentes
            if (imageFiles != null && !imageFiles.isEmpty()) {
                boolean firstImage = true;
                for (MultipartFile file : imageFiles) {
                    if (!file.isEmpty()) {
                        imageService.uploadImageForAnnonce(file, annonce, firstImage);
                        firstImage = false; // Seulement la première est primary
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Annonce créée avec succès !");
            return "redirect:/annonces/" + annonce.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la création : " + e.getMessage());
            model.addAttribute("etats", EtatObjet.values());
            model.addAttribute("modes", ModeLivraison.values());
            model.addAttribute("editing", false);
            return "annonces/form";
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> createAnnonceJson(
            @Valid @RequestBody AnnonceDto annonceDto,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Annonce annonce = annonceService.createAnnonce(annonceDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(annonceService.convertToDto(annonce));
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        Annonce annonce = annonceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce non trouvée"));

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!annonce.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé");
        }

        model.addAttribute("annonce", annonceService.convertToDto(annonce));
        model.addAttribute("etats", EtatObjet.values());
        model.addAttribute("modes", ModeLivraison.values());
        model.addAttribute("editing", true);

        return "annonces/form";
    }

    @PostMapping("/{id}")
    public String updateAnnonce(
            @PathVariable Long id,
            @Valid @ModelAttribute("annonce") AnnonceDto annonceDto,
            BindingResult result,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("etats", EtatObjet.values());
            model.addAttribute("modes", ModeLivraison.values());
            model.addAttribute("editing", true);
            return "annonces/form";
        }

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            // Mettre à jour l'annonce
            Annonce annonce = annonceService.updateAnnonce(id, annonceDto, user);

            // Uploader les nouvelles images si présentes
            if (imageFiles != null && !imageFiles.isEmpty()) {
                boolean hasPrimaryImage = !annonce.getImages().isEmpty() &&
                        annonce.getImages().stream().anyMatch(img -> img.isPrimary());

                for (MultipartFile file : imageFiles) {
                    if (!file.isEmpty()) {
                        imageService.uploadImageForAnnonce(file, annonce, !hasPrimaryImage);
                        hasPrimaryImage = true; // Après la première, on a une image primary
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Annonce mise à jour avec succès !");
            return "redirect:/annonces/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la mise à jour : " + e.getMessage());
            model.addAttribute("etats", EtatObjet.values());
            model.addAttribute("modes", ModeLivraison.values());
            model.addAttribute("editing", true);
            return "annonces/form";
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateAnnonceJson(
            @PathVariable Long id,
            @Valid @RequestBody AnnonceDto annonceDto,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Annonce annonce = annonceService.updateAnnonce(id, annonceDto, user);
        return ResponseEntity.ok(annonceService.convertToDto(annonce));
    }

    @PostMapping("/{id}/delete")
    public String deleteAnnonce(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        annonceService.deactivateAnnonce(id, user);
        redirectAttributes.addFlashAttribute("success", "Annonce supprimée avec succès !");
        return "redirect:/mes-annonces";
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> deleteAnnonceJson(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        annonceService.deactivateAnnonce(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reserve")
    public String reserveAnnonce(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        annonceService.markAsReserved(id, user);
        redirectAttributes.addFlashAttribute("success", "Annonce marquée comme réservée !");
        return "redirect:/annonces/" + id;
    }

    @PostMapping("/{id}/give")
    public String markAsGiven(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        annonceService.markAsGiven(id, user);
        redirectAttributes.addFlashAttribute("success", "Don effectué ! Merci pour votre générosité !");
        return "redirect:/mes-annonces";
    }

    @GetMapping("/mes-annonces")
    public String myAnnonces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublication"));
        Page<Annonce> annoncesPage = annonceService.findByOwner(user, pageable);

        model.addAttribute("annonces", annoncesPage.map(annonceService::convertToDto));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", annoncesPage.getTotalPages());

        return "annonces/my-annonces";
    }
}