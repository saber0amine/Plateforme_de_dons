package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.model.Favorite;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.FavoriteService;
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
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final AnnonceService annonceService;

    @GetMapping
    public String listFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Favorite> favoritesPage = favoriteService.getFavorites(user, pageable);

        model.addAttribute("favorites", favoritesPage.map(f -> annonceService.convertToDto(f.getAnnonce())));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", favoritesPage.getTotalPages());

        return "favorites/list";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listFavoritesJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Favorite> favoritesPage = favoriteService.getFavorites(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("favorites", favoritesPage.map(f -> annonceService.convertToDto(f.getAnnonce())).getContent());
        response.put("currentPage", page);
        response.put("totalPages", favoritesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/add/{annonceId}")
    public String addToFavorites(
            @PathVariable Long annonceId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            favoriteService.addToFavorites(user, annonceId);
            redirectAttributes.addFlashAttribute("success", "Annonce ajoutée aux favoris !");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/annonces/" + annonceId;
    }

    @PostMapping(value = "/add/{annonceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToFavoritesJson(
            @PathVariable Long annonceId,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        favoriteService.addToFavorites(user, annonceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Annonce ajoutée aux favoris");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/remove/{annonceId}")
    public String removeFromFavorites(
            @PathVariable Long annonceId,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        favoriteService.removeFromFavorites(user, annonceId);
        redirectAttributes.addFlashAttribute("success", "Annonce retirée des favoris !");

        if (returnUrl != null && returnUrl.equals("favorites")) {
            return "redirect:/favorites";
        }
        return "redirect:/annonces/" + annonceId;
    }

    @DeleteMapping(value = "/{annonceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromFavoritesJson(
            @PathVariable Long annonceId,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        favoriteService.removeFromFavorites(user, annonceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Annonce retirée des favoris");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getFavoritesCount(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Map<String, Long> response = new HashMap<>();
        response.put("count", favoriteService.countFavorites(user));
        return ResponseEntity.ok(response);
    }
}
