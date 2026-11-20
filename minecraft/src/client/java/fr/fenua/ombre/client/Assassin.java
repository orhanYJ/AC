package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import fr.fenua.ombre.Assassinat;
import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La cible d'assassinat, comme dans Assassin's Creed :
 * chaque image, on cherche le garde qu'on peut tuer maintenant ; il est entouré de rouge
 * (voir mixin/ContourMixin) et une invite s'affiche sous le viseur. La touche R l'assassine.
 */
public final class Assassin {
    private Assassin() {}

    /** Les créatures qui ont repéré le joueur (envoyées par le serveur). */
    private static Set<Integer> reperes = new HashSet<>();
    @Nullable private static Entity cible;
    @Nullable private static String type;

    public static final int ROUGE = 0xE01B1B;

    public static boolean estCible(Entity e) { return e != null && e == cible; }

    public static void recevoir(List<Integer> ids) { reperes = new HashSet<>(ids); }

    /** Chaque tick : choisir la meilleure cible. */
    public static void tick(MinecraftClient client) {
        cible = null;
        type = null;
        ClientPlayerEntity p = client.player;
        if (p == null || p.isSpectator() || client.world == null || !Assassinat.aLaLame(p)) return;
        float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double fx = -MathHelper.sin(yaw), fz = MathHelper.cos(yaw);
        double meilleur = Double.MAX_VALUE;
        boolean cache = Discretion.dansCachette(p);
        for (Entity e : client.world.getOtherEntities(p, p.getBoundingBox().expand(4, 10, 4), Assassinat::visable)) {
            LivingEntity v = (LivingEntity) e;
            String t = Assassinat.typePossible(p, v, reperes.contains(e.getId()), cache);
            if (t == null || !p.canSee(v)) continue;
            double dx = v.getX() - p.getX(), dz = v.getZ() - p.getZ();
            double h = Math.sqrt(dx * dx + dz * dz);
            // Au sol, il faut l'avoir devant soi ; en l'air ou au rebord, on prend le plus proche
            double face = h > 0.01 ? (dx * fx + dz * fz) / h : 1;
            if (t.equals("Assassinat") && face < 0.2) continue;
            double score = h - face * 1.5;
            if (score < meilleur) { meilleur = score; cible = e; type = t; }
        }
    }

    /** Touche R. */
    public static void frapper() {
        if (cible != null) ClientPlayNetworking.send(new ActionPayload(ActionPayload.ASSASSINER, cible.getId()));
    }

    /** L'invite sous le viseur, et la paille devant les yeux quand on est dans une meule. */
    public static void dessiner(DrawContext ctx, MinecraftClient client) {
        if (client.player != null && client.world != null && client.options.getPerspective().isFirstPerson()) {
            int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
            var monde = client.world;
            BlockPos yeux = BlockPos.ofFloored(client.player.getEyePos()), pieds = client.player.getBlockPos();
            String cache = null;
            if (monde.getBlockState(yeux).isOf(OmbreBlocs.MEULE)) { ctx.fill(0, 0, w, h, 0x99B8902C); cache = "§6Caché dans la meule"; }
            else if (monde.getBlockState(pieds).isOf(OmbreBlocs.CABINE)) { ctx.fill(0, 0, w, h, 0x88701818); cache = "§cCaché derrière le rideau"; }
            else if (monde.getBlockState(pieds).isOf(OmbreBlocs.PUITS)) { ctx.fill(0, h * 2 / 3, w, h, 0x66204060); cache = "§bCaché dans le puits"; }
            else if (Discretion.dansCachette(client.player)) cache = "§aCaché";
            if (cache != null) ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal(cache), w / 2, h - 60, 0xFFFFFF);
        }
        if (cible == null || type == null || client.options.hudHidden) return;
        String touche = OmbreClient.toucheAssassiner();
        Text t = Text.literal("§c✦ §f[" + touche + "] §c" + type + (cible instanceof MobEntity && cible.hasCustomName()
                ? " §7— " + cible.getName().getString() : ""));
        int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
        ctx.drawCenteredTextWithShadow(client.textRenderer, t, w / 2, h / 2 + 26, 0xFFFFFF);
    }
}
