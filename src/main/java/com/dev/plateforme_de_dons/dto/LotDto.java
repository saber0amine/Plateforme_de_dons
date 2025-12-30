package com.dev.plateforme_de_dons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class LotDto {

    private Long id;

    @NotBlank(message = "Le titre du lot est obligatoire")
    @Size(min = 5, max = 100, message = "Le titre doit contenir entre 5 et 100 caractères")
    private String titre;

    @Size(max = 500)
    private String description;

    private Long creatorId;
    private String creatorUsername;

    private List<Long> annonceIds = new ArrayList<>();
    private List<AnnonceDto> annonces = new ArrayList<>();

    private boolean active;
    private LocalDateTime createdAt;

    private String imageUrl;

    private List<ImageDto> images = new ArrayList<>();
    private ImageDto primaryImage;

     public LotDto() {
    }

    public LotDto(Long id, String titre, String description) {
        this.id = id;
        this.titre = titre;
        this.description = description;
    }

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