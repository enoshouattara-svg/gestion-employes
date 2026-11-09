package gestion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L'entreprise : elle regroupe les employés et toutes les demandes de congé.
 *
 * C'est elle qui applique le CONTRÔLE D'ACCÈS : avant chaque action sensible, elle vérifie
 * que l'utilisateur possède la permission nécessaire. Toutes les actions renvoient un
 * Resultat qui explique la décision (accepté ou refusé, et pourquoi).
 */
public class Entreprise {

    public static final double AUGMENTATION_MAX_POURCENT = 20;

    private final String nom;
    private final List<Employe> employes = new ArrayList<>();
    private final List<Conge> demandesDeConge = new ArrayList<>();

    public Entreprise(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    public List<Employe> getEmployes() {
        return Collections.unmodifiableList(employes);
    }

    /** Visibilité package : utilisée pour charger les données de départ, sans contrôle. */
    void ajouterEmploye(Employe employe) {
        employes.add(employe);
    }

    public Employe trouverParNom(String nomRecherche) {
        for (Employe employe : employes) {
            if (employe.getNom().equalsIgnoreCase(nomRecherche)) {
                return employe;
            }
        }
        return null;
    }

    public DirecteurGeneral getDirecteurGeneral() {
        for (Employe employe : employes) {
            if (employe instanceof DirecteurGeneral) {
                return (DirecteurGeneral) employe;
            }
        }
        return null;
    }

    // =====================================================================
    //  CONTRÔLE D'ACCÈS
    // =====================================================================

    /** Répond à "Ai-je le droit d'utiliser cette fonction ?" et explique qui y a droit. */
    public Resultat verifierAcces(Employe utilisateur, Permission permission) {
        if (utilisateur.aLaPermission(permission)) {
            return Resultat.succes("Accès autorisé : " + permission.getLibelle() + ".");
        }
        return Resultat.echec("ACCÈS REFUSÉ à la fonction « " + permission.getLibelle() + " ».",
                "Vous êtes : " + utilisateur.getMetier() + ".",
                "Fonction réservée à : " + String.join(", ", metiersAutorises(permission)) + ".");
    }

    private Set<String> metiersAutorises(Permission permission) {
        Set<String> metiers = new LinkedHashSet<>();
        for (Employe employe : employes) {
            if (employe.aLaPermission(permission)) {
                metiers.add(employe.getMetier());
            }
        }
        return metiers;
    }

    // =====================================================================
    //  CONGÉS
    // =====================================================================

    public Resultat demanderConge(Employe demandeur, TypeConge type, LocalDate debut, LocalDate fin, String motif) {
        if (!demandeur.aLaPermission(Permission.DEMANDER_CONGE)) {
            return verifierAcces(demandeur, Permission.DEMANDER_CONGE);
        }

        // 1) L'employé est-il apte ? (règles générales + règles de son métier)
        List<String> raisons = demandeur.verifierEligibilite(type, debut, fin);
        String erreurMotif = Validation.verifierMotifFacultatif(motif);
        if (erreurMotif != null) {
            raisons.add("Motif invalide : " + erreurMotif);
        }

        // 2) Le service peut-il se passer de lui ? (règle d'équipe, gérée par l'entreprise)
        boolean datesValides = type != null && debut != null && fin != null && !fin.isBefore(debut);
        if (datesValides && type.isPlanifiable()) {
            raisons.addAll(verifierEffectif(demandeur, debut, fin));
        }

        if (!raisons.isEmpty()) {
            return Resultat.echec(demandeur.getNomComplet() + " n'est pas apte à prendre ce congé :", raisons);
        }

        Conge conge = new Conge(demandeur, type, debut, fin, motif);
        demandeur.ajouterConge(conge);
        demandesDeConge.add(conge);
        String resume = type.getLibelle() + " " + conge.getPeriode() + " (" + conge.getJoursOuvres() + " jour(s) ouvré(s))";

        if (demandeur.estValidationAutomatique()) {
            accepter(conge, demandeur);
            return Resultat.succes(conge, "Congé n°" + conge.getId() + " accepté automatiquement : " + resume + ".",
                    "Le service RH est informé de l'absence du Directeur Général.");
        }
        return Resultat.succes(conge, "Demande n°" + conge.getId() + " enregistrée : " + resume + ".",
                "En attente de validation par " + demandeur.getValidateurDesConges() + ".");
    }

    public Resultat traiterDemande(Employe validateur, Conge conge, boolean accepter, String commentaire) {
        if (!validateur.aLaPermission(Permission.VALIDER_CONGES)) {
            return verifierAcces(validateur, Permission.VALIDER_CONGES);
        }
        if (conge.getStatut() != StatutConge.EN_ATTENTE) {
            return Resultat.echec("La demande n°" + conge.getId() + " a déjà été traitée ("
                    + conge.getStatut().getLibelle().toLowerCase() + ").");
        }
        Employe demandeur = conge.getDemandeur();
        if (demandeur == validateur) {
            return Resultat.echec("Conflit d'intérêts : vous ne pouvez pas valider votre propre congé.",
                    "Il doit être validé par " + demandeur.getValidateurDesConges() + ".");
        }
        if (!demandeur.peutEtreValidePar(validateur)) {
            return Resultat.echec("Vous ne pouvez pas traiter cette demande.",
                    "Les congés de " + demandeur.getNomComplet() + " sont validés par "
                            + demandeur.getValidateurDesConges() + ".");
        }

        // --- Refus : il doit être motivé ---
        if (!accepter) {
            if (commentaire == null || commentaire.trim().isEmpty()) {
                return Resultat.echec("Un refus doit obligatoirement être motivé.");
            }
            String erreurMotif = Validation.verifierMotif(commentaire);
            if (erreurMotif != null) {
                return Resultat.echec("Motif du refus invalide : " + erreurMotif);
            }
            conge.refuser(validateur, commentaire.trim());
            return Resultat.succes(conge, "Demande n°" + conge.getId() + " de " + demandeur.getNomComplet()
                    + " refusée par " + validateur.getNomComplet() + ".", "Motif : " + commentaire.trim());
        }

        // --- Acceptation : on revérifie, la situation a pu changer depuis la demande ---
        List<String> raisons = new ArrayList<>();
        if (conge.getType().isDecompteDuSolde() && conge.getJoursOuvres() > demandeur.getSoldeConges()) {
            raisons.add("Solde devenu insuffisant (" + demandeur.getSoldeConges() + " jour(s) disponible(s)).");
        }
        if (conge.getType().isPlanifiable()) {
            raisons.addAll(verifierEffectif(demandeur, conge.getDebut(), conge.getFin()));
        }
        if (!raisons.isEmpty()) {
            conge.refuser(validateur, "refus automatique : " + raisons.get(0));
            return Resultat.echec("La demande n°" + conge.getId() + " ne peut plus être acceptée : elle est refusée.", raisons);
        }
        accepter(conge, validateur);
        return Resultat.succes(conge, "Demande n°" + conge.getId() + " de " + demandeur.getNomComplet()
                        + " acceptée par " + validateur.getNomComplet() + ".",
                "Solde restant de " + demandeur.getNomComplet() + " : " + demandeur.getSoldeConges() + " jour(s).");
    }

    private void accepter(Conge conge, Employe validateur) {
        conge.accepter(validateur);
        if (conge.getType().isDecompteDuSolde()) {
            conge.getDemandeur().debiterSoldeConges(conge.getJoursOuvres());
        }
    }

    public Resultat annulerDemande(Employe auteur, Conge conge) {
        if (conge.getDemandeur() != auteur) {
            return Resultat.echec("Vous ne pouvez annuler que vos propres demandes.");
        }
        switch (conge.getStatut()) {
            case REFUSE:
            case ANNULE:
                return Resultat.echec("La demande n°" + conge.getId() + " est déjà "
                        + conge.getStatut().getLibelle().toLowerCase() + "e : rien à annuler.");
            case ACCEPTE:
                if (!conge.getDebut().isAfter(LocalDate.now())) {
                    return Resultat.echec("Le congé n°" + conge.getId() + " a déjà commencé : il ne peut plus être annulé.");
                }
                if (conge.getType().isDecompteDuSolde()) {
                    auteur.crediterSoldeConges(conge.getJoursOuvres());   // on rend les jours
                }
                break;
            default:
                break;
        }
        conge.annuler();
        return Resultat.succes(conge, "Demande n°" + conge.getId() + " annulée.",
                "Solde de congés : " + auteur.getSoldeConges() + " jour(s).");
    }

    /**
     * Règle d'équipe : au moins la moitié des employés d'un même métier doit rester présente.
     * (Ne s'applique que s'il y a au moins 2 personnes dans le métier.)
     */
    List<String> verifierEffectif(Employe demandeur, LocalDate debut, LocalDate fin) {
        List<String> raisons = new ArrayList<>();
        int effectif = 0;
        List<String> absents = new ArrayList<>();
        for (Employe collegue : employes) {
            if (collegue.getClass() == demandeur.getClass()) {   // même métier = même classe
                effectif++;
                if (collegue != demandeur && collegue.estAbsentPendant(debut, fin)) {
                    absents.add(collegue.getNomComplet());
                }
            }
        }
        int absencesMax = effectif / 2;
        if (effectif >= 2 && absents.size() + 1 > absencesMax) {
            raisons.add("Effectif insuffisant : " + absents.size() + " " + demandeur.getMetier() + "(s) sur "
                    + effectif + " déjà absent(s) sur cette période (" + String.join(", ", absents)
                    + "). La moitié de l'équipe doit rester présente.");
        }
        return raisons;
    }

    /** Demandes en attente que CE validateur a le droit de traiter. */
    public List<Conge> getDemandesATraiter(Employe validateur) {
        List<Conge> resultat = new ArrayList<>();
        if (!validateur.aLaPermission(Permission.VALIDER_CONGES)) {
            return resultat;
        }
        for (Conge conge : demandesDeConge) {
            if (conge.getStatut() == StatutConge.EN_ATTENTE && conge.getDemandeur().peutEtreValidePar(validateur)) {
                resultat.add(conge);
            }
        }
        return resultat;
    }

    /** Visibilité package : enregistre un congé déjà passé (données de départ). */
    void enregistrerCongePasse(Employe employe, TypeConge type, LocalDate debut, int joursOuvres, Employe validateur) {
        Conge conge = new Conge(employe, type, debut, Conge.finApresJoursOuvres(debut, joursOuvres), "Historique");
        employe.ajouterConge(conge);
        demandesDeConge.add(conge);
        accepter(conge, validateur);
    }

    // =====================================================================
    //  CONSULTATIONS RÉSERVÉES
    // =====================================================================

    public Resultat afficherTousLesConges(Employe lecteur) {
        if (!lecteur.aLaPermission(Permission.CONSULTER_TOUS_LES_CONGES)) {
            return verifierAcces(lecteur, Permission.CONSULTER_TOUS_LES_CONGES);
        }
        if (demandesDeConge.isEmpty()) {
            System.out.println("  Aucune demande de congé.");
        }
        for (Conge conge : demandesDeConge) {
            System.out.println("  " + conge);
        }
        return Resultat.succes(demandesDeConge.size() + " demande(s) affichée(s).");
    }

    public Resultat afficherSalaires(Employe lecteur) {
        if (!lecteur.aLaPermission(Permission.CONSULTER_TOUS_LES_SALAIRES)) {
            return verifierAcces(lecteur, Permission.CONSULTER_TOUS_LES_SALAIRES);
        }
        String format = "  %-9s %-20s %-21s %15s %15s%n";
        System.out.printf(format, "MATRICULE", "NOM", "FONCTION", "BRUT", "NET");
        double totalBrut = 0;
        double totalNet = 0;
        for (Employe employe : employes) {
            double brut = employe.calculerSalaireBrut();
            double net = employe.calculerSalaireNet();
            totalBrut += brut;
            totalNet += net;
            System.out.printf(format, employe.getMatricule(), employe.getNomComplet(), employe.getFonction(),
                    Employe.formaterMontant(brut), Employe.formaterMontant(net));
        }
        System.out.printf(format, "", "TOTAL", "", Employe.formaterMontant(totalBrut), Employe.formaterMontant(totalNet));
        return Resultat.succes("Salaires de " + employes.size() + " employé(s) affichés.");
    }

    public Resultat afficherStatistiques(Employe lecteur) {
        if (!lecteur.aLaPermission(Permission.VOIR_STATISTIQUES)) {
            return verifierAcces(lecteur, Permission.VOIR_STATISTIQUES);
        }
        if (employes.isEmpty()) {
            return Resultat.echec("Aucun employé dans l'entreprise.");
        }
        Map<String, Integer> effectifParMetier = new LinkedHashMap<>();
        Map<String, Double> netParMetier = new LinkedHashMap<>();
        double masseBrute = 0;
        double masseNette = 0;
        Employe mieuxPaye = null;
        Employe moinsPaye = null;
        int joursRestants = 0;
        List<String> absents = new ArrayList<>();

        for (Employe employe : employes) {
            double net = employe.calculerSalaireNet();
            masseBrute += employe.calculerSalaireBrut();
            masseNette += net;
            String metier = employe.getMetier();
            effectifParMetier.put(metier, effectifParMetier.getOrDefault(metier, 0) + 1);
            netParMetier.put(metier, netParMetier.getOrDefault(metier, 0.0) + net);
            if (mieuxPaye == null || net > mieuxPaye.calculerSalaireNet()) {
                mieuxPaye = employe;
            }
            if (moinsPaye == null || net < moinsPaye.calculerSalaireNet()) {
                moinsPaye = employe;
            }
            joursRestants += employe.getSoldeConges();
            if (employe.estAbsentAujourdhui()) {
                absents.add(employe.getNomComplet());
            }
        }
        int enAttente = 0;
        for (Conge conge : demandesDeConge) {
            if (conge.getStatut() == StatutConge.EN_ATTENTE) {
                enAttente++;
            }
        }

        System.out.println("  Effectif total                : " + employes.size());
        for (Map.Entry<String, Integer> ligne : effectifParMetier.entrySet()) {
            double moyenne = netParMetier.get(ligne.getKey()) / ligne.getValue();
            System.out.printf("    - %-26s: %d  (net moyen : %s)%n", ligne.getKey(), ligne.getValue(),
                    Employe.formaterMontant(moyenne));
        }
        System.out.println("  Masse salariale brute         : " + Employe.formaterMontant(masseBrute) + " / mois ("
                + Employe.formaterMontant(masseBrute * 12) + " / an)");
        System.out.println("  Masse salariale nette         : " + Employe.formaterMontant(masseNette) + " / mois");
        System.out.println("  Salaire net moyen             : " + Employe.formaterMontant(masseNette / employes.size()));
        System.out.println("  Salaire net le plus élevé     : " + mieuxPaye.getNomComplet() + " ("
                + Employe.formaterMontant(mieuxPaye.calculerSalaireNet()) + ")");
        System.out.println("  Salaire net le plus bas       : " + moinsPaye.getNomComplet() + " ("
                + Employe.formaterMontant(moinsPaye.calculerSalaireNet()) + ")");
        System.out.printf("  Écart le plus haut / plus bas : %.1f fois%n",
                mieuxPaye.calculerSalaireNet() / moinsPaye.calculerSalaireNet());
        System.out.println("  Demandes de congé en attente  : " + enAttente);
        System.out.println("  Absents aujourd'hui           : " + (absents.isEmpty() ? "aucun" : String.join(", ", absents)));
        System.out.println("  Jours de congé restants       : " + joursRestants + " (tout le personnel)");
        return Resultat.succes("Statistiques affichées.");
    }

    // =====================================================================
    //  CARRIÈRE : AUGMENTATION, PROMOTION, EMBAUCHE
    // =====================================================================

    public Resultat augmenterSalaire(Employe auteur, Employe cible, double pourcentage) {
        if (!auteur.aLaPermission(Permission.MODIFIER_SALAIRES)) {
            return verifierAcces(auteur, Permission.MODIFIER_SALAIRES);
        }
        if (cible == auteur) {
            return Resultat.echec("Vous ne pouvez pas augmenter votre propre salaire.",
                    "Cette décision revient au conseil d'administration.");
        }
        if (pourcentage <= 0) {
            return Resultat.echec("Le pourcentage d'augmentation doit être supérieur à 0.");
        }
        if (pourcentage > AUGMENTATION_MAX_POURCENT) {
            return Resultat.echec("Augmentation trop élevée : " + formaterPourcent(AUGMENTATION_MAX_POURCENT)
                    + " maximum par décision.");
        }
        double ancienBase = cible.getSalaireBase();
        double ancienNet = cible.calculerSalaireNet();
        cible.appliquerAugmentation(pourcentage);
        return Resultat.succes("Salaire de base de " + cible.getNomComplet() + " : " + Employe.formaterMontant(ancienBase)
                        + " -> " + Employe.formaterMontant(cible.getSalaireBase()) + " (+" + formaterPourcent(pourcentage) + ").",
                "Net mensuel : " + Employe.formaterMontant(ancienNet) + " -> " + Employe.formaterMontant(cible.calculerSalaireNet()) + ".");
    }

    public Resultat promouvoir(Employe auteur, Employe cible) {
        if (!auteur.aLaPermission(Permission.MODIFIER_SALAIRES)) {
            return verifierAcces(auteur, Permission.MODIFIER_SALAIRES);
        }
        if (!(cible instanceof Developpeur)) {
            return Resultat.echec("Seuls les développeurs ont des niveaux (Junior, Confirmé, Senior).");
        }
        Developpeur developpeur = (Developpeur) cible;   // cast : on sait maintenant que c'est un Developpeur
        if (developpeur.estEnPeriodeEssai()) {
            return Resultat.echec("Promotion impossible pendant la période d'essai (jusqu'au "
                    + Conge.formater(developpeur.getFinPeriodeEssai()) + ").");
        }
        Niveau ancien = developpeur.getNiveau();
        if (!developpeur.promouvoir()) {
            return Resultat.echec(developpeur.getNomComplet() + " est déjà " + ancien.getLibelle() + " : niveau maximum.");
        }
        return Resultat.succes("Promotion de " + developpeur.getNomComplet() + " : " + ancien.getLibelle() + " -> "
                        + developpeur.getNiveau().getLibelle() + ".",
                "Prime de niveau : " + Employe.formaterMontant(ancien.getPrime()) + " -> "
                        + Employe.formaterMontant(developpeur.getNiveau().getPrime()) + " par mois.");
    }

    public Resultat embaucher(Employe auteur, Employe nouveau) {
        if (!auteur.aLaPermission(Permission.EMBAUCHER)) {
            return verifierAcces(auteur, Permission.EMBAUCHER);
        }
        if (employes.contains(nouveau)) {
            return Resultat.echec(nouveau.getNomComplet() + " fait déjà partie de l'entreprise.");
        }
        if (nouveau instanceof DirecteurGeneral && getDirecteurGeneral() != null) {
            return Resultat.echec("L'entreprise a déjà un Directeur Général (" + getDirecteurGeneral().getNomComplet() + ").");
        }
        employes.add(nouveau);
        List<String> details = new ArrayList<>();
        details.add("Période d'essai jusqu'au " + Conge.formater(nouveau.getFinPeriodeEssai())
                + " : pas de congé payé avant cette date.");
        if (auteur instanceof ResponsableRH) {
            ((ResponsableRH) auteur).enregistrerRecrutement();
            details.add("Prime de recrutement de " + Employe.formaterMontant(ResponsableRH.PRIME_PAR_RECRUTEMENT)
                    + " ajoutée pour " + auteur.getNomComplet() + ".");
        }
        return Resultat.succes("Bienvenue à " + nouveau.getNomComplet() + " (" + nouveau.getMatricule() + ", "
                + nouveau.getFonction() + ") !", details.toArray(new String[0]));
    }

    private static String formaterPourcent(double valeur) {
        if (valeur == Math.floor(valeur)) {
            return (int) valeur + " %";
        }
        return String.format("%.1f %%", valeur);
    }
}
