# Gestion du personnel : salaires et congés (Java)

Programme console qui gère les employés d'une entreprise informatique :
**Développeurs, RH, Comptable et Directeur Général**.
Il calcule le salaire de chacun selon sa fonction, gère les congés avec un circuit de
validation, et indique toujours **si l'employé est apte** à prendre un congé ou à
utiliser une fonction, et **pourquoi**.

## Lancer le programme

**Eclipse / IntelliJ / NetBeans** : importer le dossier `src`, puis lancer :
- `application.Main` pour le menu interactif (on se connecte en choisissant un employé) ;
- `application.DemoAutomatique` pour une démonstration complète sans saisie.

**En ligne de commande** (Java 11 ou plus) :

```bash
javac -encoding UTF-8 -d out src/gestion/*.java src/application/*.java
```

```bash
java -cp out application.Main
```

```bash
java -cp out application.DemoAutomatique
```

## Organisation

```
src/
├── gestion/                  (le "métier")
│   ├── Employe.java          classe MÈRE abstraite : identité, congés, salaire, droits
│   ├── Developpeur.java      extends Employe : niveau, heures sup., projets, gel avant livraison
│   ├── ResponsableRH.java    extends Employe : valide les congés, embauche
│   ├── Comptable.java        extends Employe : voit les salaires, pas de congé en décembre
│   ├── DirecteurGeneral.java extends Employe : tous les droits, intérimaire obligatoire
│   ├── Entreprise.java       contrôle d'accès, circuit de validation, statistiques
│   ├── Conge.java            une demande de congé
│   ├── TypeConge / StatutConge / Permission / Niveau   (enums)
│   ├── Resultat.java         réponse "OK / REFUSÉ + raisons" de chaque action
│   └── DonneesDemo.java      entreprise d'exemple
└── application/              (l'interface console)
    ├── Main.java             menu interactif
    └── DemoAutomatique.java  scénario de démonstration
```

## Notions de cours utilisées

| Notion | Où la voir |
|---|---|
| **Héritage** (`extends`, `super(...)`) | Les 4 métiers héritent de `Employe` |
| **Classe abstraite** | `Employe` : on ne peut pas faire `new Employe()` |
| **Redéfinition / polymorphisme** (`@Override`) | `ajouterPrimes`, `aLaPermission`, `verifierReglesMetier` |
| **Surcharge** | Les 4 constructeurs d'`Employe`, les 2 `prendrePermission` |
| **`private`** | `soldeConges`, `matricule` : modifiables seulement par des méthodes contrôlées |
| **`protected`** | `salaireBase` : utilisé par les classes filles pour leurs primes |
| **visibilité package** (aucun mot-clé) | `debiterSoldeConges`, `Conge.accepter` : seule `Entreprise` peut les appeler |
| **`public`** | Tout ce que `Main` (autre package) a le droit d'utiliser |
| **`static` / `final`** | Compteur de matricules, constantes, `calculerSalaireBrut` non redéfinissable |
| **Conditions** `if / else if / switch` | Impôt par tranches, heures sup., droits, éligibilité |
| **`instanceof` + cast** | Promotion (développeurs seulement), prime de recrutement RH |
| **Enum** | Types de congé, statuts, permissions, niveaux |
| **Exceptions** | Salaire sous le minimum, nom vide, saisies clavier invalides |

## Calcul du salaire

```
brut = salaire de base + prime d'ancienneté + primes du métier - retenue congé sans solde
net  = brut - cotisation sociale (6,3 %) - impôt (barème progressif simplifié)
```

- **Ancienneté** : 1 % par année à partir de 2 ans, plafonné à 25 %.
- **Développeur** : prime de niveau (0 / 50 000 / 100 000), heures sup. majorées
  (+25 % les 8 premières, +50 % ensuite), 50 000 par projet livré.
- **RH** : prime de fonction 40 000 + 25 000 par recrutement.
- **Comptable** : prime de fonction 30 000 + prime de bilan 100 000 quand le bilan est validé.
- **DG** : prime de représentation 200 000 + intéressement (0,5 % du bénéfice / 12).

Les taux et montants sont **simplifiés et pédagogiques** : ils sont tous regroupés en
constantes en haut des classes pour être faciles à modifier.

## Règles des congés (vérifiées automatiquement)

| Règle | Concerne |
|---|---|
| Pas de congé payé pendant la période d'essai (3 mois) | Tous |
| Solde suffisant (25 jours ouvrés/an, +1 tous les 5 ans d'ancienneté) | Congé payé |
| Préavis de 7 jours | Congé payé, sans solde |
| Pas dans le passé ; maladie déclarée sous 3 jours | Tous |
| Pas de chevauchement avec une autre demande | Tous |
| Exceptionnel ≤ 4 jours, sans solde ≤ 20 jours | Selon le type |
| Gel des congés autour d'une mise en production | Développeur |
| Pas de congé en décembre (clôture des comptes) | Comptable |
| Intérimaire désigné et présent | Directeur Général |
| La moitié de l'équipe d'un même métier doit rester présente | Tous (équipes de 2 et plus) |

**Circuit de validation** : la RH valide les congés des développeurs et du comptable ;
le **DG** valide ceux de la RH (personne ne valide ses propres congés) ; ceux du DG sont
acceptés automatiquement. Un refus doit toujours être motivé, et une demande acceptée
peut être annulée avant son début (les jours sont rendus).

## Idées pour aller plus loin

- Sauvegarder les employés et les congés dans un fichier (CSV) ou une base de données.
- Ajouter des jours fériés dans le calcul des jours ouvrés.
- Acquérir les congés mois par mois au lieu d'un droit annuel.
- Ajouter un mot de passe à la connexion.
- Exporter la fiche de paie en PDF.
- Écrire des tests JUnit pour le calcul des salaires et l'éligibilité aux congés.
