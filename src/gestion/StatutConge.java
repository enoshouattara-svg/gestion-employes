package gestion;

/** Cycle de vie d'une demande : EN_ATTENTE -> ACCEPTE / REFUSE, et ANNULE par le demandeur. */
public enum StatutConge {

    EN_ATTENTE("En attente"),
    ACCEPTE("Accepté"),
    REFUSE("Refusé"),
    ANNULE("Annulé");

    private final String libelle;

    StatutConge(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
