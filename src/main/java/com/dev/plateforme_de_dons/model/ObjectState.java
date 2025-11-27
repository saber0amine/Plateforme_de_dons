package com.dev.plateforme_de_dons.model;

public enum ObjectState {
    NEW("Neuf"),
    VERY_GOOD("Très bon état"),
    GOOD("Bon état"),
    ACCEPTABLE("État acceptable"),
    FOR_PARTS("Pour pièces");

    private final String displayName;

    ObjectState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}