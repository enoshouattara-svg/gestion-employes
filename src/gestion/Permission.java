package gestion;

/**
 * Les droits d'accès aux fonctions du programme.
 * Chaque classe d'employé décide, dans aLaPermission(...), lesquels elle possède.
 */
public enum Permission {

    // --- Droits de tout employé ---
    DEMANDER_CONGE("Demander un congé"),
    CONSULTER_MES_CONGES("Consulter mes congés"),
    CONSULTER_MA_FICHE_DE_PAIE("Consulter ma fiche de paie"),
    PRENDRE_PERMISSION("Demander une permission d'absence (quelques heures)"),

    // --- Droits réservés ---
    VALIDER_CONGES("Valider / refuser les congés des autres"),
    CONSULTER_TOUS_LES_CONGES("Consulter les congés de tout le personnel"),
    CONSULTER_TOUS_LES_SALAIRES("Consulter les salaires de tout le personnel"),
    VOIR_STATISTIQUES("Voir les statistiques et la masse salariale"),
    MODIFIER_SALAIRES("Augmenter un salaire / promouvoir"),
    EMBAUCHER("Embaucher un employé");

    private final String libelle;

    Permission(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
