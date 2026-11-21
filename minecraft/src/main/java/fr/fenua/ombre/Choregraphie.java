package fr.fenua.ombre;

/**
 * Les assassinats mis en scène : où sont la victime et l'Ombre à chaque instant, comment le corps de la victime
 * bascule. Le même calcul sert au serveur (la vraie position de la victime) et au joueur (sa propre course,
 * les poses, la bascule du corps), si bien que tout reste accordé.
 *
 * Repère : A est la position de la victime au départ ; F l'axe horizontal du joueur vers la victime ;
 * S l'axe à droite de F. Une position est { f, s, y } : f le long de F, s le long de S, y en hauteur, depuis A.
 * « pf, py » : la position de départ du joueur dans ce repère (f, y). k va de 0 (début) à 1 (fin).
 */
public final class Choregraphie {
    private Choregraphie() {}

    public static final int DOS = 0, AERIEN = 1, CACHETTE = 2, REBORD = 3, PROJECTION = 4;
    /** Durée de chaque scène, en ticks. */
    public static final int[] DUREE = {26, 22, 26, 26, 30};
    /** À quel moment (de 0 à 1) le coup est porté et la victime meurt. */
    public static final double[] COUP = {0.56, 0.74, 0.86, 0.6, 0.84};
    /** Pour le rebord : à partir de quand la victime tombe librement (le serveur la lâche). */
    public static final double CHUTE_LIBRE = 0.62;

    // ───────────────────────── outils ─────────────────────────

    /** De 0 à 1 entre a et b, en douceur. */
    public static double lisse(double a, double b, double k) {
        if (k <= a) return 0;
        if (k >= b) return 1;
        double u = (k - a) / (b - a);
        return u * u * (3 - 2 * u);
    }

    private static double lerp(double a, double b, double u) { return a + (b - a) * u; }

    // ───────────────────────── la victime ─────────────────────────

    /** Où est la victime : { f, s, y }. */
    public static double[] victime(int scene, double k, double pf, double py) {
        switch (scene) {
            case DOS -> {
                double u = lisse(0.58, 0.86, k);
                return new double[]{0.5 * u, 0, 0};
            }
            case PROJECTION -> {
                // Saisie, puis un arc par-dessus l'épaule de l'Ombre, et la chute sur le dos derrière elle
                double u = lisse(0.26, 0.56, k);
                double rebond = k > 0.56 && k < 0.64 ? Math.sin((k - 0.56) / 0.08 * Math.PI) * 0.18 : 0;
                return new double[]{lerp(0, pf - 1.9, u), 0.35 * Math.sin(Math.PI * u), 1.9 * Math.sin(Math.PI * u) + rebond};
            }
            case AERIEN -> {
                // Écrasé par l'Ombre qui lui tombe dessus : il part en arrière (le long de F)
                double u = lisse(0.48, 0.66, k);
                return new double[]{0.55 * u, 0, 0};
            }
            case CACHETTE -> {
                // Attiré vers la cachette, puis enfoncé dedans
                double u = lisse(0.3, 0.68, k), v = lisse(0.68, 0.9, k);
                return new double[]{lerp(lerp(0, pf + 0.7, u), pf, v), 0, -0.15 * u - 0.75 * v};
            }
            case REBORD -> {
                // Tiré par la cheville, il bascule par-dessus le bord et passe derrière l'Ombre
                double u = lisse(0.34, CHUTE_LIBRE, k);
                return new double[]{lerp(0, pf - 0.9, u), 0, -2.0 * u * u};
            }
            default -> { return new double[]{0, 0, 0}; }
        }
    }

    /**
     * Le corps de la victime : { bascule, lacet }. Bascule en degrés : positif = en avant (face contre terre),
     * négatif = en arrière (sur le dos). Lacet : son orientation par rapport à F (0 = il tourne le dos à l'Ombre).
     */
    public static float[] corps(int scene, double k) {
        switch (scene) {
            case DOS -> {
                double cambre = -16 * lisse(0.22, 0.38, k);
                return new float[]{(float) (k < 0.58 ? cambre : lerp(-16, 90, lisse(0.58, 0.86, k))), 0};
            }
            case PROJECTION -> {
                double u = lisse(0.26, 0.56, k);
                return new float[]{(float) (-90 * u), (float) (180 * u)};
            }
            case CACHETTE -> {
                double u = lisse(0.3, 0.68, k), v = lisse(0.68, 0.9, k);
                return new float[]{(float) (55 * u + 35 * v), 180};
            }
            case REBORD -> {
                double u = lisse(0.34, 1.0, k);
                return new float[]{(float) (175 * u), 180};
            }
            default -> {}
        }
        if (scene == AERIEN) return new float[]{(float) (-90 * lisse(0.48, 0.66, k)), 180};
        return new float[]{0, 0};
    }

    // ───────────────────────── l'Ombre ─────────────────────────

    /** Où va le joueur : { f, s, y }. */
    public static double[] joueur(int scene, double k, double pf, double py) {
        switch (scene) {
            case DOS -> {
                double u = lisse(0, 0.24, k), v = lisse(0.58, 0.86, k);
                return new double[]{lerp(pf, -0.62, u) + 0.35 * v, 0, lerp(py, 0, u)};
            }
            case PROJECTION -> {
                double u = lisse(0, 0.2, k), v = lisse(0.6, 0.76, k);
                return new double[]{lerp(lerp(pf, -0.6, u), pf - 1.1, v), 0, lerp(py, 0, u)};
            }
            case AERIEN -> {
                // Un bond en arc jusque sur la victime, puis à genoux sur sa poitrine
                double u = lisse(0, 0.5, k), v = lisse(0.48, 0.66, k);
                double f = lerp(lerp(pf, -0.25, u), 0.75, v);
                double y = lerp(py, 0.9, u) + 1.1 * Math.sin(Math.PI * u) * (1 - v) - 0.55 * v;
                return new double[]{f, 0, Math.max(0.25, y)};
            }
            default -> { return new double[]{pf, 0, py}; }          // cachette, rebord : on ne bouge pas
        }
    }

    /** Le lacet du joueur par rapport à F (0 = face à la victime). */
    public static float lacetJoueur(int scene, double k) {
        if (scene == PROJECTION) return (float) (180 * lisse(0.3, 0.6, k));
        return 0;
    }
}
