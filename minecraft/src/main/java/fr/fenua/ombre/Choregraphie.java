package fr.fenua.ombre;

/**
 * Les assassinats mis en scène : où sont la victime et l'Ombre à chaque instant, comment le corps de la victime
 * bascule. Le même calcul sert au serveur (la vraie position de la victime) et au joueur (sa propre course,
 * les poses, la bascule du corps), si bien que tout reste accordé.
 *
 * Chaque famille d'assassinat (au sol, aérien, cachette, rebord, en course) a plusieurs scènes ; le serveur
 * en tire une au hasard, jamais deux fois de suite la même.
 *
 * Repère : A est la position de la victime au départ ; F l'axe horizontal du joueur vers la victime ;
 * S l'axe à droite de F. Une position est { f, s, y } : f le long de F, s le long de S, y en hauteur, depuis A.
 * « pf, py » : la position de départ du joueur dans ce repère (f, y). k va de 0 (début) à 1 (fin).
 */
public final class Choregraphie {
    private Choregraphie() {}

    // Les scènes
    public static final int DOS = 0, AERIEN = 1, CACHETTE = 2, REBORD = 3, PROJECTION = 4,
            BAILLON = 5, JARRET = 6, FACE = 7,                 // au sol
            EPAULES = 8, FOUDRE = 9,                           // aériens
            HERBES = 10, CROC = 11,                            // depuis la cachette
            REBORD_LAME = 12, REBORD_VOL = 13,                 // depuis le rebord
            TACLE = 14, PASSAGE = 15, BOND = 16;               // en course
    public static final int NB = 17;

    // Les familles
    public static final int F_SOL = 0, F_AERIEN = 1, F_CACHETTE = 2, F_REBORD = 3, F_COURSE = 4;
    public static final int[] FAMILLE = {F_SOL, F_AERIEN, F_CACHETTE, F_REBORD, F_SOL, F_SOL, F_SOL, F_SOL,
            F_AERIEN, F_AERIEN, F_CACHETTE, F_CACHETTE, F_REBORD, F_REBORD, F_COURSE, F_COURSE, F_COURSE};
    /** Les scènes de chaque famille. */
    public static final int[][] SCENES = {
            {DOS, PROJECTION, BAILLON, JARRET, FACE},
            {AERIEN, EPAULES, FOUDRE},
            {CACHETTE, HERBES, CROC},
            {REBORD, REBORD_LAME, REBORD_VOL},
            {TACLE, PASSAGE, BOND}};
    /** Le nom de chaque geste (affiché après le coup). */
    public static final String[] NOM = {"la lame dans le dos", "le plongeon", "attiré dans la cachette", "basculé du rebord",
            "la projection", "la main sur la bouche", "le coup au jarret", "le face-à-face",
            "sur les épaules", "le coup de foudre", "la lame dans les herbes", "tiré par les pieds",
            "la lame d'en bas", "par-dessus la tête", "le plaquage", "la lame au passage", "le bond"};

    /** Durée de chaque scène, en ticks. */
    public static final int[] DUREE = {26, 22, 26, 26, 30, 30, 28, 28, 22, 24, 26, 28, 22, 26, 24, 22, 24};
    /** À quel moment (de 0 à 1) le coup est porté et la victime meurt. */
    public static final double[] COUP = {0.56, 0.74, 0.86, 0.6, 0.84, 0.55, 0.6, 0.55, 0.62, 0.52, 0.5, 0.8, 0.45, 0.5, 0.62, 0.42, 0.6};
    /** À partir de quand la victime tombe librement (le serveur la lâche) ; -1 : jamais. */
    public static final double[] CHUTE = {-1, -1, -1, 0.62, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0.58, -1, -1, -1};
    /** Quand les corps se heurtent (un bruit sourd) ; -1 : pas de choc. */
    public static final double[] CHOC = {-1, 0.5, -1, -1, 0.56, -1, 0.3, -1, 0.42, 0.45, -1, 0.36, -1, -1, 0.28, -1, 0.44};
    /** Ancien nom, gardé pour le rebord. */
    public static final double CHUTE_LIBRE = CHUTE[REBORD];

    // ───────────────────────── outils ─────────────────────────

    /** De 0 à 1 entre a et b, en douceur. */
    public static double lisse(double a, double b, double k) {
        if (k <= a) return 0;
        if (k >= b) return 1;
        double u = (k - a) / (b - a);
        return u * u * (3 - 2 * u);
    }

    private static double lerp(double a, double b, double u) { return a + (b - a) * u; }
    private static double[] v(double f, double s, double y) { return new double[]{f, s, y}; }
    private static float[] c(double bascule, double lacet) { return new float[]{(float) bascule, (float) lacet}; }

    // ───────────────────────── la victime ─────────────────────────

    /** Où est la victime : { f, s, y }. */
    public static double[] victime(int scene, double k, double pf, double py) {
        switch (scene) {
            case DOS -> {
                return v(0.5 * lisse(0.58, 0.86, k), 0, 0);
            }
            case PROJECTION -> {
                // Saisie, puis un arc par-dessus l'épaule de l'Ombre, et la chute sur le dos derrière elle
                double u = lisse(0.26, 0.56, k);
                double rebond = k > 0.56 && k < 0.64 ? Math.sin((k - 0.56) / 0.08 * Math.PI) * 0.18 : 0;
                return v(lerp(0, pf - 1.9, u), 0.35 * Math.sin(Math.PI * u), 1.9 * Math.sin(Math.PI * u) + rebond);
            }
            case BAILLON -> {
                // Tiré en arrière contre l'Ombre, puis couché doucement sur le dos
                return v(-0.3 * lisse(0.22, 0.38, k), 0, 0);
            }
            case JARRET -> {
                // Le genou plie sous le coup de pied : il tombe à genoux, puis face contre terre
                double g = lisse(0.3, 0.4, k), h = lisse(0.64, 0.86, k);
                return v(0.15 * g + 0.3 * h, 0, -0.3 * g * (1 - h));
            }
            case FACE -> {
                // Retourné d'un coup, frappé de face, repoussé : il tombe à la renverse
                return v(0.2 * lisse(0.5, 0.6, k), 0, 0);
            }
            case AERIEN -> {
                // Écrasé par l'Ombre qui lui tombe dessus : il part en arrière (le long de F)
                return v(0.55 * lisse(0.48, 0.66, k), 0, 0);
            }
            case EPAULES -> {
                // L'Ombre atterrit sur ses épaules et le couche face contre terre
                return v(0.35 * lisse(0.45, 0.7, k), 0, 0);
            }
            case FOUDRE -> {
                // Frappé à la réception : il s'affaisse à genoux, puis sur le côté
                double g = lisse(0.45, 0.58, k), h = lisse(0.66, 0.9, k);
                return v(0.2 * h, 0, -0.3 * g * (1 - h));
            }
            case CACHETTE -> {
                // Attiré vers la cachette, puis enfoncé dedans
                double u = lisse(0.3, 0.68, k), w = lisse(0.68, 0.9, k);
                return v(lerp(lerp(0, pf + 0.7, u), pf, w), 0, -0.15 * u - 0.75 * w);
            }
            case HERBES -> {
                // Il se retourne, la lame le cueille ; il tombe à genoux puis vers l'Ombre, dans les herbes
                double g = lisse(0.52, 0.62, k), h = lisse(0.62, 0.88, k);
                return v(lerp(0, Math.min(0, pf + 2.0), h), 0, -0.3 * g * (1 - h) - 0.45 * h);
            }
            case CROC -> {
                // Les chevilles saisies : il tombe en avant, puis il est traîné à reculons dans la cachette
                double d = lisse(0.4, 0.72, k);
                return v(lerp(0, Math.min(0, pf + 0.9), d), 0, -0.1 * lisse(0.22, 0.38, k) - 0.5 * lisse(0.72, 0.9, k));
            }
            case REBORD -> {
                // Tiré par la cheville, il bascule par-dessus le bord et passe derrière l'Ombre
                double u = lisse(0.34, CHUTE[REBORD], k);
                return v(lerp(0, pf - 0.9, u), 0, -2.0 * u * u);
            }
            case REBORD_LAME -> {
                // Frappé d'en bas, il s'effondre sur le toit, loin du bord
                double g = lisse(0.42, 0.56, k), h = lisse(0.56, 0.85, k);
                return v(0.2 * h, 0, -0.3 * g * (1 - h));
            }
            case REBORD_VOL -> {
                // Empoigné au col et lancé par-dessus la tête de l'Ombre, dans le vide
                double u = lisse(0.3, CHUTE[REBORD_VOL], k);
                return v(lerp(0, pf - 1.6, u), 0, 0.9 * Math.sin(Math.PI * u) - 2.4 * u * u);
            }
            case TACLE -> {
                // Plaqué en pleine course : il vole en avant et s'écrase face contre terre
                return v(1.4 * lisse(0.25, 0.55, k), 0, 0);
            }
            case PASSAGE -> {
                return v(0.15 * lisse(0.42, 0.6, k), 0, 0);
            }
            case BOND -> {
                return v(0.5 * lisse(0.4, 0.62, k), 0, 0);
            }
            default -> { return v(0, 0, 0); }
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
                return c(k < 0.58 ? cambre : lerp(-16, 90, lisse(0.58, 0.86, k)), 0);
            }
            case PROJECTION -> {
                double u = lisse(0.26, 0.56, k);
                return c(-90 * u, 180 * u);
            }
            case BAILLON -> {
                double cambre = -14 * lisse(0.22, 0.38, k);
                return c(lerp(cambre, -90, lisse(0.62, 0.9, k)), 0);
            }
            case JARRET -> {
                double g = lisse(0.3, 0.4, k);
                return c(lerp(8 * g, 90, lisse(0.64, 0.86, k)), 0);
            }
            case FACE -> {
                return c(-90 * lisse(0.6, 0.85, k), 180 * lisse(0.22, 0.4, k));
            }
            case AERIEN -> { return c(-90 * lisse(0.48, 0.66, k), 180); }
            case EPAULES -> { return c(90 * lisse(0.45, 0.7, k), 0); }
            case FOUDRE -> {
                double h = lisse(0.66, 0.9, k);
                return c(lerp(10 * lisse(0.45, 0.58, k), 90, h), -70 * h);
            }
            case CACHETTE -> {
                double u = lisse(0.3, 0.68, k), w = lisse(0.68, 0.9, k);
                return c(55 * u + 35 * w, 180);
            }
            case HERBES -> {
                return c(lerp(10 * lisse(0.52, 0.62, k), 90, lisse(0.62, 0.88, k)), 180 * lisse(0.2, 0.34, k));
            }
            case CROC -> { return c(90 * lisse(0.22, 0.38, k), 0); }
            case REBORD -> { return c(175 * lisse(0.34, 1.0, k), 180); }
            case REBORD_LAME -> {
                return c(lerp(-8 * lisse(0.42, 0.56, k), -90, lisse(0.56, 0.85, k)), 180);
            }
            case REBORD_VOL -> { return c(-200 * lisse(0.3, 1.0, k), 180); }
            case TACLE -> { return c(90 * lisse(0.25, 0.5, k), 0); }
            case PASSAGE -> {
                // Il pivote sous la lame, puis s'écroule
                return c(90 * lisse(0.55, 0.85, k), -220 * lisse(0.4, 0.6, k));
            }
            case BOND -> { return c(90 * lisse(0.4, 0.62, k), 0); }
            default -> { return c(0, 0); }
        }
    }

    // ───────────────────────── l'Ombre ─────────────────────────

    /** Où va le joueur : { f, s, y }. */
    public static double[] joueur(int scene, double k, double pf, double py) {
        switch (scene) {
            case DOS -> {
                double u = lisse(0, 0.24, k), w = lisse(0.58, 0.86, k);
                return v(lerp(pf, -0.62, u) + 0.35 * w, 0, lerp(py, 0, u));
            }
            case PROJECTION -> {
                double u = lisse(0, 0.2, k), w = lisse(0.6, 0.76, k);
                return v(lerp(lerp(pf, -0.6, u), pf - 1.1, w), 0, lerp(py, 0, u));
            }
            case BAILLON -> {
                // Collé à lui, puis un pas de côté pour le coucher et poser un genou à côté
                double u = lisse(0, 0.2, k), w = lisse(0.62, 0.9, k);
                return v(lerp(pf, -0.75, u) - 0.25 * lisse(0.22, 0.38, k) - 0.1 * w, 0.8 * w, lerp(py, 0, u));
            }
            case JARRET -> {
                double u = lisse(0, 0.22, k);
                return v(lerp(pf, -0.7, u) + 0.25 * lisse(0.4, 0.5, k), 0, lerp(py, 0, u));
            }
            case FACE -> {
                double u = lisse(0, 0.2, k);
                return v(lerp(pf, -0.75, u) + 0.15 * lisse(0.5, 0.6, k), 0, lerp(py, 0, u));
            }
            case AERIEN -> {
                // Un bond en arc jusque sur la victime, puis à genoux sur sa poitrine
                double u = lisse(0, 0.5, k), w = lisse(0.48, 0.66, k);
                double f = lerp(lerp(pf, -0.25, u), 0.75, w);
                double y = lerp(py, 0.9, u) + 1.1 * Math.sin(Math.PI * u) * (1 - w) - 0.55 * w;
                return v(f, 0, Math.max(0.25, y));
            }
            case EPAULES -> {
                // Les pieds sur ses épaules, puis on l'accompagne jusqu'au sol, un genou dans son dos
                double u = lisse(0, 0.42, k), w = lisse(0.45, 0.7, k);
                double f = lerp(lerp(pf, -0.1, u), 0.9, w);
                double y = lerp(lerp(py, 1.55, u) + 0.9 * Math.sin(Math.PI * u), 0.35, w);
                return v(f, 0, y);
            }
            case FOUDRE -> {
                // On tombe droit derrière lui, la lame en avant
                double u = lisse(0, 0.45, k);
                return v(lerp(pf, -0.45, u), 0, Math.max(0, lerp(py, 0, u) + 0.6 * Math.sin(Math.PI * u)));
            }
            case HERBES -> {
                // On sort de la cachette, on frappe, on y rentre avec lui
                double u = lisse(0.1, 0.3, k), w = lisse(0.6, 0.85, k);
                return v(lerp(lerp(pf, Math.max(pf, -0.8), u), pf, w), 0, py);
            }
            case TACLE -> {
                double u = lisse(0, 0.25, k), d = lisse(0.25, 0.55, k);
                return v(lerp(lerp(pf, -0.55, u), 1.0, d), 0, lerp(py, 0, u) + 0.45 * Math.sin(Math.PI * d) * (1 - lisse(0.5, 0.6, k)) + 0.35 * lisse(0.5, 0.6, k));
            }
            case PASSAGE -> {
                // On ne s'arrête pas : on le croise, la lame tendue, et on file
                return v(lerp(pf, 2.6, k), 0.8 * lisse(0, 0.3, k) * (1 - 0.4 * lisse(0.6, 1, k)), lerp(py, 0, lisse(0, 0.2, k)));
            }
            case BOND -> {
                double u = lisse(0, 0.45, k), w = lisse(0.45, 0.65, k);
                double f = lerp(lerp(pf, 0.2, u), 0.8, w);
                double y = lerp(lerp(py, 0.9, u) + 0.9 * Math.sin(Math.PI * u), 0.35, w);
                return v(f, 0, y);
            }
            default -> { return v(pf, 0, py); }          // cachette, croc, rebords : on ne bouge pas
        }
    }

    /** Le lacet du joueur par rapport à F (0 = face à la victime). */
    public static float lacetJoueur(int scene, double k) {
        if (scene == PROJECTION) return (float) (180 * lisse(0.3, 0.6, k));
        if (scene == PASSAGE) return (float) (-25 * lisse(0.3, 0.5, k));
        if (scene == BAILLON) return (float) (-40 * lisse(0.62, 0.9, k));
        return 0;
    }

    /** Le joueur reste-t-il suspendu au rebord pendant la scène ? */
    public static boolean suspendu(int scene) { return FAMILLE[scene] == F_REBORD; }
}
