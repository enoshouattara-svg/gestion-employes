package application;

import gestion.Comptable;
import gestion.Conge;
import gestion.Developpeur;
import gestion.DirecteurGeneral;
import gestion.DonneesDemo;
import gestion.Employe;
import gestion.Entreprise;
import gestion.Niveau;
import gestion.ResponsableRH;
import gestion.Resultat;
import gestion.TypeConge;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

/**
 * Démonstration automatique : rejoue un scénario complet, sans aucune saisie au clavier.
 * Pratique pour présenter le projet ou vérifier que tout fonctionne.
 *
 * Lancement : Eclipse > clic droit sur ce fichier > Run As > Java Application
 *         ou : java -cp out application.DemoAutomatique
 */
public final class DemoAutomatique {

    private static int numeroEtape = 0;

    private DemoAutomatique() {
    }

    public static void main(String[] args) {
        Entreprise entreprise = DonneesDemo.creerEntreprise();
        LocalDate aujourdhui = LocalDate.now();

        DirecteurGeneral kone = entreprise.getDirecteurGeneral();
        ResponsableRH diallo = (ResponsableRH) entreprise.trouverParNom("Diallo");
        Comptable traore = (Comptable) entreprise.trouverParNom("Traoré");
        Developpeur kouassi = (Developpeur) entreprise.trouverParNom("Kouassi");
        Developpeur bamba = (Developpeur) entreprise.trouverParNom("Bamba");
        Developpeur yao = (Developpeur) entreprise.trouverParNom("Yao");

        // ------------------------------------------------------------------
        etape("Le personnel de " + entreprise.getNom());
        for (Employe employe : entreprise.getEmployes()) {
            System.out.println("  " + employe);
        }

        // ------------------------------------------------------------------
        etape("Calcul des salaires (consulté par le comptable)");
        entreprise.afficherSalaires(traore);
        action("Détail de la fiche de paie de Kouassi (développeur senior)");
        kouassi.afficherFicheDePaie();
        action("Détail de la fiche de paie de Koné (directeur général)");
        kone.afficherFicheDePaie();

        // ------------------------------------------------------------------
        etape("Droits d'accès : qui a le droit de faire quoi ?");
        action("Yao (développeur) essaie de consulter les salaires de tout le personnel");
        entreprise.afficherSalaires(yao).afficher();
        action("Yao vérifie ses droits et son aptitude aux congés");
        yao.afficherDroitsEtAptitudes();

        // ------------------------------------------------------------------
        etape("Demandes de congé : l'éligibilité est vérifiée automatiquement");
        LocalDate lundiDans10Jours = prochainLundi(aujourdhui.plusDays(10));

        action("Yao (en période d'essai) demande un congé payé");
        entreprise.demanderConge(yao, TypeConge.PAYE, lundiDans10Jours, lundiDans10Jours.plusDays(4), "Vacances").afficher();

        action("Bamba (solde épuisé) demande un congé payé");
        entreprise.demanderConge(bamba, TypeConge.PAYE, lundiDans10Jours, lundiDans10Jours.plusDays(4), "Vacances").afficher();

        action("Bamba demande à la place un congé exceptionnel de 2 jours pour un mariage");
        LocalDate debutMariage = Conge.prochainJourOuvre(aujourdhui.plusDays(3));
        Resultat mariage = entreprise.demanderConge(bamba, TypeConge.EXCEPTIONNEL, debutMariage,
                Conge.finApresJoursOuvres(debutMariage, 2), "Mariage");
        mariage.afficher();

        action("Yao demande un congé sans solde qui commence dans 2 jours");
        LocalDate bientot = Conge.prochainJourOuvre(aujourdhui.plusDays(2));
        entreprise.demanderConge(yao, TypeConge.SANS_SOLDE, bientot, Conge.finApresJoursOuvres(bientot, 2), "Déménagement").afficher();

        action("Kouassi demande un congé payé juste avant sa mise en production");
        LocalDate pendantLeGel = Conge.prochainJourOuvre(kouassi.getDateMiseEnProduction().minusDays(2));
        entreprise.demanderConge(kouassi, TypeConge.PAYE, pendantLeGel, Conge.finApresJoursOuvres(pendantLeGel, 3), "Repos").afficher();

        action("Kouassi redemande un congé payé, après la livraison");
        LocalDate lundiDans30Jours = prochainLundi(aujourdhui.plusDays(30));
        Resultat congeKouassi = entreprise.demanderConge(kouassi, TypeConge.PAYE, lundiDans30Jours,
                lundiDans30Jours.plusDays(4), "Vacances en famille");
        congeKouassi.afficher();

        action("Traoré (comptable) demande un congé payé en décembre");
        LocalDate decembre = LocalDate.of(aujourdhui.getYear(), Month.DECEMBER, 8);
        if (decembre.isBefore(aujourdhui.plusDays(Employe.PREAVIS_CONGE_JOURS))) {
            decembre = decembre.plusYears(1);
        }
        entreprise.demanderConge(traore, TypeConge.PAYE, decembre, decembre.plusDays(4), "Fêtes de fin d'année").afficher();

        // ------------------------------------------------------------------
        etape("Circuit de validation des congés");
        action("Yao essaie de valider la demande de Kouassi");
        entreprise.traiterDemande(yao, congeKouassi.getConge(), true, "").afficher();

        action("Diallo (RH) consulte les demandes en attente");
        Conge demandeTraore = null;
        for (Conge conge : entreprise.getDemandesATraiter(diallo)) {
            System.out.println("  " + conge);
            if (conge.getDemandeur() == traore) {
                demandeTraore = conge;
            }
        }

        action("Diallo accepte les demandes de Kouassi et de Bamba");
        entreprise.traiterDemande(diallo, congeKouassi.getConge(), true, "").afficher();
        entreprise.traiterDemande(diallo, mariage.getConge(), true, "").afficher();

        if (demandeTraore != null) {
            action("Diallo refuse la demande de Traoré... sans donner de motif");
            entreprise.traiterDemande(diallo, demandeTraore, false, "").afficher();
            action("Diallo refuse en donnant un motif");
            entreprise.traiterDemande(diallo, demandeTraore, false, "audit fiscal prévu cette semaine-là").afficher();
        }

        action("Bamba demande un congé sans solde pendant la semaine de congé de Kouassi");
        entreprise.demanderConge(bamba, TypeConge.SANS_SOLDE, lundiDans30Jours, lundiDans30Jours.plusDays(2),
                "Projet personnel").afficher();

        action("Diallo (RH) pose ses propres congés");
        LocalDate lundiDans40Jours = prochainLundi(aujourdhui.plusDays(40));
        Resultat congeDiallo = entreprise.demanderConge(diallo, TypeConge.PAYE, lundiDans40Jours,
                lundiDans40Jours.plusDays(2), "Voyage");
        congeDiallo.afficher();

        action("Diallo essaie de valider son propre congé");
        entreprise.traiterDemande(diallo, congeDiallo.getConge(), true, "").afficher();

        action("Koné (DG) valide le congé de Diallo");
        entreprise.traiterDemande(kone, congeDiallo.getConge(), true, "").afficher();

        action("Koné (DG) veut partir la même semaine, mais son intérimaire Diallo sera en congé");
        entreprise.demanderConge(kone, TypeConge.PAYE, lundiDans40Jours, lundiDans40Jours.plusDays(1), "Séminaire").afficher();

        action("Koné désigne Traoré comme intérimaire et redemande");
        kone.setInterim(traore);
        entreprise.demanderConge(kone, TypeConge.PAYE, lundiDans40Jours, lundiDans40Jours.plusDays(1), "Séminaire").afficher();

        // ------------------------------------------------------------------
        etape("Permissions d'absence (quelques heures)");
        action("Yao demande 3 h pour un rendez-vous médical");
        yao.prendrePermission(3, "rendez-vous médical").afficher();
        action("Yao demande 6 h pour des démarches administratives");
        yao.prendrePermission(6, "démarches administratives").afficher();

        // ------------------------------------------------------------------
        etape("Augmentations et promotions (réservées au DG)");
        action("Diallo (RH) essaie d'augmenter Bamba");
        entreprise.augmenterSalaire(diallo, bamba, 10).afficher();
        action("Koné (DG) augmente Bamba de 10 %");
        entreprise.augmenterSalaire(kone, bamba, 10).afficher();
        action("Koné essaie de s'augmenter de 15 %");
        entreprise.augmenterSalaire(kone, kone, 15).afficher();
        action("Koné veut promouvoir Yao (encore en période d'essai)");
        entreprise.promouvoir(kone, yao).afficher();
        action("Koné promeut Bamba");
        entreprise.promouvoir(kone, bamba).afficher();

        // ------------------------------------------------------------------
        etape("Embauche d'un nouveau développeur par la RH");
        action("Traoré (comptable) essaie d'embaucher");
        Developpeur coulibaly = new Developpeur("Coulibaly", "Awa", 450_000, aujourdhui, Niveau.JUNIOR, "Kotlin");
        entreprise.embaucher(traore, coulibaly).afficher();
        action("Diallo (RH) embauche");
        entreprise.embaucher(diallo, coulibaly).afficher();

        // ------------------------------------------------------------------
        etape("Tableau de bord de la direction");
        entreprise.afficherStatistiques(kone);
        action("Historique de tous les congés (consulté par la RH)");
        entreprise.afficherTousLesConges(diallo);
        System.out.println();
    }

    private static LocalDate prochainLundi(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private static void etape(String titre) {
        numeroEtape++;
        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println("  ÉTAPE " + numeroEtape + " : " + titre.toUpperCase());
        System.out.println("=".repeat(78));
    }

    private static void action(String description) {
        System.out.println();
        System.out.println("> " + description);
    }
}
