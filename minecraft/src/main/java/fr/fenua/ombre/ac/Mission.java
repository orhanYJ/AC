package fr.fenua.ombre.ac;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Le journal de la mission de Prague (affiché à gauche de l'écran). Chaque objectif est retenu
 * par une étiquette sur le joueur (elle est sauvegardée avec lui).
 */
public final class Mission {
    private Mission() {}

    public static final String ACTIVE = "ombre_mission_prague";
    public static final String[][] OBJECTIFS = {
            {"ombre_m_tycho", "Parler à Tycho Brahe"},
            {"ombre_m_kepler", "Parler à Johannes Kepler"},
            {"ombre_m_ottokar", "Éliminer Ottokar von Rabenstein"},
            {"ombre_m_kragl", "Éliminer Melchior Kragl"},
            {"ombre_m_vue", "Facultatif : synchroniser un point de vue"},
            {"ombre_m_avis", "Facultatif : arracher un avis de recherche"},
    };

    public static List<String> lignes(ServerPlayerEntity p) {
        List<String> l = new ArrayList<>();
        for (String[] o : OBJECTIFS) l.add((p.getCommandTags().contains(o[0]) ? "1" : "0") + o[1]);
        int n = Defis.plumes(p, true);
        l.add((n >= Defis.PLUMES ? "1" : "0") + "Facultatif : plumes de corbeau (" + n + "/" + Defis.PLUMES + ")");
        l.add((p.getCommandTags().contains("ombre_course_finie") ? "1" : "0") + "Facultatif : la course des toits");
        return l;
    }

    public static void marquer(ServerPlayerEntity p, String tag) {
        if (!p.getCommandTags().contains(ACTIVE) || !p.addCommandTag(tag)) return;
        for (String[] o : OBJECTIFS) if (o[0].equals(tag)) p.sendMessage(Text.literal("§a✔ " + o[1]), true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.4f);
        if (p.getCommandTags().contains("ombre_m_ottokar") && p.getCommandTags().contains("ombre_m_kragl") && p.addCommandTag("ombre_m_fin")) epilogue(p);
    }

    /** La fin : ce que l'histoire a retenu. */
    private static void epilogue(ServerPlayerEntity p) {
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6La Confrérie du Corbeau est tombée")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Prague, 1600")));
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
        String[] l = {
                "§6— Épilogue —",
                "§7Tycho Brahe meurt à Prague en octobre 1601. Johannes Kepler hérite de ses registres d'observations.",
                "§7Grâce à eux, Kepler montre en 1609 que les planètes tournent autour du Soleil en suivant des ellipses.",
                "§7Rodolphe II règne à Prague jusqu'en 1611 ; sa cour fait de la ville un carrefour des arts et des sciences.",
                "§7La Confrérie du Corbeau, elle, n'a jamais existé : c'est ton histoire.",
        };
        for (String s : l) p.sendMessage(Text.literal(s), false);
    }
}
