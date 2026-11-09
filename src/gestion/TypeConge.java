package gestion;

/**
 * Les types de congés possibles.
 * Un "enum" est une liste FERMÉE de valeurs : impossible de demander un type qui n'existe pas.
 */
public enum TypeConge {

    //            libellé               planifiable  décompté du solde  conditions affichées
    PAYE("Congé payé", true, true,
            "payé, décompté du solde, préavis de " + Employe.PREAVIS_CONGE_JOURS + " jours"),
    MALADIE("Congé maladie", false, false,
            "certificat médical, à déclarer sous " + Employe.DELAI_DECLARATION_MALADIE_JOURS + " jours"),
    SANS_SOLDE("Congé sans solde", true, false,
            "non payé (retenue sur salaire), préavis de " + Employe.PREAVIS_CONGE_JOURS + " jours, "
                    + Employe.MAX_JOURS_SANS_SOLDE + " j max"),
    EXCEPTIONNEL("Congé exceptionnel", false, false,
            "mariage, naissance, décès : " + Employe.MAX_JOURS_EXCEPTIONNEL + " jours max, payé");

    private final String libelle;
    /** Planifiable = posé à l'avance : soumis au préavis, aux gels de service et aux effectifs. */
    private final boolean planifiable;
    /** Vrai si les jours sont retirés du solde de congés. */
    private final boolean decompteDuSolde;
    private final String conditions;

    TypeConge(String libelle, boolean planifiable, boolean decompteDuSolde, String conditions) {
        this.libelle = libelle;
        this.planifiable = planifiable;
        this.decompteDuSolde = decompteDuSolde;
        this.conditions = conditions;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean isPlanifiable() {
        return planifiable;
    }

    public boolean isDecompteDuSolde() {
        return decompteDuSolde;
    }

    public String getConditions() {
        return conditions;
    }
}
