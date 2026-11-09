package application;

import gestion.Comptable;
import gestion.Conge;
import gestion.Developpeur;
import gestion.DirecteurGeneral;
import gestion.DonneesDemo;
import gestion.Employe;
import gestion.Entreprise;
import gestion.Niveau;
import gestion.Permission;
import gestion.ResponsableRH;
import gestion.Resultat;
import gestion.TypeConge;
import gestion.Validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Scanner;
import java.util.function.UnaryOperator;

/**
 * Programme principal : menu interactif en console.
 *
 * On se connecte en choisissant un employé. Le menu s'adapte à sa FONCTION :
 * les options réservées s'affichent [VERROUILLÉ] et refusent l'accès en expliquant pourquoi.
 *
 * RÈGLE DE SAISIE : toute saisie incorrecte affiche « Saisie incorrecte », explique ce qu'il
 * faut taper, puis REPOSE LA MÊME QUESTION (boucle) jusqu'à obtenir une valeur valide.
 * La saisie de 0 permet toujours de revenir au menu précédent.
 *
 * Main est dans le package "application", différent de "gestion" : il ne voit donc QUE les
 * méthodes public. Par exemple, la ligne suivante ne compilerait pas :
 *     employe.debiterSoldeConges(5);   // ERREUR : méthode invisible hors du package gestion
 */
public class Main {

    /**
     * true = CACHER complètement les options non autorisées au lieu de les verrouiller.
     * Désactivé par défaut ; pour l'activer, lancer avec : java -Dmasquer=true -cp out application.Main
     */
    private static final boolean MASQUER_OPTIONS_VERROUILLEES = Boolean.getBoolean("masquer");

    private static final Scanner CLAVIER = new Scanner(System.in);
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static Entreprise entreprise;

    public static void main(String[] args) {
        entreprise = DonneesDemo.creerEntreprise();
        boolean quitter = false;

        while (!quitter) {
            titre("GESTION DU PERSONNEL - " + entreprise.getNom().toUpperCase());
            System.out.println("   1. Se connecter en tant qu'employé");
            System.out.println("   2. Annuaire du personnel");
            System.out.println("   0. Quitter");
            int choix = lireEntier("Votre choix : ", 0, 2);

            switch (choix) {
                case 1:
                    Employe utilisateur = choisirEmploye("Qui êtes-vous ?");
                    if (utilisateur != null) {
                        menuEmploye(utilisateur);
                    }
                    break;
                case 2:
                    for (Employe employe : entreprise.getEmployes()) {
                        System.out.println("  " + employe);
                    }
                    pause();
                    break;
                default:
                    quitter = true;
            }
        }
        System.out.println("Au revoir !");
    }

    // =====================================================================
    //  MENU D'UN EMPLOYÉ CONNECTÉ
    // =====================================================================

    private static void menuEmploye(Employe moi) {
        boolean deconnexion = false;
        while (!deconnexion) {
            titre("ESPACE DE " + moi.getNomComplet() + " - " + moi.getFonction());
            System.out.println("  Solde de congés : " + moi.getSoldeConges() + " jour(s) ouvré(s)  |  Permission : "
                    + moi.getHeuresPermissionRestantes() + " h restantes");
            System.out.println();
            System.out.println("  --- Mon espace ---");
            System.out.println("   1. Mon profil");
            System.out.println("   2. Ma fiche de paie du mois");
            System.out.println("   3. Mes droits et mon aptitude aux congés");
            System.out.println("   4. Demander un congé" + (moi.estApte(TypeConge.PAYE) ? ""
                    : "   (congé payé INDISPONIBLE pour vous : voir option 3)"));
            System.out.println("   5. Mes demandes de congé (suivi / annulation)");
            System.out.println("   6. Demander une permission d'absence (quelques heures)");
            System.out.println("  --- Fonctions réservées ---");
            int aTraiter = entreprise.getDemandesATraiter(moi).size();
            option(moi, 7, "Traiter les demandes de congé" + (aTraiter > 0 ? " (" + aTraiter + " en attente)" : ""),
                    Permission.VALIDER_CONGES);
            option(moi, 8, "Consulter les congés de tout le personnel", Permission.CONSULTER_TOUS_LES_CONGES);
            option(moi, 9, "Consulter les salaires du personnel", Permission.CONSULTER_TOUS_LES_SALAIRES);
            option(moi, 10, "Statistiques et masse salariale", Permission.VOIR_STATISTIQUES);
            option(moi, 11, "Augmenter un salaire / promouvoir un développeur", Permission.MODIFIER_SALAIRES);
            option(moi, 12, "Embaucher un employé", Permission.EMBAUCHER);
            System.out.println("   0. Se déconnecter");

            int choix = lireEntier("Votre choix : ", 0, 12);
            System.out.println();
            switch (choix) {
                case 1:
                    moi.afficherProfil();
                    break;
                case 2:
                    moi.afficherFicheDePaie();
                    break;
                case 3:
                    moi.afficherDroitsEtAptitudes();
                    break;
                case 4:
                    demanderConge(moi);
                    break;
                case 5:
                    gererMesDemandes(moi);
                    break;
                case 6:
                    demanderPermission(moi);
                    break;
                case 7:
                    if (estAutorise(moi, Permission.VALIDER_CONGES)) {
                        traiterDemandes(moi);
                    }
                    break;
                case 8:
                    afficherSiRefus(entreprise.afficherTousLesConges(moi));
                    break;
                case 9:
                    afficherSiRefus(entreprise.afficherSalaires(moi));
                    break;
                case 10:
                    afficherSiRefus(entreprise.afficherStatistiques(moi));
                    break;
                case 11:
                    if (estAutorise(moi, Permission.MODIFIER_SALAIRES)) {
                        augmenterOuPromouvoir(moi);
                    }
                    break;
                case 12:
                    if (estAutorise(moi, Permission.EMBAUCHER)) {
                        embaucher(moi);
                    }
                    break;
                default:
                    deconnexion = true;
            }
            if (!deconnexion) {
                pause();
            }
        }
    }

    private static void option(Employe moi, int numero, String libelle, Permission permission) {
        boolean autorise = moi.aLaPermission(permission);
        if (!autorise && MASQUER_OPTIONS_VERROUILLEES) {
            return;
        }
        System.out.printf("  %2d. %s%s%n", numero, libelle, autorise ? "" : "   [VERROUILLÉ]");
    }

    /** Affiche le refus d'accès si l'employé n'a pas la permission. */
    private static boolean estAutorise(Employe moi, Permission permission) {
        Resultat acces = entreprise.verifierAcces(moi, permission);
        if (!acces.estUnSucces()) {
            acces.afficher();
        }
        return acces.estUnSucces();
    }

    private static void afficherSiRefus(Resultat resultat) {
        if (!resultat.estUnSucces()) {
            resultat.afficher();
        }
    }

    // =====================================================================
    //  PERMISSIONS D'ABSENCE
    // =====================================================================

    private static void demanderPermission(Employe moi) {
        while (true) {
            int max = Math.min(Employe.MAX_HEURES_PAR_PERMISSION, moi.getHeuresPermissionRestantes());
            if (max <= 0) {
                Resultat.echec("Vous avez utilisé tout votre quota d'heures de permission pour cette année.").afficher();
                return;
            }
            int heures = lireEntier("Nombre d'heures (1 à " + max + " ; 0 = annuler) : ", 0, max);
            if (heures == 0) {
                return;
            }
            String motif = lireTexteValide("Motif de l'absence : ", Validation::verifierMotif);
            Resultat resultat = moi.prendrePermission(heures, motif);
            resultat.afficher();
            if (resultat.estUnSucces()) {
                return;
            }
            System.out.println("  Recommencez votre demande (0 = annuler).");
        }
    }

    // =====================================================================
    //  CONGÉS
    // =====================================================================

    private static void demanderConge(Employe moi) {
        TypeConge[] types = TypeConge.values();
        while (true) {
            System.out.println("  Quel type de congé ?");
            for (int i = 0; i < types.length; i++) {
                List<String> raisons = moi.verifierAptitude(types[i]);
                String etat = raisons.isEmpty() ? types[i].getConditions() : "INDISPONIBLE pour vous";
                System.out.printf("   %d. %-19s %s%n", i + 1, types[i].getLibelle(), etat);
            }
            System.out.println("   0. Annuler");
            int choix = lireEntier("Votre choix : ", 0, types.length);
            if (choix == 0) {
                return;
            }
            TypeConge type = types[choix - 1];

            // Le programme dit tout de suite si l'employé est apte, avant même de demander les dates
            List<String> raisons = moi.verifierAptitude(type);
            if (!raisons.isEmpty()) {
                Resultat.echec("Vous n'êtes pas apte à prendre un " + type.getLibelle().toLowerCase() + " :", raisons).afficher();
                System.out.println("  Choisissez un autre type de congé (0 = annuler).");
                System.out.println();
                continue;
            }
            LocalDate debut = lireDate("Date de début (jj/mm/aaaa) : ");
            LocalDate fin;
            while (true) {
                fin = lireDate("Date de fin   (jj/mm/aaaa) : ");
                if (!fin.isBefore(debut)) {
                    break;
                }
                erreurSaisie("la date de fin doit être le " + Conge.formater(debut) + " ou après.");
            }
            String motif = lireTexteValide("Motif (facultatif, Entrée pour passer) : ", Validation::verifierMotifFacultatif);
            Resultat resultat = entreprise.demanderConge(moi, type, debut, fin, motif);
            resultat.afficher();
            if (resultat.estUnSucces()) {
                return;
            }
            System.out.println("  Demande non enregistrée : recommencez avec d'autres choix (0 = annuler).");
            System.out.println();
        }
    }

    private static void gererMesDemandes(Employe moi) {
        while (true) {
            List<Conge> mesConges = moi.getHistoriqueConges();
            if (mesConges.isEmpty()) {
                System.out.println("  Vous n'avez aucune demande de congé.");
                return;
            }
            for (Conge conge : mesConges) {
                System.out.println("  " + conge);
            }
            int numero = lireEntier("Numéro de la demande à annuler (0 = retour) : ", 0, Integer.MAX_VALUE);
            if (numero == 0) {
                return;
            }
            Conge conge = trouverConge(mesConges, numero);
            if (conge == null) {
                erreurSaisie("aucune de vos demandes ne porte le n°" + numero + ". Choisissez un numéro de la liste.");
                continue;
            }
            if (!lireOuiNon("Confirmer l'annulation de la demande n°" + numero + " ? (o/n) : ")) {
                continue;
            }
            Resultat resultat = entreprise.annulerDemande(moi, conge);
            resultat.afficher();
            if (resultat.estUnSucces()) {
                return;
            }
            System.out.println();
        }
    }

    private static void traiterDemandes(Employe moi) {
        while (true) {
            List<Conge> aTraiter = entreprise.getDemandesATraiter(moi);
            if (aTraiter.isEmpty()) {
                System.out.println("  Aucune demande en attente que vous puissiez traiter.");
                return;
            }
            for (Conge conge : aTraiter) {
                System.out.println("  " + conge);
            }
            int numero = lireEntier("Numéro de la demande à traiter (0 = retour) : ", 0, Integer.MAX_VALUE);
            if (numero == 0) {
                return;
            }
            Conge conge = trouverConge(aTraiter, numero);
            if (conge == null) {
                erreurSaisie("aucune demande en attente ne porte le n°" + numero + ". Choisissez un numéro de la liste.");
                continue;
            }
            Employe demandeur = conge.getDemandeur();
            System.out.println("  Demandeur : " + demandeur.getNomComplet() + " (" + demandeur.getFonction()
                    + ") - solde actuel : " + demandeur.getSoldeConges() + " jour(s) - motif : " + conge.getMotif());
            boolean accepter = lireOuiNon("Accepter cette demande ? (o/n) : ");
            String commentaire = accepter ? "" : lireTexteValide("Motif du refus : ", Validation::verifierMotif);
            entreprise.traiterDemande(moi, conge, accepter, commentaire).afficher();
            System.out.println();
        }
    }

    private static Conge trouverConge(List<Conge> liste, int numero) {
        for (Conge conge : liste) {
            if (conge.getId() == numero) {
                return conge;
            }
        }
        return null;
    }

    // =====================================================================
    //  CARRIÈRE
    // =====================================================================

    private static void augmenterOuPromouvoir(Employe moi) {
        while (true) {
            System.out.println("   1. Augmenter un salaire");
            System.out.println("   2. Promouvoir un développeur");
            System.out.println("   0. Retour");
            int choix = lireEntier("Votre choix : ", 0, 2);
            if (choix == 0) {
                return;
            }
            while (true) {
                Employe cible = choisirEmploye("Quel employé ?");
                if (cible == null) {
                    break;   // retour au menu juste avant (augmenter / promouvoir)
                }
                Resultat resultat;
                if (choix == 1) {
                    double pourcentage = lireNombre("Pourcentage d'augmentation (0,5 à 100) : ", 0.5, 100);
                    resultat = entreprise.augmenterSalaire(moi, cible, pourcentage);
                } else {
                    resultat = entreprise.promouvoir(moi, cible);
                }
                resultat.afficher();
                if (resultat.estUnSucces()) {
                    return;
                }
                System.out.println("  Choisissez un autre employé (0 = retour).");
            }
        }
    }

    private static void embaucher(Employe moi) {
        while (true) {
            System.out.println("  Quel poste ?");
            System.out.println("   1. Développeur");
            System.out.println("   2. Ressources Humaines");
            System.out.println("   3. Comptable");
            System.out.println("   4. Directeur Général");
            System.out.println("   0. Annuler");
            int poste = lireEntier("Votre choix : ", 0, 4);
            if (poste == 0) {
                return;
            }
            if (poste == 4 && entreprise.getDirecteurGeneral() != null) {
                Resultat.echec("L'entreprise a déjà un Directeur Général ("
                        + entreprise.getDirecteurGeneral().getNomComplet() + ").").afficher();
                System.out.println("  Choisissez un autre poste (0 = annuler).");
                continue;
            }
            String nom = lireTexteValide("Nom : ", texte -> Validation.verifierNom(texte, "Le nom"));
            String prenom = lireTexteValide("Prénom : ", texte -> Validation.verifierNom(texte, "Le prénom"));
            double salaire = lireNombre("Salaire de base mensuel (minimum " + Employe.formaterMontant(Employe.SALAIRE_MINIMUM)
                    + ") : ", Employe.SALAIRE_MINIMUM, 100_000_000);
            LocalDate aujourdhui = LocalDate.now();

            Employe nouveau;
            try {
                switch (poste) {
                    case 1:
                        Niveau niveau = Niveau.values()[lireEntier("Niveau (1 = Junior, 2 = Confirmé, 3 = Senior) : ", 1, 3) - 1];
                        String langage = lireTexteValide("Langage principal : ", Validation::verifierLangage);
                        nouveau = new Developpeur(nom, prenom, salaire, aujourdhui, niveau, langage);
                        break;
                    case 2:
                        nouveau = new ResponsableRH(nom, prenom, salaire, aujourdhui);
                        break;
                    case 3:
                        nouveau = new Comptable(nom, prenom, salaire, aujourdhui);
                        break;
                    default:
                        nouveau = new DirecteurGeneral(nom, prenom, salaire, aujourdhui);
                }
            } catch (IllegalArgumentException erreur) {
                Resultat.echec(erreur.getMessage()).afficher();
                System.out.println("  Recommencez l'embauche (0 = annuler).");
                continue;
            }
            entreprise.embaucher(moi, nouveau).afficher();
            return;
        }
    }

    // =====================================================================
    //  SAISIES AU CLAVIER (toujours vérifiées, redemandées tant qu'elles sont fausses)
    // =====================================================================

    /** Message unique pour toutes les erreurs de saisie. */
    private static void erreurSaisie(String quoiTaper) {
        System.out.println("  >> Saisie incorrecte : " + quoiTaper);
    }

    private static Employe choisirEmploye(String question) {
        List<Employe> employes = entreprise.getEmployes();
        System.out.println("  " + question);
        for (int i = 0; i < employes.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, employes.get(i));
        }
        System.out.println("   0. Retour");
        int choix = lireEntier("Votre choix : ", 0, employes.size());
        return choix == 0 ? null : employes.get(choix - 1);
    }

    private static String lireLigne(String invite) {
        System.out.print("  " + invite);
        if (!CLAVIER.hasNextLine()) {
            System.out.println();
            System.out.println("Fin de la saisie. Au revoir !");
            System.exit(0);
        }
        return CLAVIER.nextLine().replace("﻿", "").trim();   // ignore un éventuel caractère invisible (BOM)
    }

    private static int lireEntier(String invite, int min, int max) {
        String plage = (max == Integer.MAX_VALUE) ? "un nombre entier positif" : "un nombre entier entre " + min + " et " + max;
        while (true) {
            String saisie = lireLigne(invite);
            try {
                int valeur = Integer.parseInt(saisie);
                if (valeur >= min && valeur <= max) {
                    return valeur;
                }
            } catch (NumberFormatException erreur) {
                // traité juste en dessous, comme une valeur hors limites
            }
            erreurSaisie("tapez " + plage + ".");
        }
    }

    private static double lireNombre(String invite, double min, double max) {
        while (true) {
            String saisie = lireLigne(invite).replace(" ", "").replace(" ", "").replace(',', '.');
            try {
                double valeur = Double.parseDouble(saisie);
                if (valeur >= min && valeur <= max && !Double.isNaN(valeur)) {
                    return valeur;
                }
            } catch (NumberFormatException erreur) {
                // traité juste en dessous
            }
            erreurSaisie("tapez un nombre entre " + nombreLisible(min) + " et " + nombreLisible(max) + ".");
        }
    }

    /** 75000.0 -> "75 000" ; 0.5 -> "0,5" (plus lisible que l'affichage brut d'un double). */
    private static String nombreLisible(double valeur) {
        if (valeur == Math.rint(valeur)) {
            return String.format(java.util.Locale.US, "%,.0f", valeur).replace(',', ' ');
        }
        return String.valueOf(valeur).replace('.', ',');
    }

    private static LocalDate lireDate(String invite) {
        int anneeCourante = LocalDate.now().getYear();
        while (true) {
            try {
                LocalDate date = LocalDate.parse(lireLigne(invite), FORMAT_DATE);
                if (date.getYear() >= anneeCourante - 1 && date.getYear() <= anneeCourante + 2) {
                    return date;
                }
                erreurSaisie("l'année doit être comprise entre " + (anneeCourante - 1) + " et " + (anneeCourante + 2) + ".");
            } catch (DateTimeParseException erreur) {
                erreurSaisie("tapez une date valide au format jj/mm/aaaa (exemple : 15/01/" + (anneeCourante + 1) + ").");
            }
        }
    }

    /**
     * Lit un texte et le redemande tant que la règle de validation renvoie une erreur.
     * @param regle renvoie null si le texte est correct, sinon le message d'erreur
     */
    private static String lireTexteValide(String invite, UnaryOperator<String> regle) {
        while (true) {
            String saisie = lireLigne(invite);
            String erreur = regle.apply(saisie);
            if (erreur == null) {
                return saisie;
            }
            if (erreur.startsWith("RÉPONSE INVALIDE")) {
                System.out.println("  >> " + erreur);   // message ferme des motifs, affiché tel quel
            } else {
                erreurSaisie(erreur);
            }
        }
    }

    private static boolean lireOuiNon(String invite) {
        while (true) {
            String saisie = lireLigne(invite).toLowerCase();
            if (saisie.equals("o") || saisie.equals("oui")) {
                return true;
            }
            if (saisie.equals("n") || saisie.equals("non")) {
                return false;
            }
            erreurSaisie("répondez par o (oui) ou n (non).");
        }
    }

    private static void pause() {
        System.out.println();
        lireLigne("Appuyez sur Entrée pour continuer...");
    }

    private static void titre(String texte) {
        String ligne = "=".repeat(Math.max(60, texte.length() + 4));
        System.out.println();
        System.out.println(ligne);
        System.out.println("  " + texte);
        System.out.println(ligne);
    }
}
