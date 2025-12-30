package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.dto.AnnonceDto;
import com.dev.plateforme_de_dons.dto.SavedSearchDto;
import com.dev.plateforme_de_dons.dto.SearchCriteriaDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final AnnonceService annonceService;
    private final SavedSearchService savedSearchService;
    private final UserService userService;

    @GetMapping
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) EtatObjet etat,
            @RequestParam(required = false) ModeLivraison mode,
            @RequestParam(required = false) String keywords,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        SearchCriteriaDto criteria = buildCriteria(query, zone, etat, mode, keywords);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublication"));
        Page<Annonce> results = annonceService.search(criteria, pageable);

        model.addAttribute("annonces", results.map(annonceService::convertToDto));
        model.addAttribute("criteria", criteria);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", results.getTotalPages());
        model.addAttribute("etats", EtatObjet.values());
        model.addAttribute("modes", ModeLivraison.values());

        if (authentication != null) {
            model.addAttribute("canSave", true);
        }

        return "search/results";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchJson(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) EtatObjet etat,
            @RequestParam(required = false) ModeLivraison mode,
            @RequestParam(required = false) String keywords,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        SearchCriteriaDto criteria = buildCriteria(query, zone, etat, mode, keywords);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublication"));
        Page<Annonce> results = annonceService.search(criteria, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("annonces", results.map(annonceService::convertToDto).getContent());
        response.put("currentPage", page);
        response.put("totalPages", results.getTotalPages());
        response.put("totalElements", results.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public String saveSearch(
            @RequestParam String name,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) EtatObjet etat,
            @RequestParam(required = false) ModeLivraison mode,
            @RequestParam(required = false) String keywords,
            @RequestParam(defaultValue = "true") boolean notifications,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        SearchCriteriaDto criteria = buildCriteria(query, zone, etat, mode, keywords);
        savedSearchService.saveSearchFromCriteria(name, criteria, user, notifications);

        redirectAttributes.addFlashAttribute("success", "Recherche sauvegardée avec succès !");
        return "redirect:/saved-searches";
    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<SavedSearchDto> saveSearchJson(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        String name = (String) request.get("name");
        String query = (String) request.get("query");
        String zone = (String) request.get("zone");
        EtatObjet etat = request.get("etat") != null ? EtatObjet.valueOf((String) request.get("etat")) : null;
        ModeLivraison mode = request.get("mode") != null ? ModeLivraison.valueOf((String) request.get("mode")) : null;
        String keywords = (String) request.get("keywords");
        boolean notifications = request.get("notifications") != null ? (Boolean) request.get("notifications") : true;

        SearchCriteriaDto criteria = buildCriteria(query, zone, etat, mode, keywords);
        var savedSearch = savedSearchService.saveSearchFromCriteria(name, criteria, user, notifications);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedSearchService.convertToDto(savedSearch));
    }

    private SearchCriteriaDto buildCriteria(String query, String zone, EtatObjet etat, ModeLivraison mode, String keywords) {
        SearchCriteriaDto criteria = new SearchCriteriaDto();
        criteria.setQuery(query);
        criteria.setZoneGeographique(zone);
        criteria.setEtatObjet(etat);
        criteria.setModeLivraison(mode);

        if (keywords != null && !keywords.isBlank()) {
            criteria.setKeywords(
                    Arrays.stream(keywords.split(","))
                            .map(String::trim)
                            .filter(k -> !k.isEmpty())
                            .toList()
            );
            criteria.setKeywordsInput(keywords);
        }

        return criteria;
    }
}
