package com.dev.plateforme_de_dons.model;

public enum EtatObjet {
    NEUF("Neuf"),
    TRES_BON_ETAT("Très bon état"),
    BON_ETAT("Bon état"),
    ETAT_CORRECT("État correct"),
    A_REPARER("À réparer");

    private final String displayName;

    EtatObjet(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
