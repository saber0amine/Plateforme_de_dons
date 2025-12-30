package com.dev.plateforme_de_dons.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "annonces", indexes = {
        @Index(name = "idx_annonce_zone", columnList = "zoneGeographique"),
        @Index(name = "idx_annonce_etat", columnList = "etatObjet"),
        @Index(name = "idx_annonce_date", columnList = "datePublication"),
        @Index(name = "idx_annonce_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Annonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 5, max = 100)
    @Column(nullable = false)
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 2000)
    @Column(nullable = false, length = 2000)
    private String description;

    @NotNull(message = "L'état de l'objet est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatObjet etatObjet;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime datePublication;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @NotBlank(message = "La zone géographique est obligatoire")
    @Size(max = 100)
    @Column(nullable = false)
    private String zoneGeographique;

    @NotNull(message = "Le mode de livraison est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeLivraison modeLivraison;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "annonce_keywords",
            joinColumns = @JoinColumn(name = "annonce_id"),
            inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    private Set<Keyword> keywords = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "annonce", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Favorite> favorites = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @OneToMany(mappedBy = "annonce", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    private boolean active = true;

    private boolean reserved = false;

    private boolean given = false;

    @Version
    private Long version;

    public void addKeyword(Keyword keyword) {
        keywords.add(keyword);
        keyword.getAnnonces().add(this);
    }

    public void removeKeyword(Keyword keyword) {
        keywords.remove(keyword);
        keyword.getAnnonces().remove(this);
    }

    public void addImage(Image image) {
        images.add(image);
        image.setAnnonce(this);
    }

    public void removeImage(Image image) {
        images.remove(image);
        image.setAnnonce(null);
    }

    public Image getPrimaryImage() {
        return images.stream()
                .filter(Image::isPrimary)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
    }
}