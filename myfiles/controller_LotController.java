package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.dto.AnnonceDto;
import com.dev.plateforme_de_dons.dto.LotDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Image;
import com.dev.plateforme_de_dons.model.Lot;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.ImageService;
import com.dev.plateforme_de_dons.service.LotService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;
    private final UserService userService;
    private final AnnonceService annonceService;
    private final ImageService imageService;

    @GetMapping
    public String listLots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Lot> lotsPage = lotService.findAllActive(pageable);

        model.addAttribute("lots", lotsPage.map(this::convertToDto));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", lotsPage.getTotalPages());

        return "lots/list";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listLotsJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Lot> lotsPage = lotService.findAllActive(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("lots", lotsPage.map(this::convertToDto).getContent());
        response.put("currentPage", page);
        response.put("totalPages", lotsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public String viewLot(@PathVariable Long id, Model model, Authentication authentication) {
        Lot lot = lotService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot non trouvé"));

        model.addAttribute("lot", convertToDto(lot));

        if (authentication != null) {
            userService.findByUsername(authentication.getName()).ifPresent(user -> {
                model.addAttribute("isOwner", lot.getCreator().getId().equals(user.getId()));
            });
        }

        return "lots/view";
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<LotDto> viewLotJson(@PathVariable Long id) {
        return lotService.findById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        List<Annonce> availableAnnonces = lotService.getAvailableAnnoncesForLot(user);

        model.addAttribute("lot", new LotDto());
        model.addAttribute("availableAnnonces", availableAnnonces.stream()
                .map(annonceService::convertToDto)
                .collect(Collectors.toList()));
        model.addAttribute("editing", false);

        return "lots/form";
    }

    @PostMapping
    public String createLot(
            @Valid @ModelAttribute("lot") LotDto lotDto,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (result.hasErrors()) {
            List<Annonce> availableAnnonces = lotService.getAvailableAnnoncesForLot(user);
            model.addAttribute("availableAnnonces", availableAnnonces.stream()
                    .map(annonceService::convertToDto)
                    .collect(Collectors.toList()));
            return "lots/form";
        }

        try {
            Lot lot = lotService.createLot(lotDto, user);
            redirectAttributes.addFlashAttribute("success", "Lot créé avec succès !");
            return "redirect:/lots/" + lot.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            List<Annonce> availableAnnonces = lotService.getAvailableAnnoncesForLot(user);
            model.addAttribute("availableAnnonces", availableAnnonces.stream()
                    .map(annonceService::convertToDto)
                    .collect(Collectors.toList()));
            return "lots/form";
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<LotDto> createLotJson(
            @Valid @RequestBody LotDto lotDto,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Lot lot = lotService.createLot(lotDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(lot));
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        Lot lot = lotService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot non trouvé"));

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!lot.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé");
        }

        List<Annonce> availableAnnonces = lotService.getAvailableAnnoncesForLot(user);

        model.addAttribute("lot", convertToDto(lot));
        model.addAttribute("availableAnnonces", availableAnnonces.stream()
                .map(annonceService::convertToDto)
                .collect(Collectors.toList()));
        model.addAttribute("editing", true);

        return "lots/form";
    }

    @PostMapping("/{id}")
    public String updateLot(
            @PathVariable Long id,
            @Valid @ModelAttribute("lot") LotDto lotDto,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (result.hasErrors()) {
            List<Annonce> availableAnnonces = lotService.getAvailableAnnoncesForLot(user);
            model.addAttribute("availableAnnonces", availableAnnonces.stream()
                    .map(annonceService::convertToDto)
                    .collect(Collectors.toList()));
            model.addAttribute("editing", true);
            return "lots/form";
        }

        try {
            lotService.updateLot(id, lotDto, user);
            redirectAttributes.addFlashAttribute("success", "Lot mis à jour avec succès !");
            return "redirect:/lots/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            List<Annonce> availableAnnonces = lotService.getAvailableAnnoncesForLot(user);
            model.addAttribute("availableAnnonces", availableAnnonces.stream()
                    .map(annonceService::convertToDto)
                    .collect(Collectors.toList()));
            model.addAttribute("editing", true);
            return "lots/form";
        }
    }

    @PostMapping("/{lotId}/add-annonce/{annonceId}")
    public String addAnnonceToLot(
            @PathVariable Long lotId,
            @PathVariable Long annonceId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            lotService.addAnnonceToLot(lotId, annonceId, user);
            redirectAttributes.addFlashAttribute("success", "Annonce ajoutée au lot !");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/lots/" + lotId;
    }

    @PostMapping("/{lotId}/remove-annonce/{annonceId}")
    public String removeAnnonceFromLot(
            @PathVariable Long lotId,
            @PathVariable Long annonceId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            lotService.removeAnnonceFromLot(lotId, annonceId, user);
            redirectAttributes.addFlashAttribute("success", "Annonce retirée du lot !");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/lots/" + lotId;
    }

    @PostMapping("/{id}/delete")
    public String deleteLot(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        lotService.deactivateLot(id, user);
        redirectAttributes.addFlashAttribute("success", "Lot supprimé avec succès !");
        return "redirect:/mes-lots";
    }

    @GetMapping("/mes-lots")
    public String myLots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Lot> lotsPage = lotService.findByCreator(user, pageable);

        model.addAttribute("lots", lotsPage.map(this::convertToDto));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", lotsPage.getTotalPages());

        return "lots/my-lots";
    }

    private LotDto convertToDto(Lot lot) {
        LotDto dto = new LotDto();
        dto.setId(lot.getId());
        dto.setTitre(lot.getTitre());
        dto.setDescription(lot.getDescription());
        dto.setCreatorId(lot.getCreator().getId());
        dto.setCreatorUsername(lot.getCreator().getUsername());
        dto.setActive(lot.isActive());
        dto.setCreatedAt(lot.getCreatedAt());
        dto.setAnnonceIds(lot.getAnnonces().stream()
                .map(Annonce::getId)
                .collect(Collectors.toList()));
        dto.setAnnonces(lot.getAnnonces().stream()
                .map(annonceService::convertToDto)
                .collect(Collectors.toList()));

        // Ajouter les images
        dto.setImages(imageService.convertToDtoList(lot.getImages()));
        Image primaryImage = lot.getPrimaryImage();
        if (primaryImage != null) {
            dto.setPrimaryImage(imageService.convertToDto(primaryImage));
        }

        return dto;
    }
}