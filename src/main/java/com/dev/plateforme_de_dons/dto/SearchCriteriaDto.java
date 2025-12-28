package com.dev.plateforme_de_dons.dto;

import com.dev.plateforme_de_dons.model.EtatObjet;
import com.dev.plateforme_de_dons.model.ModeLivraison;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaDto {

    private String query;
    private String zoneGeographique;
    private EtatObjet etatObjet;
    private ModeLivraison modeLivraison;
    private List<String> keywords;
    private String keywordsInput;

    public boolean isEmpty() {
        return (query == null || query.isBlank()) &&
               (zoneGeographique == null || zoneGeographique.isBlank()) &&
               etatObjet == null &&
               modeLivraison == null &&
               (keywords == null || keywords.isEmpty());
    }
}
