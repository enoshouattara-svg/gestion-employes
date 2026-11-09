package gestion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Un Développeur EST UN Employe (héritage : "extends Employe").
 * Il hérite de tout (identité, congés, fiche de paie...) et ajoute :
 *  - un niveau (Junior / Confirmé / Senior) qui donne une prime,
 *  - des heures supplémentaires majorées (+25 % puis +50 %),
 *  - une prime par projet livré,
 *  - une règle de congé propre : gel des congés autour d'une mise en production.
 * Droits : uniquement ceux d'un employé de base (il ne redéfinit pas aLaPermission).
 */
public class Developpeur extends Employe {

    public static final double PRIME_PAR_PROJET_LIVRE = 50_000;
    public static final double HEURES_LEGALES_PAR_MOIS = 173.33;
    public static final int MAX_HEURES_SUP_PAR_MOIS = 60;
    public static final int JOURS_GEL_AVANT_LIVRAISON = 5;
    public static final int JOURS_GEL_APRES_LIVRAISON = 2;

    private Niveau niveau;
    private String langagePrincipal;
    private int heuresSupplementaires;       // du mois en cours
    private int projetsLivres;               // du mois en cours
    private LocalDate dateMiseEnProduction;  // null = aucune livraison prévue

    public Developpeur(String nom, String prenom, double salaireBase, LocalDate dateEmbauche,
                       Niveau niveau, String langagePrincipal) {
        super(nom, prenom, salaireBase, dateEmbauche);   // appelle le constructeur de la classe mère
        this.niveau = (niveau == null) ? Niveau.JUNIOR : niveau;
        Validation.exiger(Validation.verifierLangage(langagePrincipal));
        this.langagePrincipal = langagePrincipal.trim();
    }

    @Override
    public String getMetier() {
        return "Développeur";
    }

    @Override
    public String getFonction() {
        return getMetier() + " " + niveau.getLibelle();
    }

    // ------------------------- Salaire -------------------------

    @Override
    protected void ajouterPrimes(Map<String, Double> lignes) {
        if (niveau.getPrime() > 0) {
            lignes.put("Prime de niveau (" + niveau.getLibelle() + ")", niveau.getPrime());
        }
        if (heuresSupplementaires > 0) {
            lignes.put("Heures supplémentaires (" + heuresSupplementaires + " h)", calculerHeuresSupplementaires());
        }
        if (projetsLivres > 0) {
            lignes.put("Prime projets livrés (x" + projetsLivres + ")", projetsLivres * PRIME_PAR_PROJET_LIVRE);
        }
    }

    /** Les 8 premières heures sont majorées de 25 %, les suivantes de 50 %. */
    private double calculerHeuresSupplementaires() {
        double tauxHoraire = salaireBase / HEURES_LEGALES_PAR_MOIS;   // salaireBase est protected : accessible ici
        if (heuresSupplementaires <= 8) {
            return heuresSupplementaires * tauxHoraire * 1.25;
        }
        return 8 * tauxHoraire * 1.25 + (heuresSupplementaires - 8) * tauxHoraire * 1.50;
    }

    // ------------------------- Congés -------------------------

    @Override
    protected void verifierReglesMetier(TypeConge type, LocalDate debut, LocalDate fin, List<String> raisons) {
        if (dateMiseEnProduction == null || !type.isPlanifiable()) {
            return;
        }
        LocalDate debutGel = dateMiseEnProduction.minusDays(JOURS_GEL_AVANT_LIVRAISON);
        LocalDate finGel = dateMiseEnProduction.plusDays(JOURS_GEL_APRES_LIVRAISON);
        boolean pendantLeGel = !debut.isAfter(finGel) && !fin.isBefore(debutGel);
        if (pendantLeGel) {
            raisons.add("Gel des congés : mise en production prévue le " + Conge.formater(dateMiseEnProduction)
                    + " (aucun congé du " + Conge.formater(debutGel) + " au " + Conge.formater(finGel) + ").");
        }
    }

    // ------------------------- Carrière -------------------------

    /** Visibilité package : seule l'Entreprise peut promouvoir, après avoir vérifié les droits. */
    boolean promouvoir() {
        Niveau suivant = niveau.suivant();
        if (suivant == null) {
            return false;
        }
        niveau = suivant;
        return true;
    }

    // ------------------------- Accesseurs -------------------------

    public Niveau getNiveau() {
        return niveau;
    }

    public String getLangagePrincipal() {
        return langagePrincipal;
    }

    public void setLangagePrincipal(String langagePrincipal) {
        Validation.exiger(Validation.verifierLangage(langagePrincipal));
        this.langagePrincipal = langagePrincipal.trim();
    }

    public int getHeuresSupplementaires() {
        return heuresSupplementaires;
    }

    public void setHeuresSupplementaires(int heures) {
        if (heures < 0 || heures > MAX_HEURES_SUP_PAR_MOIS) {
            throw new IllegalArgumentException("Heures supplémentaires : entre 0 et " + MAX_HEURES_SUP_PAR_MOIS + " h par mois.");
        }
        this.heuresSupplementaires = heures;
    }

    public int getProjetsLivres() {
        return projetsLivres;
    }

    public void setProjetsLivres(int projetsLivres) {
        if (projetsLivres < 0) {
            throw new IllegalArgumentException("Le nombre de projets livrés ne peut pas être négatif.");
        }
        this.projetsLivres = projetsLivres;
    }

    public LocalDate getDateMiseEnProduction() {
        return dateMiseEnProduction;
    }

    public void setDateMiseEnProduction(LocalDate dateMiseEnProduction) {
        this.dateMiseEnProduction = dateMiseEnProduction;
    }

    @Override
    protected void afficherInfosSpecifiques() {
        System.out.println("  Langage principal  : " + langagePrincipal);
        System.out.println("  Ce mois-ci         : " + heuresSupplementaires + " h sup., "
                + projetsLivres + " projet(s) livré(s)");
        if (dateMiseEnProduction != null) {
            System.out.println("  Mise en production : " + Conge.formater(dateMiseEnProduction)
                    + " (congés gelés autour de cette date)");
        }
    }
}
