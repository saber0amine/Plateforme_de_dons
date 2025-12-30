package com.dev.plateforme_de_dons.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre du lot est obligatoire")
    @Size(min = 5, max = 100)
    @Column(nullable = false)
    private String titre;

    @Size(max = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @OneToMany(mappedBy = "lot", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Annonce> annonces = new ArrayList<>();

    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void addAnnonce(Annonce annonce) {
        annonces.add(annonce);
        annonce.setLot(this);
    }

    public void removeAnnonce(Annonce annonce) {
        annonces.remove(annonce);
        annonce.setLot(null);
    }

    public void addImage(Image image) {
        images.add(image);
        image.setLot(this);
    }

    public void removeImage(Image image) {
        images.remove(image);
        image.setLot(null);
    }

    public Image getPrimaryImage() {
        return images.stream()
                .filter(Image::isPrimary)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
    }
}