# Plateforme de Dons - Documentation Technique

## Membres du groupe
- ZGAOUA Mohamed
- SABER Amine

---

## 1. Architecture du Système

### 1.1 Vue d'ensemble

L'application est construite avec **Spring Boot 3.5.6** suivant une architecture en couches (Layered Architecture) :

```
┌─────────────────────────────────────────────────────────────┐
│                    Couche Présentation                       │
│         (Controllers + Templates Thymeleaf)                  │
├─────────────────────────────────────────────────────────────┤
│                    Couche Service                            │
│              (Logique métier)                                │
├─────────────────────────────────────────────────────────────┤
│                    Couche Repository                         │
│              (Accès aux données JPA)                         │
├─────────────────────────────────────────────────────────────┤
│                    Base de données                           │
│              (H2 dev / PostgreSQL prod)                      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Technologies utilisées

| Composant | Technologie |
|-----------|-------------|
| Framework | Spring Boot 3.5.6 |
| ORM | Spring Data JPA / Hibernate |
| Sécurité | Spring Security 6 |
| Templates | Thymeleaf |
| Base de données (dev) | H2 (embarquée) |
| Base de données (prod) | Configurable (PostgreSQL recommandé) |
| Build | Maven |
| Java | 17 |

### 1.3 Structure des packages

```
com.dev.plateforme_de_dons/
├── config/             # Configuration (Security, Web, Scheduling)
├── controller/         # Contrôleurs REST/Web
├── dto/               # Data Transfer Objects
├── model/             # Entités JPA
├── repository/        # Repositories Spring Data
└── service/           # Services métier
```

---

## 2. Modèle de Données

### 2.1 Diagramme Entité-Relation

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    User      │────<│   Annonce    │>────│   Keyword    │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id           │     │ id           │     │ id           │
│ username     │     │ titre        │     │ name         │
│ email        │     │ description  │     └──────────────┘
│ password     │     │ etatObjet    │
│ firstName    │     │ datePublication│
│ lastName     │     │ zoneGeographique│
│ location     │     │ modeLivraison│
└──────────────┘     │ active       │
       │             │ reserved     │
       │             │ given        │
       │             └──────────────┘
       │                    │
       ▼                    ▼
┌──────────────┐     ┌──────────────┐
│   Favorite   │     │     Lot      │
├──────────────┤     ├──────────────┤
│ id           │     │ id           │
│ user_id (FK) │     │ titre        │
│ annonce_id   │     │ description  │
│ createdAt    │     │ creator_id   │
└──────────────┘     │ active       │
                     └──────────────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Message    │     │ SavedSearch  │     │ Notification │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id           │     │ id           │     │ id           │
│ sender_id    │     │ name         │     │ user_id      │
│ receiver_id  │     │ user_id      │     │ title        │
│ annonce_id   │     │ query        │     │ message      │
│ content      │     │ zone         │     │ type         │
│ sentAt       │     │ etatObjet    │     │ read         │
│ read         │     │ notifications│     │ createdAt    │
└──────────────┘     └──────────────┘     └──────────────┘
```

### 2.2 Description des entités

| Entité | Description |
|--------|-------------|
| **User** | Utilisateur de la plateforme (donneur/receveur) |
| **Annonce** | Objet proposé au don |
| **Keyword** | Mots-clés pour la recherche |
| **Favorite** | Annonces mises en favoris |
| **Lot** | Regroupement d'annonces pour transaction groupée |
| **Message** | Messagerie interne entre utilisateurs |
| **SavedSearch** | Recherches sauvegardées avec notifications |
| **Notification** | Notifications utilisateur |

---

## 3. API REST - Ressources et URLs

### 3.1 Annonces

| Méthode | URL | Description | Auth | Représentations |
|---------|-----|-------------|------|-----------------|
| GET | `/annonces` | Liste des annonces | Non | HTML, JSON |
| GET | `/annonces/{id}` | Détail d'une annonce | Non | HTML, JSON |
| POST | `/annonces` | Créer une annonce | Oui | HTML Form, JSON |
| PUT | `/annonces/{id}` | Modifier une annonce | Oui (owner) | JSON |
| POST | `/annonces/{id}` | Modifier une annonce | Oui (owner) | HTML Form |
| DELETE | `/annonces/{id}` | Supprimer une annonce | Oui (owner) | JSON |
| POST | `/annonces/{id}/delete` | Supprimer une annonce | Oui (owner) | HTML |
| POST | `/annonces/{id}/reserve` | Marquer réservé | Oui (owner) | HTML |
| POST | `/annonces/{id}/give` | Marquer donné | Oui (owner) | HTML |

### 3.2 Recherche

| Méthode | URL | Description | Auth | Représentations |
|---------|-----|-------------|------|-----------------|
| GET | `/search` | Recherche avec filtres | Non | HTML, JSON |
| POST | `/search/save` | Sauvegarder recherche | Oui | HTML, JSON |

**Paramètres de recherche :**
- `query` : Texte libre (titre/description)
- `zone` : Zone géographique
- `etat` : État de l'objet (enum)
- `mode` : Mode de livraison (enum)
- `keywords` : Mots-clés séparés par virgules

### 3.3 Messages

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| GET | `/messages` | Liste des messages | Oui |
| GET | `/messages/conversation/{userId}` | Conversation | Oui |
| POST | `/messages` | Envoyer un message | Oui |
| POST | `/messages/{id}/read` | Marquer comme lu | Oui |

### 3.4 Favoris

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| GET | `/favorites` | Liste des favoris | Oui |
| POST | `/favorites/add/{annonceId}` | Ajouter aux favoris | Oui |
| POST | `/favorites/remove/{annonceId}` | Retirer des favoris | Oui |
| DELETE | `/favorites/{annonceId}` | Retirer (REST) | Oui |

### 3.5 Lots

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| GET | `/lots` | Liste des lots | Non |
| GET | `/lots/{id}` | Détail d'un lot | Non |
| POST | `/lots` | Créer un lot | Oui |
| POST | `/lots/{id}` | Modifier un lot | Oui (owner) |
| POST | `/lots/{lotId}/add-annonce/{annonceId}` | Ajouter annonce | Oui (owner) |
| POST | `/lots/{lotId}/remove-annonce/{annonceId}` | Retirer annonce | Oui (owner) |

### 3.6 Recherches Sauvegardées

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| GET | `/saved-searches` | Liste des recherches | Oui |
| GET | `/saved-searches/{id}/execute` | Exécuter recherche | Oui |
| POST | `/saved-searches/{id}/toggle-notifications` | Activer/désactiver notifs | Oui |
| DELETE | `/saved-searches/{id}` | Supprimer recherche | Oui |

### 3.7 Notifications

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| GET | `/notifications` | Liste des notifications | Oui |
| POST | `/notifications/{id}/read` | Marquer comme lue | Oui |
| POST | `/notifications/mark-all-read` | Tout marquer lu | Oui |

---

## 4. Négociation de Contenu

L'application supporte la négociation de contenu via l'en-tête `Accept` :

- `Accept: text/html` → Réponse HTML (templates Thymeleaf)
- `Accept: application/json` → Réponse JSON (API REST)

Exemple :
```bash
# Obtenir du HTML
curl -H "Accept: text/html" http://localhost:8080/annonces

# Obtenir du JSON
curl -H "Accept: application/json" http://localhost:8080/annonces
```

---

## 5. Passage à l'échelle

### 5.1 Cache HTTP

- En-têtes `Cache-Control` configurés pour les ressources statiques
- Support pour les ETags (gérés automatiquement par Spring)

### 5.2 Pagination

Toutes les listes sont paginées :
- Taille par défaut : 20 éléments
- Taille maximum : 100 éléments
- Paramètres : `page` et `size`

### 5.3 Optimisation des requêtes

- Index sur les colonnes fréquemment filtrées
- Lazy loading pour les relations
- Requêtes optimisées avec Spring Data JPA

### 5.4 Répartition de charge

L'application est stateless (sans état de session côté serveur critique), permettant :
- Déploiement sur plusieurs instances
- Load balancing facile
- Scalabilité horizontale

---

## 6. Sécurité

### 6.1 Authentification

- Spring Security avec formulaire de connexion
- Mots de passe hashés avec BCrypt
- Sessions HTTP avec cookies

### 6.2 Autorisation

- Endpoints publics : consultation des annonces et lots
- Endpoints protégés : création, modification, messagerie
- Vérification de propriété pour les modifications

### 6.3 Protection CSRF

- Tokens CSRF pour les formulaires HTML
- Désactivé pour l'API REST JSON

---

## 7. Démarrage de l'application

### Développement

```bash
# Compiler
./mvnw clean compile

# Lancer les tests
./mvnw test

# Démarrer l'application
./mvnw spring-boot:run
```

L'application est accessible sur : http://localhost:8080

Console H2 : http://localhost:8080/h2-console
- JDBC URL : `jdbc:h2:mem:donsdb`
- Username : `sa`
- Password : (vide)

### Production

```bash
# Construire le JAR
./mvnw clean package -DskipTests

# Lancer avec profil production
java -jar target/Plateforme_de_dons-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 8. Démarche suivie

1. **Analyse des besoins** : Étude du cahier des charges et identification des fonctionnalités
2. **Modélisation** : Conception du modèle de données et des relations
3. **Implémentation couche données** : Entités JPA et repositories
4. **Implémentation couche service** : Logique métier
5. **Implémentation couche présentation** : Contrôleurs et templates
6. **Sécurisation** : Configuration Spring Security
7. **Tests** : Tests unitaires et d'intégration
8. **Documentation** : Rédaction de la documentation technique

