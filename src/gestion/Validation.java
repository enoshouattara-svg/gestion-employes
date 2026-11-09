package gestion;

/**
 * Règles de validation des TEXTES saisis (motifs, noms, langages).
 *
 * Chaque méthode renvoie :
 *   - null            si le texte est correct ;
 *   - un message      qui explique ce qui ne va pas, sinon.
 *
 * Elles sont utilisées à deux endroits :
 *   - dans Main, pour redemander la saisie tant qu'elle est incorrecte ;
 *   - dans les classes métier (Employe, Entreprise...), pour qu'aucune donnée invalide
 *     ne puisse être enregistrée, même sans passer par le menu.
 */
public final class Validation {

    public static final int LONGUEUR_MIN_MOTIF = 3;
    public static final int LONGUEUR_MAX_MOTIF = 100;
    public static final int LONGUEUR_MAX_NOM = 40;

    private static final String VOYELLES = "aeiouyàâäéèêëîïôöùûüÿæœ";

    private Validation() {
    }

    /**
     * Un motif doit être une vraie phrase ou expression :
     *   - entre 3 et 100 caractères ;
     *   - uniquement des lettres, chiffres, espaces et la ponctuation courante ;
     *   - au moins un mot de 2 lettres ou plus, contenant une voyelle ;
     *   - majoritairement des lettres (pas "123456" ni "a1b2c3d4") ;
     *   - pas un même caractère répété ("aaaa", "zzzzz").
     */
    public static String verifierMotif(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return refusMotif("le motif est obligatoire.");
        }
        String motif = texte.trim();
        int lettres = 0;
        int chiffres = 0;
        int autres = 0;
        for (char c : motif.toCharArray()) {
            if (Character.isLetter(c)) {
                lettres++;
            } else if (Character.isDigit(c)) {
                chiffres++;
            } else if (c != ' ') {
                autres++;
                if (".,;:!?'’-()/\"".indexOf(c) < 0) {
                    return refusMotif("le caractère « " + c + " » n'est pas autorisé.");
                }
            }
        }
        if (lettres == 0 && chiffres > 0) {
            return refusMotif("un chiffre ou un nombre n'est pas un motif.");
        }
        if (lettres == 0) {
            return refusMotif("des symboles ne sont pas un motif.");
        }
        if (motif.length() < LONGUEUR_MIN_MOTIF) {
            return refusMotif("le motif est trop court (au moins " + LONGUEUR_MIN_MOTIF + " caractères).");
        }
        if (motif.length() > LONGUEUR_MAX_MOTIF) {
            return refusMotif("le motif est trop long (au plus " + LONGUEUR_MAX_MOTIF + " caractères).");
        }
        if (lettres < chiffres + autres) {
            return refusMotif("le motif contient plus de chiffres ou de symboles que de lettres.");
        }
        if (!contientUnVraiMot(motif) || estUnCaractereRepete(motif) || estDuCharabia(motif)) {
            return refusMotif("« " + motif + " » n'a pas de sens.");
        }
        return null;
    }

    /** Réponses « bidon » souvent tapées pour passer une question sans répondre. */
    private static final java.util.Set<String> MOTS_VIDES_DE_SENS = new java.util.HashSet<>(java.util.Arrays.asList(
            "abc", "abcd", "abcde", "xyz", "azerty", "qwerty", "qsdf", "qsdfgh", "asdf", "wxcv", "aze", "qsd",
            "test", "tests", "toto", "tata", "titi", "tutu", "blabla", "bla", "blablabla", "lol", "mdr", "xxx",
            "rien", "aucun", "aucune", "ras", "ok", "oui", "non", "jsp", "truc", "machin", "bidule", "chose",
            "motif", "raison", "absence", "permission", "conge", "congé", "aaa", "azertyuiop", "hello", "salut"));

    /**
     * Détecte les textes qui ressemblent à des mots mais n'en sont pas :
     *   - réponses bidon (« test », « toto », « rien », « ok »...) ;
     *   - suites de l'alphabet ou du clavier (« abc », « bcde », « azerty ») ;
     *   - charabia : 5 consonnes d'affilée ou presque pas de voyelles (« kjdhfg », « zrtplk »).
     */
    private static boolean estDuCharabia(String texte) {
        String minuscules = texte.toLowerCase();
        String lettresSeules = minuscules.replaceAll("[^\\p{L}]", "");
        if (MOTS_VIDES_DE_SENS.contains(minuscules.trim()) || MOTS_VIDES_DE_SENS.contains(lettresSeules)) {
            return true;
        }
        if (estUneSuiteAlphabetique(lettresSeules) || "azertyuiopqsdfghjklmwxcvbn".contains(lettresSeules)
                || "qwertyuiopasdfghjklzxcvbnm".contains(lettresSeules)) {
            return true;
        }
        int voyelles = 0;
        int consonnesDAffilee = 0;
        for (char c : lettresSeules.toCharArray()) {
            if (VOYELLES.indexOf(c) >= 0) {
                voyelles++;
                consonnesDAffilee = 0;
            } else if (++consonnesDAffilee >= 5) {
                return true;
            }
        }
        return voyelles * 5 < lettresSeules.length();   // moins de 20 % de voyelles
    }

    /** « abc », « bcde », « cba »... : lettres qui se suivent dans l'alphabet. */
    private static boolean estUneSuiteAlphabetique(String lettres) {
        if (lettres.length() < 3) {
            return false;
        }
        int pas = lettres.charAt(1) - lettres.charAt(0);
        if (pas != 1 && pas != -1) {
            return false;
        }
        for (int i = 2; i < lettres.length(); i++) {
            if (lettres.charAt(i) - lettres.charAt(i - 1) != pas) {
                return false;
            }
        }
        return true;
    }

    /** Message ferme, identique pour tous les motifs refusés. */
    private static String refusMotif(String raison) {
        return "RÉPONSE INVALIDE : " + raison
                + "\n     Écrivez un mot ou une phrase qui a du sens (exemple : « rendez-vous médical »).";
    }

    /** Motif facultatif : vide accepté, sinon il doit respecter les mêmes règles que verifierMotif. */
    public static String verifierMotifFacultatif(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return null;
        }
        return verifierMotif(texte);
    }

    /**
     * Nom ou prénom : uniquement des lettres (accents compris), avec éventuellement
     * des espaces, traits d'union ou apostrophes entre les mots (ex. « Jean-Marc », « N'Guessan »).
     */
    public static String verifierNom(String texte, String champ) {
        if (texte == null || texte.trim().isEmpty()) {
            return champ + " est obligatoire.";
        }
        String nom = texte.trim();
        if (nom.length() > LONGUEUR_MAX_NOM) {
            return champ + " est trop long (au plus " + LONGUEUR_MAX_NOM + " caractères).";
        }
        if (!nom.matches("\\p{L}+([ '’-]\\p{L}+)*")) {
            return champ + " ne doit contenir que des lettres (espace, trait d'union ou apostrophe autorisés entre les mots).";
        }
        if (nom.replaceAll("[^\\p{L}]", "").length() < 2) {
            return champ + " doit contenir au moins 2 lettres.";
        }
        if (estUnCaractereRepete(nom)) {
            return champ + " n'a pas de sens (une même lettre répétée).";
        }
        return null;
    }

    /** Langage de programmation : doit commencer par une lettre (ex. Java, C++, C#, Node.js, Python 3). */
    public static String verifierLangage(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return "Le langage est obligatoire.";
        }
        String langage = texte.trim();
        if (langage.length() > 30) {
            return "Le nom du langage est trop long (au plus 30 caractères).";
        }
        if (!langage.matches("\\p{L}[\\p{L}0-9+#. -]*")) {
            return "Langage invalide : il doit commencer par une lettre (exemples : Java, C++, C#, Python).";
        }
        return null;
    }

    /** Pour les classes métier : lève une exception si le texte est invalide. */
    static void exiger(String erreur) {
        if (erreur != null) {
            throw new IllegalArgumentException(erreur);
        }
    }

    private static boolean contientUnVraiMot(String texte) {
        for (String mot : texte.toLowerCase().split("[^\\p{L}]+")) {
            if (mot.length() >= 2) {
                for (char c : mot.toCharArray()) {
                    if (VOYELLES.indexOf(c) >= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean estUnCaractereRepete(String texte) {
        String sansEspaces = texte.toLowerCase().replaceAll("[^\\p{L}0-9]", "");
        return sansEspaces.length() >= 2 && sansEspaces.chars().distinct().count() == 1;
    }
}
