package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.Profil;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Set;

/** Ce que le joueur sait de son profil : sa bourse, ses points, ses compétences (reçus du serveur chaque seconde). */
public final class ProfilClient {
    private ProfilClient() {}

    public static int thalers, points;
    public static final Set<String> COMPETENCES = new HashSet<>();
    /** Les corps déjà fouillés (pour ne plus proposer de les fouiller). */
    public static final Set<Integer> FOUILLES = new HashSet<>();

    public static boolean a(String id) { return COMPETENCES.contains(id); }

    /**
     * Le corps de garde visé : Minecraft ne laisse pas viser un mort, et un corps couché est difficile à viser.
     * On prend celui qui est le plus près de la direction du regard (à moins de 30°), à moins de 4 blocs.
     */
    public static net.minecraft.entity.LivingEntity corpsVise(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return null;
        net.minecraft.util.math.Vec3d oeil = mc.player.getEyePos(), regard = mc.player.getRotationVec(1f);
        net.minecraft.entity.LivingEntity mieux = null;
        double meilleur = Math.cos(Math.toRadians(30));
        for (net.minecraft.entity.LivingEntity l : mc.world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                mc.player.getBoundingBox().expand(5), e -> !e.isAlive() && fr.fenua.ombre.Discretion.estGardeClient(e) && !FOUILLES.contains(e.getId()))) {
            net.minecraft.util.math.Vec3d vers = l.getPos().add(0, 0.3, 0).subtract(oeil);
            if (vers.length() > 4.2) continue;
            double c = vers.normalize().dotProduct(regard);
            if (c > meilleur) { meilleur = c; mieux = l; }
        }
        return mieux;
    }

    private static boolean utiliserAvant;

    /** Chaque tick : le clic droit sur un corps visé le fouille. */
    public static void tick(MinecraftClient mc) {
        boolean u = mc.options.useKey.isPressed();
        if (u && !utiliserAvant && mc.currentScreen == null) {
            net.minecraft.entity.LivingEntity c = corpsVise(mc);
            if (c != null) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new Profil.DemandePayload(Profil.DemandePayload.FOUILLER, c.getId(), 0, ""));
                FOUILLES.add(c.getId());
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        }
        utiliserAvant = u;
    }

    public static void recevoir(Profil.ProfilPayload p) {
        thalers = p.thalers();
        points = p.points();
        COMPETENCES.clear();
        for (String s : p.competences().split(",")) if (!s.isEmpty()) COMPETENCES.add(s);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (p.boutique() >= 0) mc.setScreen(new EcranBoutique(p.boutique()));
        else if (mc.currentScreen instanceof EcranBoutique || mc.currentScreen instanceof EcranCompetences) mc.currentScreen.init(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }
}
