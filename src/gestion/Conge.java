package gestion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Une demande de congé.
 *
 * VISIBILITÉ : le constructeur et les méthodes accepter / refuser / annuler n'ont AUCUN
 * mot-clé (visibilité "package"). Seule la classe Entreprise (même package "gestion")
 * peut donc créer ou faire changer d'état une demande, après avoir fait ses contrôles.
 * Depuis Main (autre package), on peut seulement la LIRE grâce aux méthodes public.
 */
public class Conge {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static int compteur = 0;

    private final int id;
    private final Employe demandeur;
    private final TypeConge type;
    private final LocalDate debut;
    private final LocalDate fin;
    private final String motif;
    private final LocalDate dateDemande;
    private StatutConge statut;
    private Employe decideur;
    private String commentaire;

    Conge(Employe demandeur, TypeConge type, LocalDate debut, LocalDate fin, String motif) {
        compteur++;
        this.id = compteur;
        this.demandeur = demandeur;
        this.type = type;
        this.debut = debut;
        this.fin = fin;
        this.motif = (motif == null || motif.trim().isEmpty()) ? "-" : motif.trim();
        this.dateDemande = LocalDate.now();
        this.statut = StatutConge.EN_ATTENTE;
        this.commentaire = "";
    }

    // ===================== Changements d'état (package) =====================

    void accepter(Employe decideur) {
        this.statut = StatutConge.ACCEPTE;
        this.decideur = decideur;
    }

    void refuser(Employe decideur, String commentaire) {
        this.statut = StatutConge.REFUSE;
        this.decideur = decideur;
        this.commentaire = commentaire;
    }

    void annuler() {
        this.statut = StatutConge.ANNULE;
    }

    // ===================== Lecture (public) =====================

    public int getId() {
        return id;
    }

    public Employe getDemandeur() {
        return demandeur;
    }

    public TypeConge getType() {
        return type;
    }

    public LocalDate getDebut() {
        return debut;
    }

    public LocalDate getFin() {
        return fin;
    }

    public String getMotif() {
        return motif;
    }

    public LocalDate getDateDemande() {
        return dateDemande;
    }

    public StatutConge getStatut() {
        return statut;
    }

    public Employe getDecideur() {
        return decideur;
    }

    public String getCommentaire() {
        return commentaire;
    }

    /** Nombre de jours réellement travaillés manqués (les week-ends ne comptent pas). */
    public int getJoursOuvres() {
        return compterJoursOuvres(debut, fin);
    }

    /** Une demande "active" bloque la période : elle est en attente ou acceptée. */
    public boolean estActif() {
        return statut == StatutConge.EN_ATTENTE || statut == StatutConge.ACCEPTE;
    }

    /** Vrai si cette demande et la période [autreDebut ; autreFin] ont au moins un jour en commun. */
    public boolean chevauche(LocalDate autreDebut, LocalDate autreFin) {
        return !debut.isAfter(autreFin) && !fin.isBefore(autreDebut);
    }

    public String getPeriode() {
        return "du " + formater(debut) + " au " + formater(fin);
    }

    @Override
    public String toString() {
        String texte = String.format("n°%-3d %-20s %-18s %s  %2d j  %-10s",
                id, demandeur.getNomComplet(), type.getLibelle(), getPeriode(), getJoursOuvres(),
                statut.getLibelle());
        if (decideur != null && statut != StatutConge.ANNULE) {
            texte += " par " + decideur.getNom();
        }
        if (!commentaire.isEmpty()) {
            texte += " (" + commentaire + ")";
        }
        return texte;
    }

    // ===================== Outils sur les dates (static) =====================

    public static boolean estJourOuvre(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        return jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY;
    }

    public static int compterJoursOuvres(LocalDate debut, LocalDate fin) {
        int nombre = 0;
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            if (estJourOuvre(jour)) {
                nombre++;
            }
        }
        return nombre;
    }

    public static LocalDate prochainJourOuvre(LocalDate date) {
        LocalDate jour = date;
        while (!estJourOuvre(jour)) {
            jour = jour.plusDays(1);
        }
        return jour;
    }

    /** Date de fin d'un congé de nbJours jours ouvrés commençant à debut. */
    public static LocalDate finApresJoursOuvres(LocalDate debut, int nbJours) {
        LocalDate jour = prochainJourOuvre(debut);
        int compte = 1;
        while (compte < nbJours) {
            jour = jour.plusDays(1);
            if (estJourOuvre(jour)) {
                compte++;
            }
        }
        return jour;
    }

    public static String formater(LocalDate date) {
        return date.format(FORMAT_DATE);
    }
}
