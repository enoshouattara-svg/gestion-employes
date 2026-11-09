package gestion;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CLASSE MÈRE de tous les employés de l'entreprise.
 *
 * Elle est ABSTRAITE : on ne peut pas écrire "new Employe(...)", car dans l'entreprise
 * tout employé a forcément une fonction (Développeur, RH, Comptable ou DG).
 *
 * HÉRITAGE : chaque classe fille ("extends Employe") récupère automatiquement tout ce qui
 * est commun (identité, congés, permissions, fiche de paie...) et ne définit que ce qui
 * lui est propre : ses primes, ses règles de congé et ses droits d'accès.
 *
 * VISIBILITÉS utilisées dans le projet :
 *   private   -> visible uniquement dans CETTE classe               (ex. soldeConges)
 *   (aucun)   -> visible dans le même package "gestion"            (ex. debiterSoldeConges)
 *   protected -> visible dans le package ET dans les classes filles (ex. salaireBase)
 *   public    -> visible partout, y compris depuis Main             (ex. calculerSalaireNet)
 */
public abstract class Employe {

    // =====================================================================
    //  CONSTANTES (public static final : partagées par tous, non modifiables)
    //  Les montants et taux sont SIMPLIFIÉS, à but pédagogique.
    // =====================================================================
    public static final String DEVISE = "FCFA";
    public static final double SALAIRE_MINIMUM = 75_000;
    public static final int JOURS_CONGES_PAR_AN = 25;            // jours ouvrés (lundi -> vendredi)
    public static final int ANNEES_PAR_JOUR_BONUS = 5;           // +1 jour de congé tous les 5 ans
    public static final int DUREE_PERIODE_ESSAI_MOIS = 3;
    public static final int PREAVIS_CONGE_JOURS = 7;
    public static final int DELAI_DECLARATION_MALADIE_JOURS = 3;
    public static final int MAX_JOURS_EXCEPTIONNEL = 4;
    public static final int MAX_JOURS_SANS_SOLDE = 20;
    public static final int MAX_HEURES_PAR_PERMISSION = 4;
    public static final int QUOTA_HEURES_PERMISSION_PAR_AN = 24;
    public static final int JOURS_OUVRES_PAR_MOIS = 22;
    public static final double TAUX_COTISATION_SOCIALE = 0.063; // 6,3 %

    /** static : UN SEUL compteur partagé par tous les employés, pour générer les matricules. */
    private static int compteurMatricule = 0;

    // =====================================================================
    //  ATTRIBUTS
    // =====================================================================
    private final String matricule;          // final : ne change jamais après la création
    private String nom;
    private String prenom;
    private final LocalDate dateEmbauche;
    protected double salaireBase;            // protected : les classes filles s'en servent pour leurs primes
    private int soldeConges;                 // private : modifiable UNIQUEMENT par des méthodes contrôlées
    private int heuresPermissionUtilisees;
    private final List<Conge> historiqueConges = new ArrayList<>();

    // =====================================================================
    //  CONSTRUCTEURS (surcharge : même nom, paramètres différents)
    //  protected : seules les classes filles peuvent les appeler, via super(...)
    // =====================================================================

    /** Votre constructeur d'origine, sans paramètre. */
    protected Employe() {
        this("INCONNU");
    }

    /** Votre constructeur d'origine, avec le nom. */
    protected Employe(String nom) {
        this(nom, "Inconnu");
    }

    /** Votre constructeur d'origine avec nom et prénom (salaire minimum, embauché aujourd'hui). */
    protected Employe(String nom, String prenom) {
        this(nom, prenom, SALAIRE_MINIMUM, LocalDate.now());
    }

    /** Constructeur complet : les trois autres finissent tous par l'appeler grâce à this(...). */
    protected Employe(String nom, String prenom, double salaireBase, LocalDate dateEmbauche) {
        super(); // appelle le constructeur de Object, la classe mère de toutes les classes Java
        verifierTexte(nom, "Le nom");
        verifierTexte(prenom, "Le prénom");
        if (salaireBase < SALAIRE_MINIMUM) {
            throw new IllegalArgumentException(
                    "Le salaire de base doit être d'au moins " + formaterMontant(SALAIRE_MINIMUM) + ".");
        }
        if (dateEmbauche == null || dateEmbauche.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date d'embauche doit être aujourd'hui ou dans le passé.");
        }
        compteurMatricule++;
        this.matricule = String.format("EMP-%03d", compteurMatricule);
        this.nom = nom.trim().toUpperCase();
        this.prenom = prenom.trim();
        this.dateEmbauche = dateEmbauche;
        this.salaireBase = salaireBase;
        this.soldeConges = JOURS_CONGES_PAR_AN + getJoursBonusAnciennete();
    }

    private static void verifierTexte(String valeur, String champ) {
        Validation.exiger(Validation.verifierNom(valeur, champ));
    }

    // =====================================================================
    //  MÉTHODES ABSTRAITES : chaque classe fille DOIT les écrire
    // =====================================================================

    /** "Développeur", "Ressources Humaines", "Comptable" ou "Directeur Général". */
    public abstract String getMetier();

    /** Chaque métier ajoute ses propres primes (libellé -> montant) sur la fiche de paie. */
    protected abstract void ajouterPrimes(Map<String, Double> lignes);

    // =====================================================================
    //  IDENTITÉ
    // =====================================================================

    public String getMatricule() {
        return matricule;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        verifierTexte(nom, "Le nom");
        this.nom = nom.trim().toUpperCase();
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        verifierTexte(prenom, "Le prénom");
        this.prenom = prenom.trim();
    }

    public String getNomComplet() {
        return nom + " " + prenom;
    }

    /** Par défaut la fonction = le métier. Le Développeur la redéfinit pour ajouter son niveau. */
    public String getFonction() {
        return getMetier();
    }

    public LocalDate getDateEmbauche() {
        return dateEmbauche;
    }

    public double getSalaireBase() {
        return salaireBase;
    }

    public final int getAncienneteAnnees() {
        return Period.between(dateEmbauche, LocalDate.now()).getYears();
    }

    public final int getJoursBonusAnciennete() {
        return getAncienneteAnnees() / ANNEES_PAR_JOUR_BONUS;
    }

    public LocalDate getFinPeriodeEssai() {
        return dateEmbauche.plusMonths(DUREE_PERIODE_ESSAI_MOIS);
    }

    public boolean estEnPeriodeEssai() {
        return LocalDate.now().isBefore(getFinPeriodeEssai());
    }

    // =====================================================================
    //  DROITS D'ACCÈS
    // =====================================================================

    /**
     * Ce que TOUT employé a le droit de faire. Les classes filles REDÉFINISSENT (@Override)
     * cette méthode pour ajouter leurs droits, puis appellent super.aLaPermission(...) pour le reste.
     */
    public boolean aLaPermission(Permission permission) {
        switch (permission) {
            case DEMANDER_CONGE:
            case CONSULTER_MES_CONGES:
            case CONSULTER_MA_FICHE_DE_PAIE:
            case PRENDRE_PERMISSION:
                return true;
            default:
                return false;
        }
    }

    /** Qui a le droit de valider MES congés ? Par défaut : quelqu'un d'autre ayant le droit VALIDER_CONGES. */
    public boolean peutEtreValidePar(Employe validateur) {
        return validateur != this && validateur.aLaPermission(Permission.VALIDER_CONGES);
    }

    public String getValidateurDesConges() {
        return "le service RH (ou le Directeur Général)";
    }

    /** Vrai seulement pour le DG, qui n'a pas de supérieur. */
    public boolean estValidationAutomatique() {
        return false;
    }

    // =====================================================================
    //  CONGÉS : APTITUDE ET ÉLIGIBILITÉ
    // =====================================================================

    public int getSoldeConges() {
        return soldeConges;
    }

    /** Liste en lecture seule : impossible de la modifier depuis l'extérieur. */
    public List<Conge> getHistoriqueConges() {
        return Collections.unmodifiableList(historiqueConges);
    }

    /**
     * Aptitude GÉNÉRALE à un type de congé (sans regarder les dates).
     * @return la liste des raisons qui empêchent le congé ; liste vide = APTE.
     */
    public List<String> verifierAptitude(TypeConge type) {
        List<String> raisons = new ArrayList<>();
        if (type == TypeConge.PAYE) {
            if (estEnPeriodeEssai()) {
                raisons.add("Période d'essai en cours jusqu'au " + Conge.formater(getFinPeriodeEssai())
                        + " : les congés payés ne sont pas encore ouverts.");
            }
            if (soldeConges <= 0) {
                raisons.add("Solde de congés payés épuisé (0 jour restant).");
            }
        }
        return raisons;
    }

    public boolean estApte(TypeConge type) {
        return verifierAptitude(type).isEmpty();
    }

    /**
     * Éligibilité COMPLÈTE pour une demande précise (type + dates).
     * @return la liste de TOUTES les raisons de refus ; liste vide = la demande est recevable.
     */
    public List<String> verifierEligibilite(TypeConge type, LocalDate debut, LocalDate fin) {
        List<String> raisons = new ArrayList<>();
        LocalDate aujourdhui = LocalDate.now();

        // 1) Cohérence des dates
        if (type == null || debut == null || fin == null) {
            raisons.add("Le type et les dates du congé sont obligatoires.");
            return raisons;
        }
        if (fin.isBefore(debut)) {
            raisons.add("La date de fin (" + Conge.formater(fin) + ") est avant la date de début ("
                    + Conge.formater(debut) + ").");
            return raisons;
        }
        int jours = Conge.compterJoursOuvres(debut, fin);
        if (jours == 0) {
            raisons.add("La période choisie ne contient aucun jour ouvré (uniquement un week-end).");
            return raisons;
        }

        // 2) Aptitude générale (période d'essai, solde épuisé...)
        raisons.addAll(verifierAptitude(type));

        // 3) Règles propres à chaque type de congé
        switch (type) {
            case PAYE:
                if (soldeConges > 0 && jours > soldeConges) {
                    raisons.add("Solde insuffisant : " + jours + " jour(s) ouvré(s) demandé(s), "
                            + soldeConges + " disponible(s).");
                }
                break;
            case SANS_SOLDE:
                if (jours > MAX_JOURS_SANS_SOLDE) {
                    raisons.add("Un congé sans solde est limité à " + MAX_JOURS_SANS_SOLDE
                            + " jours ouvrés par demande (" + jours + " demandés).");
                }
                break;
            case EXCEPTIONNEL:
                if (jours > MAX_JOURS_EXCEPTIONNEL) {
                    raisons.add("Un congé exceptionnel est limité à " + MAX_JOURS_EXCEPTIONNEL
                            + " jours ouvrés (" + jours + " demandés).");
                }
                break;
            case MALADIE:
                if (debut.isBefore(aujourdhui.minusDays(DELAI_DECLARATION_MALADIE_JOURS))) {
                    raisons.add("Un arrêt maladie doit être déclaré dans les "
                            + DELAI_DECLARATION_MALADIE_JOURS + " jours.");
                }
                break;
            default:
                break;
        }

        // 4) Délais : pas de congé dans le passé, préavis pour les congés planifiables
        if (type != TypeConge.MALADIE && debut.isBefore(aujourdhui)) {
            raisons.add("Impossible de poser ce congé dans le passé.");
        } else if (type.isPlanifiable() && debut.isBefore(aujourdhui.plusDays(PREAVIS_CONGE_JOURS))) {
            raisons.add("Préavis insuffisant : un " + type.getLibelle().toLowerCase() + " se demande "
                    + PREAVIS_CONGE_JOURS + " jours à l'avance (début au plus tôt le "
                    + Conge.formater(aujourdhui.plusDays(PREAVIS_CONGE_JOURS)) + ").");
        }

        // 5) Pas deux demandes sur la même période
        for (Conge conge : historiqueConges) {
            if (conge.estActif() && conge.chevauche(debut, fin)) {
                raisons.add("Cette période chevauche votre demande n°" + conge.getId() + " ("
                        + conge.getPeriode() + ", " + conge.getStatut().getLibelle().toLowerCase() + ").");
            }
        }

        // 6) Règles propres au métier (POLYMORPHISME : chaque classe fille a les siennes)
        verifierReglesMetier(type, debut, fin, raisons);
        return raisons;
    }

    /** Par défaut aucune règle en plus. Les classes filles la redéfinissent si besoin. */
    protected void verifierReglesMetier(TypeConge type, LocalDate debut, LocalDate fin, List<String> raisons) {
        // rien par défaut
    }

    public boolean estAbsentPendant(LocalDate debut, LocalDate fin) {
        for (Conge conge : historiqueConges) {
            if (conge.getStatut() == StatutConge.ACCEPTE && conge.chevauche(debut, fin)) {
                return true;
            }
        }
        return false;
    }

    public boolean estAbsentAujourdhui() {
        LocalDate aujourdhui = LocalDate.now();
        return estAbsentPendant(aujourdhui, aujourdhui);
    }

    // --- Visibilité PACKAGE : seule l'Entreprise peut toucher au solde, après ses contrôles ---

    void ajouterConge(Conge conge) {
        historiqueConges.add(conge);
    }

    void debiterSoldeConges(int jours) {
        soldeConges -= jours;
    }

    void crediterSoldeConges(int jours) {
        soldeConges += jours;
    }

    void appliquerAugmentation(double pourcentage) {
        salaireBase = Math.round(salaireBase * (1 + pourcentage / 100));
    }

    // =====================================================================
    //  PERMISSION D'ABSENCE (quelques heures, sans toucher au solde)
    // =====================================================================

    public int getHeuresPermissionRestantes() {
        return QUOTA_HEURES_PERMISSION_PAR_AN - heuresPermissionUtilisees;
    }

    /** Votre méthode d'origine, désormais complète : une absence de quelques heures. */
    public Resultat prendrePermission(int heures, String motif) {
        if (heures <= 0) {
            return Resultat.echec("Le nombre d'heures doit être supérieur à 0.");
        }
        if (heures > MAX_HEURES_PAR_PERMISSION) {
            return Resultat.echec("Une permission dure au maximum " + MAX_HEURES_PAR_PERMISSION + " h.",
                    "Pour une absence plus longue, il faut poser un congé.");
        }
        if (estAbsentAujourdhui()) {
            return Resultat.echec("Vous êtes déjà en congé aujourd'hui : permission inutile.");
        }
        if (heures > getHeuresPermissionRestantes()) {
            return Resultat.echec("Quota annuel dépassé : il vous reste " + getHeuresPermissionRestantes()
                    + " h de permission sur " + QUOTA_HEURES_PERMISSION_PAR_AN + " h.");
        }
        String erreurMotif = Validation.verifierMotif(motif);
        if (erreurMotif != null) {
            return Resultat.echec("Motif de la permission refusé : " + erreurMotif);
        }
        heuresPermissionUtilisees += heures;
        String raison = motif.trim();
        return Resultat.succes("Permission de " + heures + " h accordée (motif : " + raison + ").",
                "Il vous reste " + getHeuresPermissionRestantes() + " h de permission cette année.");
    }

    /** SURCHARGE : même nom, sans paramètre -> permission d'1 heure. */
    public Resultat prendrePermission() {
        return prendrePermission(1, "non précisé");
    }

    // =====================================================================
    //  SALAIRE
    //  brut = base + prime d'ancienneté + primes du métier - retenue sans solde
    //  net  = brut - cotisations sociales - impôt
    // =====================================================================

    /** 1 % du salaire de base par année d'ancienneté, à partir de 2 ans, plafonné à 25 %. */
    public int getTauxAnciennete() {
        int annees = getAncienneteAnnees();
        if (annees < 2) {
            return 0;
        }
        return Math.min(annees, 25);
    }

    public double getPrimeAnciennete() {
        return salaireBase * getTauxAnciennete() / 100.0;
    }

    /** "final" : aucune classe fille ne peut modifier la façon de collecter les primes. */
    public final Map<String, Double> getDetailPrimes() {
        Map<String, Double> lignes = new LinkedHashMap<>();
        ajouterPrimes(lignes);   // appel POLYMORPHE : c'est la version du Développeur, du RH... qui s'exécute
        return lignes;
    }

    public final double getTotalPrimes() {
        double total = 0;
        for (double montant : getDetailPrimes().values()) {
            total += montant;
        }
        return total;
    }

    /** Jours ouvrés de congé SANS SOLDE acceptés tombant dans le mois donné. */
    public int getJoursSansSoldeDuMois(YearMonth mois) {
        int total = 0;
        for (Conge conge : historiqueConges) {
            if (conge.getType() == TypeConge.SANS_SOLDE && conge.getStatut() == StatutConge.ACCEPTE) {
                LocalDate debut = conge.getDebut().isBefore(mois.atDay(1)) ? mois.atDay(1) : conge.getDebut();
                LocalDate fin = conge.getFin().isAfter(mois.atEndOfMonth()) ? mois.atEndOfMonth() : conge.getFin();
                if (!debut.isAfter(fin)) {
                    total += Conge.compterJoursOuvres(debut, fin);
                }
            }
        }
        return total;
    }

    public double getRetenueSansSolde() {
        double retenue = getJoursSansSoldeDuMois(YearMonth.now()) * salaireBase / JOURS_OUVRES_PAR_MOIS;
        return Math.min(retenue, salaireBase);
    }

    public final double calculerSalaireBrut() {
        return salaireBase + getPrimeAnciennete() + getTotalPrimes() - getRetenueSansSolde();
    }

    public final double calculerCotisations() {
        return calculerSalaireBrut() * TAUX_COTISATION_SOCIALE;
    }

    public final double calculerImpot() {
        return calculerImpotSurSalaire(calculerSalaireBrut() - calculerCotisations());
    }

    public final double calculerSalaireNet() {
        return calculerSalaireBrut() - calculerCotisations() - calculerImpot();
    }

    /**
     * Barème progressif SIMPLIFIÉ (fictif, pédagogique) : chaque tranche a son taux.
     *   0 -> 100 000 : 0 %  |  100 000 -> 300 000 : 10 %  |  300 000 -> 800 000 : 20 %  |  au-delà : 30 %
     */
    public static double calculerImpotSurSalaire(double imposable) {
        double impot = 0;
        double reste = imposable;
        if (reste > 800_000) {
            impot += (reste - 800_000) * 0.30;
            reste = 800_000;
        }
        if (reste > 300_000) {
            impot += (reste - 300_000) * 0.20;
            reste = 300_000;
        }
        if (reste > 100_000) {
            impot += (reste - 100_000) * 0.10;
        }
        return impot;
    }

    // =====================================================================
    //  AFFICHAGE
    // =====================================================================

    public static String formaterMontant(double montant) {
        String chiffres = String.format(Locale.US, "%,.0f", Math.abs(montant)).replace(',', ' ');
        return (montant < -0.5 ? "- " : "") + chiffres + " " + DEVISE;
    }

    public void afficherFicheDePaie() {
        YearMonth mois = YearMonth.now();
        String nomMois = mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + mois.getYear();
        double brut = calculerSalaireBrut();
        String bordure = "+" + "-".repeat(63) + "+";

        System.out.println(bordure);
        ligneTexte("FICHE DE PAIE - " + nomMois.toUpperCase());
        ligneTexte(getNomComplet() + " (" + matricule + ") - " + getFonction());
        System.out.println(bordure);
        ligneMontant("Salaire de base", salaireBase);
        if (getPrimeAnciennete() > 0) {
            ligneMontant("Prime d'ancienneté (" + getAncienneteAnnees() + " ans, " + getTauxAnciennete() + " %)",
                    getPrimeAnciennete());
        }
        for (Map.Entry<String, Double> prime : getDetailPrimes().entrySet()) {
            ligneMontant(prime.getKey(), prime.getValue());
        }
        int joursSansSolde = getJoursSansSoldeDuMois(mois);
        if (joursSansSolde > 0) {
            ligneMontant("Retenue congé sans solde (" + joursSansSolde + " j)", -getRetenueSansSolde());
        }
        System.out.println(bordure);
        ligneMontant("SALAIRE BRUT", brut);
        ligneMontant("Cotisation sociale (6,3 %)", -calculerCotisations());
        ligneMontant("Impôt sur salaire (barème simplifié)", -calculerImpot());
        System.out.println(bordure);
        ligneMontant("Net à payer", calculerSalaireNet());
        System.out.println(bordure);
    }

    private static void ligneTexte(String texte) {
        String coupe = texte.length() > 61 ? texte.substring(0, 61) : texte;
        System.out.printf("| %-61s |%n", coupe);
    }

    private static void ligneMontant(String libelle, double montant) {
        String coupe = libelle.length() > 42 ? libelle.substring(0, 42) : libelle;
        System.out.printf("| %-42s %18s |%n", coupe, formaterMontant(montant));
    }

    public void afficherProfil() {
        System.out.println("  Matricule          : " + matricule);
        System.out.println("  Nom complet        : " + getNomComplet());
        System.out.println("  Fonction           : " + getFonction());
        System.out.println("  Date d'embauche    : " + Conge.formater(dateEmbauche)
                + " (" + getAncienneteAnnees() + " an(s) d'ancienneté)");
        System.out.println("  Période d'essai    : " + (estEnPeriodeEssai()
                ? "en cours jusqu'au " + Conge.formater(getFinPeriodeEssai()) : "terminée"));
        System.out.println("  Salaire de base    : " + formaterMontant(salaireBase));
        System.out.println("  Solde de congés    : " + soldeConges + " jour(s) ouvré(s)"
                + (getJoursBonusAnciennete() > 0 ? " (dont +" + getJoursBonusAnciennete() + " pour ancienneté)" : ""));
        System.out.println("  Permissions        : " + getHeuresPermissionRestantes() + " h restantes sur "
                + QUOTA_HEURES_PERMISSION_PAR_AN + " h");
        System.out.println("  Aujourd'hui        : " + (estAbsentAujourdhui() ? "en congé" : "présent au poste"));
        afficherInfosSpecifiques();
    }

    /** Les classes filles ajoutent ici leurs informations propres. */
    protected void afficherInfosSpecifiques() {
        // rien par défaut
    }

    /** Répond à la question : "Ai-je le droit de... ? Suis-je apte à prendre un congé ?" */
    public void afficherDroitsEtAptitudes() {
        System.out.println("  Mes droits d'accès (" + getMetier() + ") :");
        for (Permission permission : Permission.values()) {
            System.out.println("    " + (aLaPermission(permission) ? "[OUI] " : "[NON] ") + permission.getLibelle());
        }
        System.out.println();
        System.out.println("  Mon aptitude à prendre un congé :");
        for (TypeConge type : TypeConge.values()) {
            List<String> raisons = verifierAptitude(type);
            if (raisons.isEmpty()) {
                System.out.printf("    [APTE]     %-19s (%s)%n", type.getLibelle(), type.getConditions());
            } else {
                System.out.printf("    [NON APTE] %-19s%n", type.getLibelle());
                for (String raison : raisons) {
                    System.out.println("                 -> " + raison);
                }
            }
        }
        System.out.println();
        System.out.println("  Mes congés sont validés par : " + getValidateurDesConges());
        System.out.println("  Solde : " + soldeConges + " jour(s) ouvré(s) | Permissions : "
                + getHeuresPermissionRestantes() + " h restantes");
    }

    @Override
    public String toString() {
        return String.format("%-8s %-20s %s", matricule, getNomComplet(), getFonction());
    }
}
