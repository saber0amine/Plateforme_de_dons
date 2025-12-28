package com.dev.plateforme_de_dons.dto;

import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedSearchDto {

    private Long id;

    @NotBlank(message = "Le nom de la recherche est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String name;

    private String query;
    private String zoneGeographique;
    private EtatObjet etatObjet;
    private ModeLivraison modeLivraison;
    private String keywords;

    private boolean notificationsEnabled = true;

    private LocalDateTime createdAt;
    private LocalDateTime lastNotificationAt;
}
