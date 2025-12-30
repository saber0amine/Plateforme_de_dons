package com.dev.plateforme_de_dons.model;

public enum ModeLivraison {
    MAIN_PROPRE("En main propre"),
    ENVOI("Envoi possible"),
    LES_DEUX("Main propre ou envoi");

    private final String displayName;

    ModeLivraison(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
