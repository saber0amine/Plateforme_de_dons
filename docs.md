C'est un excellent projet pour valider ton M2 ISIMA. Compte tenu de ton profil **Avancé** et de ton expertise **DevOps/Spring**, je te propose une approche "Vertical Slicing" (découpage par fonctionnalité de bout en bout).

Cela évite le piège "Front vs Back" (qui crée des goulots d'étranglement) et permet à chacun de toucher à tout (Controller, Service, Repository, Thymeleaf/HTML).

Voici une proposition de répartition équilibrée, pensée pour minimiser les conflits de fusion (Merge Conflicts) et maximiser l'efficacité.

---

### 1. Stratégie Git & Workflow Technique
Comme vous êtes deux, inutile de faire une usine à gaz, mais restez pro (surtout pour le rapport).

* **Branching Model :** *Feature Branch Workflow*.
    * `main` : Code de production, toujours stable.
    * `feat/nom-fonctionnalite` : Branche de travail pour chaque ticket.
* **Revue de code (Pull Requests) :** Obligatoire. Dev A valide le code de Dev B et vice-versa.
* **CI/CD (Ton expertise DevOps) :**
    * [cite_start]Mets en place un petit pipeline (GitHub Actions ou GitLab CI) dès le début pour lancer les tests unitaires et le script de build [cite: 22] à chaque push.
    * Cela garantit que le projet reste "compilable/exécutable" en permanence.

---

### 2. Répartition des Fonctionnalités (Vertical Slicing)

Je découpe le projet en deux "Domaines" distincts.

#### 👨‍💻 Développeur A : "Core Domain" (Objets & API)
*Ce rôle se concentre sur le cœur du métier : les données des annonces et l'architecture REST avancée demandée.*

1.  **Gestion des Annonces (CRUD) :**
    * [cite_start]Création de l'entité `Annonce` (Titre, description, état, date, zone, livraison, mots-clés)[cite: 7].
    * Formulaire de dépôt d'annonce (Thymeleaf) et Endpoint API.
2.  **Moteur de Recherche & Filtrage :**
    * [cite_start]Implémentation des filtres complexes (Zone, mots-clés, état)[cite: 8].
    * *Challenge technique :* Utiliser `JPA Specification` ou `QueryDSL` pour un filtrage dynamique propre.
3.  **Architecture HTTP & Scalabilité (Le point technique critique) :**
    * [cite_start]Gestion des en-têtes de cache (`ETag`, `Last-Modified`) pour gérer les requêtes conditionnelles[cite: 16].
    * [cite_start]Mise en place de la **Négociation de contenu** (HTML vs JSON) pour supporter les futurs clients mobiles/SPA[cite: 19]. C'est crucial pour la note technique.

#### 👨‍💻 Développeur B : "User Experience & Social"
*Ce rôle se concentre sur l'interaction utilisateur, la persistance avancée et les flux asynchrones.*

1.  **Utilisateurs & Authentification :**
    * Gestion des comptes (Inscription/Login) - *Tu peux utiliser Spring Security standard*.
    * Sécurisation des endpoints (seul le propriétaire modifie son annonce).
2.  **Alerting & Sauvegardes :**
    * [cite_start]Système de "Favoris"[cite: 10].
    * [cite_start]Sauvegarde des recherches et **Notifications**[cite: 9].
    * *Challenge technique :* Implémenter un système d'Event (Spring Events) pour déclencher une notification quand une nouvelle annonce matche une recherche sauvegardée.
3.  **Interactions & Transactions :**
    * [cite_start]Création de "Lots" d'objets[cite: 10].
    * [cite_start]Messagerie interne simple entre donneur et receveur[cite: 11].

---

### 3. Socle Commun (À faire ensemble au démarrage - 2h max)
Avant de vous séparer, configurez le squelette ensemble (Pair Programming) :

1.  **Initialisation Spring Boot :** Dépendances Web, JPA, H2, Thymeleaf, Lombok, Validation.
2.  **Configuration BDD :**
    * [cite_start]Profil `dev` : H2 (embarquée)[cite: 14].
    * [cite_start]Profil `prod` : Configuration pour PostgreSQL/MySQL (externe) via `application-prod.properties`[cite: 15].
3.  [cite_start]**Design API (URL Scheme) :** Accordez-vous sur le format des URL (ex: `/api/v1/annonces` vs `/annonces`) car cela doit figurer dans le rapport PDF[cite: 23].

---

### 4. Résumé Visuel pour votre Tableau (Trello/Jira/GitHub Projects)

| Feature | Responsable | Complexité Technique |
| :--- | :--- | :--- |
| **Setup Projet & CI** | **Ensemble** (Lead par toi) | ⭐ |
| **Modèle Annonce & CRUD** | Dev A | ⭐⭐ |
| **Recherche & Filtres** | Dev A | ⭐⭐⭐ |
| **Cache HTTP & Content Neg.** | Dev A | [cite_start]⭐⭐⭐⭐ (Point clé du sujet [cite: 16, 19]) |
| **Auth & Profils** | Dev B | ⭐⭐ |
| **Favoris & Lots** | Dev B | ⭐⭐ |
| **Messagerie Interne** | Dev B | ⭐⭐⭐ |
| **Recherche Sauvegardée + Notif** | Dev B | [cite_start]⭐⭐⭐⭐ (Logique métier complexe [cite: 9]) |

**Pourquoi ce découpage est idéal pour toi (M2 ISIMA) ?**
* **Dev A** travaille sur les aspects "Architecture Web pure" (REST, Cache, HATEOAS), ce qui correspond aux exigences techniques pointues du sujet.
* **Dev B** travaille sur la logique métier et les données relationnelles, assurant que l'application est fonctionnelle et riche.

**Prochaine étape possible :**
Veux-tu que je te génère le fichier `docker-compose.yml` pour simuler l'environnement de prod (BDD externe) ou une structure de classe Java pour gérer proprement la négociation de contenu (Vue HTML vs JSON) ?