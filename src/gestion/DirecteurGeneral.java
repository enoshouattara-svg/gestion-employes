package gestion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Le Directeur Général : il a TOUS les droits.
 * Ses congés sont acceptés automatiquement (il n'a pas de supérieur), mais il doit
 * avoir désigné un intérimaire présent pendant son absence.
 */
public class DirecteurGeneral extends Employe {

    public static final double PRIME_DE_REPRESENTATION = 200_000;
    public static final double TAUX_INTERESSEMENT = 0.005;   // 0,5 % du bénéfice annuel, réparti sur 12 mois

    private double beneficeAnnuel;
    private Employe interim;

    public DirecteurGeneral(String nom, String prenom, double salaireBase, LocalDate dateEmbauche) {
        super(nom, prenom, salaireBase, dateEmbauche);
    }

    @Override
    public String getMetier() {
        return "Directeur Général";
    }

    @Override
    public boolean aLaPermission(Permission permission) {
        return true;   // le DG a accès à tout
    }

    @Override
    public boolean peutEtreValidePar(Employe validateur) {
        return false;  // personne au-dessus de lui
    }

    @Override
    public boolean estValidationAutomatique() {
        return true;
    }

    @Override
    public String getValidateurDesConges() {
        return "personne : validation automatique (le DG n'a pas de supérieur)";
    }

    @Override
    protected void ajouterPrimes(Map<String, Double> lignes) {
        lignes.put("Prime de représentation", PRIME_DE_REPRESENTATION);
        if (beneficeAnnuel > 0) {
            lignes.put("Intéressement (0,5 % du bénéfice / 12)", beneficeAnnuel * TAUX_INTERESSEMENT / 12);
        }
    }

    @Override
    protected void verifierReglesMetier(TypeConge type, LocalDate debut, LocalDate fin, List<String> raisons) {
        if (!type.isPlanifiable()) {
            return;
        }
        if (interim == null) {
            raisons.add("Aucun intérimaire désigné : le DG doit nommer un remplaçant avant de partir.");
        } else if (interim.estAbsentPendant(debut, fin)) {
            raisons.add("Votre intérimaire (" + interim.getNomComplet() + ") est en congé sur cette période.");
        }
    }

    public double getBeneficeAnnuel() {
        return beneficeAnnuel;
    }

    public void setBeneficeAnnuel(double beneficeAnnuel) {
        this.beneficeAnnuel = beneficeAnnuel;
    }

    public Employe getInterim() {
        return interim;
    }

    public void setInterim(Employe interim) {
        if (interim == this) {
            throw new IllegalArgumentException("Le DG ne peut pas être son propre intérimaire.");
        }
        this.interim = interim;
    }

    @Override
    protected void afficherInfosSpecifiques() {
        System.out.println("  Bénéfice annuel    : " + formaterMontant(beneficeAnnuel));
        System.out.println("  Intérimaire        : " + (interim == null ? "aucun" : interim.getNomComplet()));
    }
}
