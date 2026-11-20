package fr.fenua.ombre.ac;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * La notoriété (0 à 100), comme dans Assassin's Creed II : chaque alarme la fait monter,
 * un innocent tué la fait bondir. Au-delà de 75, tu es « recherché » : les gardes te remarquent
 * plus vite et de plus loin. On la fait baisser en arrachant les avis de recherche collés
 * sur les murs, ou en se faisant oublier. Elle est gardée dans le tableau des scores du monde.
 */
public final class Notoriete {
    private Notoriete() {}

    public static final int RECHERCHE = 75;
    private static final String NOM = "ombre_notoriete";

    private static ScoreboardObjective objectif(Scoreboard sb) {
        ScoreboardObjective o = sb.getNullableObjective(NOM);
        if (o == null) o = sb.addObjective(NOM, ScoreboardCriterion.DUMMY, Text.literal("Notoriété"),
                ScoreboardCriterion.RenderType.INTEGER, true, null);
        return o;
    }

    public static int get(PlayerEntity p) {
        if (p.getWorld().isClient()) return 0;
        Scoreboard sb = p.getWorld().getScoreboard();
        ScoreboardObjective o = sb.getNullableObjective(NOM);
        if (o == null) return 0;
        var s = sb.getScore(p, o);
        return s == null ? 0 : s.getScore();
    }

    public static void ajouter(ServerPlayerEntity p, int d, String raison) {
        Scoreboard sb = p.getWorld().getScoreboard();
        ScoreAccess s = sb.getOrCreateScore(p, objectif(sb));
        int avant = s.getScore(), apres = Math.max(0, Math.min(100, avant + d));
        s.setScore(apres);
        if (raison != null && apres != avant)
            p.sendMessage(Text.literal((d > 0 ? "§c▲ Notoriété : " : "§a▼ Notoriété : ") + raison), true);
        if (avant < RECHERCHE && apres >= RECHERCHE)
            p.sendMessage(Text.literal("§4§lRECHERCHÉ§r §c— les gardes te reconnaissent de loin. Arrache les avis de recherche."), false);
    }

    /** Le facteur de discrétion des gardes face à un joueur connu : de 1 (inconnu) à 1,8 (recherché). */
    public static double facteur(PlayerEntity p) { return 1 + get(p) / 100.0 * 0.8; }
}
