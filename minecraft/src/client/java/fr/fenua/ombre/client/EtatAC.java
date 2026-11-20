package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.EtatPayload;
import fr.fenua.ombre.ac.JournalPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ce que le serveur nous a dit (soupçon des gardes, zone, notoriété, journal) : l'écran s'en sert. */
public final class EtatAC {
    private EtatAC() {}

    /** Numéro de créature → soupçon (1 à 100 ; 100 = il t'a repéré). */
    public static Map<Integer, Integer> soupcons = new HashMap<>();
    public static int zone, notoriete;
    public static List<String> objectifs = new ArrayList<>();
    public static List<Long> vues = new ArrayList<>(), cibles = new ArrayList<>();
    public static List<String> nomsVues = new ArrayList<>();

    public static void recevoir(EtatPayload e) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int i = 0; i < e.gardes().size() && i < e.niveaux().size(); i++) m.put(e.gardes().get(i), e.niveaux().get(i));
        soupcons = m;
        zone = e.zone();
        notoriete = e.notoriete();
        List<Integer> reperes = new ArrayList<>();
        m.forEach((id, n) -> { if (n >= 100) reperes.add(id); });
        Assassin.recevoir(reperes);
    }

    public static void recevoir(JournalPayload j) {
        objectifs = j.objectifs();
        vues = j.vues();
        nomsVues = j.nomsVues();
        cibles = j.cibles();
    }

    /** La couleur d'un soupçon : blanc → jaune → orange, rouge quand il t'a repéré. */
    public static int couleur(int n) {
        if (n >= 100) return 0xFFE0281E;
        if (n >= 45) return mix(0xFFF2C23A, 0xFFF07A1E, (n - 45) / 55f);
        return mix(0xFFE8E8E8, 0xFFF2C23A, n / 45f);
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
