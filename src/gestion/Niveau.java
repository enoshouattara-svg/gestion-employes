package gestion;

/** Niveau d'expérience d'un développeur : il détermine une prime mensuelle. */
public enum Niveau {

    JUNIOR("Junior", 0),
    CONFIRME("Confirmé", 50_000),
    SENIOR("Senior", 100_000);

    private final String libelle;
    private final double prime;

    Niveau(String libelle, double prime) {
        this.libelle = libelle;
        this.prime = prime;
    }

    public String getLibelle() {
        return libelle;
    }

    public double getPrime() {
        return prime;
    }

    /** Niveau suivant pour une promotion, ou null si on est déjà au maximum. */
    public Niveau suivant() {
        switch (this) {
            case JUNIOR:
                return CONFIRME;
            case CONFIRME:
                return SENIOR;
            default:
                return null;
        }
    }
}
