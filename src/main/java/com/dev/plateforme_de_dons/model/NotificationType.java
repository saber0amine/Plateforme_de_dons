package com.dev.plateforme_de_dons.model;

public enum NotificationType {
    NEW_ANNONCE_MATCH("Nouvelle annonce correspondante"),
    NEW_MESSAGE("Nouveau message"),
    ANNONCE_RESERVED("Annonce réservée"),
    ANNONCE_GIVEN("Don effectué");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
