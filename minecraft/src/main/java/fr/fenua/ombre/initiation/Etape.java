package fr.fenua.ombre.initiation;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Une étape de l'initiation : un titre, quelques lignes d'explication, un but (pour la boussole),
 * ce qui la déclenche au début (donner un objet, faire venir un garde), ce qui la réussit,
 * et, pour les étapes de discrétion, ce qui la fait rater (on recommence au début de la cour).
 */
public final class Etape {
    final int cour;
    final String titre;
    final String[] lignes;
    int[] but;                       // position locale du but (losange de la boussole), ou null
    Consumer<Ctx> debut = c -> {};
    Predicate<Ctx> reussie = c -> false;
    Predicate<Ctx> ratee = null;
    int retour = -1;                 // en cas d'échec : revenir à cette étape (sinon on reste à celle-ci)
    boolean ouvre;                   // réussie : la grille au bout de la cour s'ouvre

    Etape(int cour, String titre, String... lignes) { this.cour = cour; this.titre = titre; this.lignes = lignes; }

    Etape but(int x, int y, int z) { but = new int[]{x, y, z}; return this; }
    Etape debut(Consumer<Ctx> d) { debut = d; return this; }
    Etape si(Predicate<Ctx> r) { reussie = r; return this; }
    Etape rate(Predicate<Ctx> r) { ratee = r; return this; }
    Etape retour(int e) { retour = e; return this; }
    Etape ouvre() { ouvre = true; return this; }
}
