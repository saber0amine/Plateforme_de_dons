package com.dev.plateforme_de_dons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
