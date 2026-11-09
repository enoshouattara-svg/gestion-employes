package gestion;

import java.time.LocalDate;
import java.time.Month;

/**
 * Crée une entreprise de démonstration avec les 4 types d'employés.
 * Les dates sont calculées à partir d'aujourd'hui : le programme fonctionne quel que soit le jour.
 *
 * Profils choisis pour tester chaque cas :
 *  - KOUASSI : développeur senior, une mise en production arrive bientôt (gel des congés)
 *  - BAMBA   : développeur confirmé qui a déjà pris TOUS ses congés payés (solde = 0)
 *  - YAO     : développeur junior embauché il y a 1 mois (en période d'essai)
 *  - TRAORÉ  : comptable, avec une demande de congé en attente
 */
public final class DonneesDemo {

    private DonneesDemo() {
        // classe utilitaire : on ne l'instancie pas
    }

    public static Entreprise creerEntreprise() {
        LocalDate aujourdhui = LocalDate.now();
        Entreprise entreprise = new Entreprise("TechSoft Solutions");

        DirecteurGeneral kone = new DirecteurGeneral("Koné", "Mariam", 1_500_000, aujourdhui.minusYears(12).minusMonths(4));
        kone.setBeneficeAnnuel(180_000_000);

        ResponsableRH diallo = new ResponsableRH("Diallo", "Aïcha", 650_000, aujourdhui.minusYears(6).minusMonths(2));
        diallo.enregistrerRecrutement();

        Comptable traore = new Comptable("Traoré", "Seydou", 600_000, aujourdhui.minusYears(4));

        Developpeur kouassi = new Developpeur("Kouassi", "Jean-Marc", 900_000,
                aujourdhui.minusYears(8).minusMonths(7), Niveau.SENIOR, "Java");
        kouassi.setHeuresSupplementaires(12);
        kouassi.setProjetsLivres(1);
        kouassi.setDateMiseEnProduction(aujourdhui.plusDays(20));

        Developpeur bamba = new Developpeur("Bamba", "Fatou", 700_000,
                aujourdhui.minusYears(3).minusMonths(1), Niveau.CONFIRME, "Python");
        bamba.setHeuresSupplementaires(4);

        Developpeur yao = new Developpeur("Yao", "Kevin", 400_000, aujourdhui.minusMonths(1), Niveau.JUNIOR, "JavaScript");

        kone.setInterim(diallo);

        entreprise.ajouterEmploye(kone);
        entreprise.ajouterEmploye(diallo);
        entreprise.ajouterEmploye(traore);
        entreprise.ajouterEmploye(kouassi);
        entreprise.ajouterEmploye(bamba);
        entreprise.ajouterEmploye(yao);

        // Bamba a déjà posé tous ses congés payés il y a 5 mois -> solde à 0
        entreprise.enregistrerCongePasse(bamba, TypeConge.PAYE, Conge.prochainJourOuvre(aujourdhui.minusMonths(5)),
                bamba.getSoldeConges(), diallo);

        // Traoré a une demande en attente (hors décembre, mois de clôture)
        LocalDate debut = Conge.prochainJourOuvre(aujourdhui.plusDays(14));
        if (debut.getMonth() == Month.DECEMBER) {
            debut = Conge.prochainJourOuvre(LocalDate.of(debut.getYear() + 1, Month.JANUARY, 10));
        }
        entreprise.demanderConge(traore, TypeConge.PAYE, debut, Conge.finApresJoursOuvres(debut, 3), "Raisons familiales");

        return entreprise;
    }
}
