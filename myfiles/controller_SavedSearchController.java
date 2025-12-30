package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.dto.SavedSearchDto;
import com.dev.plateforme_de_dons.model.SavedSearch;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.SavedSearchService;
import com.dev.plateforme_de_dons.service.UserService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/saved-searches")
@RequiredArgsConstructor
public class SavedSearchController {

    private final SavedSearchService savedSearchService;
    private final UserService userService;
    private final AnnonceService annonceService;

    @GetMapping
    public String listSavedSearches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SavedSearch> searchesPage = savedSearchService.findByUser(user, pageable);

        model.addAttribute("savedSearches", searchesPage.map(savedSearchService::convertToDto));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", searchesPage.getTotalPages());

        return "saved-searches/list";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listSavedSearchesJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SavedSearch> searchesPage = savedSearchService.findByUser(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("savedSearches", searchesPage.map(savedSearchService::convertToDto).getContent());
        response.put("currentPage", page);
        response.put("totalPages", searchesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/execute")
    public String executeSavedSearch(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        SavedSearch savedSearch = savedSearchService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recherche non trouvée"));

        if (!savedSearch.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé");
        }

        var criteria = savedSearchService.convertToSearchCriteria(savedSearch);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublication"));
        var results = annonceService.search(criteria, pageable);

        model.addAttribute("savedSearch", savedSearchService.convertToDto(savedSearch));
        model.addAttribute("annonces", results.map(annonceService::convertToDto));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", results.getTotalPages());

        return "saved-searches/results";
    }

    @PostMapping("/{id}/toggle-notifications")
    public String toggleNotifications(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        savedSearchService.toggleNotifications(id, user);
        redirectAttributes.addFlashAttribute("success", "Paramètres de notification mis à jour !");
        return "redirect:/saved-searches";
    }

    @PostMapping(value = "/{id}/toggle-notifications", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleNotificationsJson(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        savedSearchService.toggleNotifications(id, user);

        SavedSearch savedSearch = savedSearchService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("notificationsEnabled", savedSearch.isNotificationsEnabled());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/delete")
    public String deleteSavedSearch(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        savedSearchService.deleteSavedSearch(id, user);
        redirectAttributes.addFlashAttribute("success", "Recherche supprimée avec succès !");
        return "redirect:/saved-searches";
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> deleteSavedSearchJson(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        savedSearchService.deleteSavedSearch(id, user);
        return ResponseEntity.noContent().build();
    }
}
