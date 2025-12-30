package com.dev.plateforme_de_dons.dto;

import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class AnnonceDto {

    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 5, max = 100, message = "Le titre doit contenir entre 5 et 100 caractères")
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caractères")
    private String description;

    @NotNull(message = "L'état de l'objet est obligatoire")
    private EtatObjet etatObjet;

    @NotBlank(message = "La zone géographique est obligatoire")
    @Size(max = 100)
    private String zoneGeographique;

    @NotNull(message = "Le mode de livraison est obligatoire")
    private ModeLivraison modeLivraison;

    private Set<String> keywords = new HashSet<>();

    private String keywordsInput;

    private LocalDateTime datePublication;
    private String ownerUsername;
    private Long ownerId;
    private boolean active;
    private boolean reserved;
    private boolean given;
    private Long lotId;
    private int favoriteCount;

    // Gestion des images
    private List<ImageDto> images = new ArrayList<>();
    private ImageDto primaryImage;

    // Constructeurs
    public AnnonceDto() {
    }

    public AnnonceDto(Long id, String titre, String description) {
        this.id = id;
        this.titre = titre;
        this.description = description;
    }

    // Getters et Setters explicites pour les images
    public List<ImageDto> getImages() {
        return images;
    }

    public void setImages(List<ImageDto> images) {
        this.images = images;
    }

    public ImageDto getPrimaryImage() {
        return primaryImage;
    }

    public void setPrimaryImage(ImageDto primaryImage) {
        this.primaryImage = primaryImage;
    }
}