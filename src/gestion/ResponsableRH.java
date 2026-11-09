package gestion;

import java.time.LocalDate;
import java.util.Map;

/**
 * Le service des Ressources Humaines.
 * Droits en plus : valider les congés, consulter tous les congés, embaucher.
 * Règle de séparation des pouvoirs : un RH ne valide JAMAIS ses propres congés,
 * c'est le Directeur Général qui s'en charge.
 */
public class ResponsableRH extends Employe {

    public static final double PRIME_DE_FONCTION = 40_000;
    public static final double PRIME_PAR_RECRUTEMENT = 25_000;

    private int recrutementsDuMois;

    public ResponsableRH(String nom, String prenom, double salaireBase, LocalDate dateEmbauche) {
        super(nom, prenom, salaireBase, dateEmbauche);
    }

    @Override
    public String getMetier() {
        return "Ressources Humaines";
    }

    @Override
    public boolean aLaPermission(Permission permission) {
        switch (permission) {
            case VALIDER_CONGES:
            case CONSULTER_TOUS_LES_CONGES:
            case EMBAUCHER:
                return true;
            default:
                return super.aLaPermission(permission);   // + les droits de tout employé
        }
    }

    /** Seul le DG peut valider les congés d'un RH. */
    @Override
    public boolean peutEtreValidePar(Employe validateur) {
        return validateur instanceof DirecteurGeneral;
    }

    @Override
    public String getValidateurDesConges() {
        return "le Directeur Général (un RH ne valide pas ses propres congés)";
    }

    @Override
    protected void ajouterPrimes(Map<String, Double> lignes) {
        lignes.put("Prime de fonction RH", PRIME_DE_FONCTION);
        if (recrutementsDuMois > 0) {
            lignes.put("Prime de recrutement (x" + recrutementsDuMois + ")",
                    recrutementsDuMois * PRIME_PAR_RECRUTEMENT);
        }
    }

    /** Appelée par l'Entreprise à chaque embauche réalisée par ce RH. */
    void enregistrerRecrutement() {
        recrutementsDuMois++;
    }

    public int getRecrutementsDuMois() {
        return recrutementsDuMois;
    }

    @Override
    protected void afficherInfosSpecifiques() {
        System.out.println("  Recrutements       : " + recrutementsDuMois + " ce mois-ci");
    }
}
