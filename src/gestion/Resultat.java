package gestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Réponse renvoyée par chaque action (demande de congé, validation, augmentation...).
 * Elle dit si l'action a réussi ET pourquoi : le programme peut ainsi toujours expliquer
 * à l'employé s'il est apte ou non, et pour quelles raisons.
 */
public class Resultat {

    private final boolean succes;
    private final List<String> messages;
    private final Conge conge;   // la demande concernée, si l'action en a créé ou modifié une

    private Resultat(boolean succes, Conge conge, String message, List<String> details) {
        this.succes = succes;
        this.conge = conge;
        List<String> liste = new ArrayList<>();
        liste.add(message);
        liste.addAll(details);
        this.messages = Collections.unmodifiableList(liste);
    }

    // --- "Fabriques" : méthodes static qui créent un Resultat ---

    public static Resultat succes(String message, String... details) {
        return new Resultat(true, null, message, Arrays.asList(details));
    }

    public static Resultat succes(Conge conge, String message, String... details) {
        return new Resultat(true, conge, message, Arrays.asList(details));
    }

    public static Resultat echec(String message, String... raisons) {
        return new Resultat(false, null, message, Arrays.asList(raisons));
    }

    public static Resultat echec(String message, List<String> raisons) {
        return new Resultat(false, null, message, raisons);
    }

    public boolean estUnSucces() {
        return succes;
    }

    public List<String> getMessages() {
        return messages;
    }

    public Conge getConge() {
        return conge;
    }

    /** Affiche le résultat dans la console : [OK] ou [REFUSÉ], suivi des détails. */
    public void afficher() {
        System.out.println((succes ? "  [OK] " : "  [REFUSÉ] ") + messages.get(0));
        for (int i = 1; i < messages.size(); i++) {
            System.out.println("        - " + messages.get(i));
        }
    }
}
