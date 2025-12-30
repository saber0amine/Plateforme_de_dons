package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.dto.ImageDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Image;
import com.dev.plateforme_de_dons.model.Lot;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.ImageService;
import com.dev.plateforme_de_dons.service.LotService;
import com.dev.plateforme_de_dons.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final AnnonceService annonceService;
    private final LotService lotService;
    private final UserService userService;

    @PostMapping("/annonce/{annonceId}")
    public ResponseEntity<ImageDto> uploadAnnonceImage(
            @PathVariable Long annonceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Annonce annonce = annonceService.findById(annonceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce non trouvée"));

        if (!annonce.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à ajouter des images à cette annonce");
        }

        try {
            Image image = imageService.uploadImageForAnnonce(file, annonce, isPrimary);
            return ResponseEntity.status(HttpStatus.CREATED).body(imageService.convertToDto(image));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'upload de l'image");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<ImageDto> uploadLotImage(
            @PathVariable Long lotId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Lot lot = lotService.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot non trouvé"));

        if (!lot.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à ajouter des images à ce lot");
        }

        try {
            Image image = imageService.uploadImageForLot(file, lot, isPrimary);
            return ResponseEntity.status(HttpStatus.CREATED).body(imageService.convertToDto(image));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'upload de l'image");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        Image image = imageService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image non trouvée"));

        try {
            byte[] imageBytes = imageService.getImageBytes(image);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .body(imageBytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération de l'image");
        }
    }

    @GetMapping("/annonce/{annonceId}")
    public ResponseEntity<List<ImageDto>> getAnnonceImages(@PathVariable Long annonceId) {
        Annonce annonce = annonceService.findById(annonceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce non trouvée"));

        List<Image> images = imageService.findByAnnonce(annonce);
        return ResponseEntity.ok(imageService.convertToDtoList(images));
    }

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<ImageDto>> getLotImages(@PathVariable Long lotId) {
        Lot lot = lotService.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot non trouvé"));

        List<Image> images = imageService.findByLot(lot);
        return ResponseEntity.ok(imageService.convertToDtoList(images));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Image image = imageService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image non trouvée"));

        // Vérifier que l'utilisateur est propriétaire
        boolean isOwner = false;
        if (image.getAnnonce() != null) {
            isOwner = image.getAnnonce().getOwner().getId().equals(user.getId());
        } else if (image.getLot() != null) {
            isOwner = image.getLot().getCreator().getId().equals(user.getId());
        }

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à supprimer cette image");
        }

        try {
            imageService.deleteImage(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression de l'image");
        }
    }

    @PutMapping("/{id}/set-primary")
    public ResponseEntity<Map<String, Object>> setPrimaryImage(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Image image = imageService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image non trouvée"));

        // Vérifier que l'utilisateur est propriétaire
        boolean isOwner = false;
        if (image.getAnnonce() != null) {
            isOwner = image.getAnnonce().getOwner().getId().equals(user.getId());
        } else if (image.getLot() != null) {
            isOwner = image.getLot().getCreator().getId().equals(user.getId());
        }

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à modifier cette image");
        }

        imageService.setPrimaryImage(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Image principale définie avec succès");
        return ResponseEntity.ok(response);
    }
}