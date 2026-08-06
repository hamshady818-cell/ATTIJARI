# 🖥️ Application Frontend GED-AWB

Ce répertoire contient l'application web monopage (*Single Page Application* - SPA) pour **GED-AWB** (*Gestion Électronique des Documents - Attijariwafa Bank*).

## 🚀 Technologies Utilisées

* **Framework** : [React 19](https://react.dev/) + [TypeScript 5](https://www.typescriptlang.org/)
* **Outil de Build** : [Vite 8](https://vitejs.dev/)
* **Styles** : [Tailwind CSS v3](https://tailwindcss.com/)
* **Routage** : [React Router v7](https://reactrouter.com/)
* **Gestion d'État & Requêtes API** : [TanStack Query v5 (React Query)](https://tanstack.com/query/latest)
* **Formulaires & Validation** : [React Hook Form](https://react-hook-form.com/) + [Zod](https://zod.dev/)
* **Client HTTP** : [Axios](https://axios-http.com/)
* **Icônes** : [Lucide React](https://lucide.dev/)
* **Linter** : [Oxlint](https://oxc.rs/docs/guide/usage/linter.html)

---

## 🛠️ Commandes Disponibles

Dans le dossier `frontend`, vous pouvez exécuter les commandes suivantes :

### `npm run dev`
Lance l'application en mode développement sur [http://localhost:5173](http://localhost:5173).

### `npm run build`
Exécute la vérification des types TypeScript (`tsc -b`) et compile l'application pour la production dans le dossier `dist`.

### `npm run lint`
Lance l'analyse du code avec Oxlint.

### `npm run preview`
Affiche un aperçu local du build de production.

---

## 📁 Principales Fonctionnalités Incluses

* **Explorateur de Documents** : Grille et tableau d'affichage dynamiques avec fil d'Ariane, tri et filtrage en temps réel.
* **Actions sur les Documents** : Modal de téléversement par glisser-déposer, modal de création de dossier, barre d'outils pour la sélection groupée.
* **Panneau de Détails** : Volet latéral de prévisualisation avec affichage des métadonnées dynamiques, des permissions et panneau de chat IA.
* **Corbeille & Récupération** : Interface pour visualiser les éléments supprimés avec possibilité de restauration ou de suppression définitive.
* **Tableau de Bord (Dashboard)** : Statistiques globales sur les documents stockés, les catégories, l'espace disque consommé et les activités récentes.

---

## 📖 Documentation Principale

Pour la documentation complète sur l'architecture globale, la configuration du backend et les variables d'environnement, veuillez vous référer au fichier [README.md](file:///c:/Users/HP/Documents/GED-ATTIJARI/ATTIJARI/README.md) situé à la racine du projet.
