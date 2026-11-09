package gestion;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

/**
 * Le Comptable.
 * Droits en plus : consulter les salaires de tout le personnel et les statistiques.
 * Règle de congé propre : pas de congé planifiable pendant la clôture annuelle (décembre).
 */
public class Comptable extends Employe {

    public static final double PRIME_DE_FONCTION = 30_000;
    public static final double PRIME_DE_BILAN = 100_000;
    public static final Month MOIS_DE_CLOTURE = Month.DECEMBER;

    private boolean bilanAnnuelValide;

    public Comptable(String nom, String prenom, double salaireBase, LocalDate dateEmbauche) {
        super(nom, prenom, salaireBase, dateEmbauche);
    }

    @Override
    public String getMetier() {
        return "Comptable";
    }

    @Override
    public boolean aLaPermission(Permission permission) {
        if (permission == Permission.CONSULTER_TOUS_LES_SALAIRES || permission == Permission.VOIR_STATISTIQUES) {
            return true;
        }
        return super.aLaPermission(permission);
    }

    @Override
    protected void ajouterPrimes(Map<String, Double> lignes) {
        lignes.put("Prime de fonction comptable", PRIME_DE_FONCTION);
        if (bilanAnnuelValide) {
            lignes.put("Prime de bilan annuel", PRIME_DE_BILAN);
        }
    }

    @Override
    protected void verifierReglesMetier(TypeConge type, LocalDate debut, LocalDate fin, List<String> raisons) {
        if (!type.isPlanifiable()) {
            return;
        }
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            if (jour.getMonth() == MOIS_DE_CLOTURE) {
                raisons.add("Clôture annuelle des comptes : un comptable ne peut pas poser de congé planifiable en décembre.");
                break;   // une seule fois suffit
            }
        }
    }

    public boolean isBilanAnnuelValide() {
        return bilanAnnuelValide;
    }

    public void setBilanAnnuelValide(boolean bilanAnnuelValide) {
        this.bilanAnnuelValide = bilanAnnuelValide;
    }

    @Override
    protected void afficherInfosSpecifiques() {
        System.out.println("  Bilan annuel       : " + (bilanAnnuelValide ? "validé" : "en cours"));
    }
}
