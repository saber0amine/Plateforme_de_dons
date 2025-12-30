package com.dev.plateforme_de_dons.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le mot-clé ne peut pas être vide")
    @Size(min = 2, max = 50)
    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "keywords")
    private Set<Annonce> annonces = new HashSet<>();

    public Keyword(String name) {
        this.name = name.toLowerCase().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keyword keyword = (Keyword) o;
        return Objects.equals(name, keyword.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
