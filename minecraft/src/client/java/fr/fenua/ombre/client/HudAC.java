package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.Zones;
import fr.fenua.ombre.bloc.Mobilier;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * L'écran façon Assassin's Creed :
 *  - la boussole en haut (points cardinaux, cibles dorées avec leur distance, points de vue, gardes en alerte) ;
 *  - la barre de zone interdite juste dessous (jaune : tout près ; rouge : dedans) ;
 *  - les flèches de détection autour du viseur, qui se remplissent du blanc au jaune puis au rouge ;
 *  - la notoriété et le journal de mission en haut à gauche ;
 *  - l'invite du saut de la foi quand une meule ou l'eau attend en contrebas ;
 *  - dans le monde, un « ? » ou un « ! » au-dessus de la tête des gardes qui te soupçonnent.
 */
public final class HudAC {
    private HudAC() {}

    private static final int LARGEUR_BOUSSOLE = 220, CHAMP = 100;   // la boussole couvre ±100°
    private static boolean sautDeLaFoi;

    // ───────────── chaque tick ─────────────

    public static void tick(MinecraftClient mc) {
        sautDeLaFoi = mc.player != null && sautPossible(mc.player);
    }

    /** Debout au bord d'un vide, une meule (ou l'eau, du foin, un auvent) au moins 5 blocs plus bas devant soi. */
    private static boolean sautPossible(ClientPlayerEntity p) {
        if (!p.isOnGround() || p.isSpectator()) return false;
        float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double fx = -MathHelper.sin(yaw), fz = MathHelper.cos(yaw);
        var w = p.getWorld();
        for (double d = 0.8; d <= 3.2; d += 0.6) {
            BlockPos devant = BlockPos.ofFloored(p.getX() + fx * d, p.getY() + 0.2, p.getZ() + fz * d);
            if (!w.getBlockState(devant).getCollisionShape(w, devant).isEmpty()) return false;   // un mur, pas un vide
            for (int k = 1; k <= 48; k++) {
                BlockPos b = devant.down(k);
                BlockState s = w.getBlockState(b);
                if (s.isAir()) continue;
                if (k < 5) break;
                if (moelleux(s) || s.getFluidState().isIn(FluidTags.WATER)) return true;
                break;
            }
        }
        return false;
    }

    private static boolean moelleux(BlockState s) {
        return s.isOf(OmbreBlocs.MEULE) || s.isOf(OmbreBlocs.CHARRETTE) || s.isOf(Blocks.HAY_BLOCK) || s.isIn(BlockTags.LEAVES)
                || s.getBlock() instanceof Mobilier.Auvent;
    }

    // ───────────── l'écran ─────────────

    public static void dessiner(DrawContext ctx, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        long t = mc.world.getTime();
        float pulse = 0.5f + 0.5f * MathHelper.sin((t + mc.getRenderTickCounter().getTickDelta(true)) * 0.35f);

        // La zone interdite : un voile rouge sur les bords de l'écran quand on est dedans
        if (EtatAC.zone == Zones.DEDANS) {
            int a = (int) (40 + 50 * pulse) << 24;
            ctx.fillGradient(0, 0, w, h / 5, a | 0x8A0000, 0x008A0000);
            ctx.fillGradient(0, h - h / 5, w, h, 0x008A0000, a | 0x8A0000);
        }

        boussole(ctx, mc, p, tr, w, pulse);
        zone(ctx, tr, w, pulse);
        fleches(ctx, mc, p, w, h, pulse);
        notorieteEtJournal(ctx, tr, pulse);

        if (sautDeLaFoi && mc.currentScreen == null)
            ctx.drawCenteredTextWithShadow(tr, Text.literal("§6⬇ Saut de la foi §7— saute dans le vide"), w / 2, h / 2 + 40, 0xFFFFFF);
    }

    /** L'angle d'un point par rapport au regard, en degrés (négatif à gauche, positif à droite). */
    private static float angle(ClientPlayerEntity p, double x, double z) {
        double dx = x - p.getX(), dz = z - p.getZ();
        float cap = (float) (MathHelper.atan2(-dx, dz) * MathHelper.DEGREES_PER_RADIAN);
        return MathHelper.wrapDegrees(cap - p.getYaw());
    }

    private static void boussole(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p, TextRenderer tr, int w, float pulse) {
        int x0 = w / 2 - LARGEUR_BOUSSOLE / 2, x1 = x0 + LARGEUR_BOUSSOLE, y = 5, cx = w / 2;
        // Le fond, qui s'estompe vers les bouts
        for (int i = 0; i < 6; i++) {
            int a = (i + 1) * 14 << 24;
            ctx.fill(x0 + i * 6, y, x0 + i * 6 + 6, y + 13, a);
            ctx.fill(x1 - i * 6 - 6, y, x1 - i * 6, y + 13, a);
        }
        ctx.fill(x0 + 36, y, x1 - 36, y + 13, 0x60000000);
        ctx.fill(x0 + 10, y + 13, x1 - 10, y + 14, 0x50FFFFFF);
        // Graduations et points cardinaux (yaw 0 = sud, 90 = ouest, 180 = nord, -90 = est)
        String[] noms = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
        for (int deg = 0; deg < 360; deg += 15) {
            float rel = MathHelper.wrapDegrees(deg - p.getYaw());
            if (Math.abs(rel) > CHAMP) continue;
            int x = cx + Math.round(rel / CHAMP * LARGEUR_BOUSSOLE / 2);
            if (deg % 45 == 0) {
                String n = noms[deg / 45];
                ctx.drawCenteredTextWithShadow(tr, Text.literal(n), x, y + 3, n.equals("N") ? 0xFFE04040 : n.length() == 1 ? 0xFFFFFFFF : 0xFFB0B0B0);
            } else ctx.fill(x, y + 9, x + 1, y + 12, 0x90FFFFFF);
        }
        // Points de vue synchronisés
        for (Long l : EtatAC.vues) {
            BlockPos b = BlockPos.fromLong(l);
            marque(ctx, tr, p, b.getX() + 0.5, b.getZ() + 0.5, "✦", 0xFF9FD8FF, null, cx, y);
        }
        // Gardes qui te soupçonnent
        for (Map.Entry<Integer, Integer> e : EtatAC.soupcons.entrySet()) {
            Entity g = mc.world.getEntityById(e.getKey());
            if (g == null || e.getValue() < 45) continue;
            marque(ctx, tr, p, g.getX(), g.getZ(), e.getValue() >= 100 ? "▼" : "▽", EtatAC.couleur(e.getValue()), null, cx, y);
        }
        // Marqueurs de mission : losange doré et distance
        for (Long l : EtatAC.cibles) {
            BlockPos b = BlockPos.fromLong(l);
            double d = Math.sqrt(p.squaredDistanceTo(Vec3d.ofBottomCenter(b)));
            marque(ctx, tr, p, b.getX() + 0.5, b.getZ() + 0.5, "◆", 0xFFF2C23A, (int) d + " m", cx, y);
        }
        ctx.fill(cx, y - 2, cx + 1, y, 0xFFFFFFFF);   // le repère du regard
    }

    private static void marque(DrawContext ctx, TextRenderer tr, ClientPlayerEntity p, double x, double z, String s, int c, String sous, int cx, int y) {
        float rel = angle(p, x, z);
        boolean bord = Math.abs(rel) > CHAMP;
        rel = MathHelper.clamp(rel, -CHAMP, CHAMP);
        int px = cx + Math.round(rel / CHAMP * LARGEUR_BOUSSOLE / 2);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(bord ? (rel < 0 ? "◀" : "▶") : s), px, y + 3, c);
        if (sous != null && !bord) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(px, y + 16, 0);
            ctx.getMatrices().scale(0.7f, 0.7f, 1);
            ctx.drawCenteredTextWithShadow(tr, Text.literal(sous), 0, 0, c);
            ctx.getMatrices().pop();
        }
    }

    private static void zone(DrawContext ctx, TextRenderer tr, int w, float pulse) {
        if (EtatAC.zone == Zones.AUCUNE) return;
        int y = 31, demi = 70;
        boolean dedans = EtatAC.zone == Zones.DEDANS;
        int c = dedans ? EtatAC.mix(0xFF8A0D0D, 0xFFE0281E, pulse) : 0xFFE0B020;
        ctx.fill(w / 2 - demi, y, w / 2 + demi, y + 3, c);
        ctx.fill(w / 2 - demi, y + 3, w / 2 + demi, y + 4, 0x80000000);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(dedans ? "ZONE INTERDITE" : "Zone interdite à proximité"), w / 2, y + 6,
                dedans ? 0xFFFF5040 : 0xFFF2D060);
    }

    /** Les flèches autour du viseur : une par garde qui te soupçonne, tournée vers lui. */
    private static void fleches(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p, int w, int h, float pulse) {
        MatrixStack m = ctx.getMatrices();
        for (Map.Entry<Integer, Integer> e : EtatAC.soupcons.entrySet()) {
            Entity g = mc.world.getEntityById(e.getKey());
            if (g == null) continue;
            int n = e.getValue();
            float rel = angle(p, g.getX(), g.getZ());
            m.push();
            m.translate(w / 2f, h / 2f, 0);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rel));
            int r = 22 + (n >= 100 ? Math.round(pulse * 3) : 0);
            int c = EtatAC.couleur(n);
            // Le fond de la flèche, puis son remplissage selon le soupçon
            ctx.fill(-8, -r - 3, 8, -r, 0x90000000);
            ctx.fill(-5, -r - 6, 5, -r - 3, 0x90000000);
            ctx.fill(-2, -r - 8, 2, -r - 6, 0x90000000);
            int plein = Math.max(1, Math.round(16 * Math.min(100, n) / 100f));
            ctx.fill(-8, -r - 3, -8 + plein, -r, c);
            if (n >= 45) ctx.fill(-5, -r - 6, 5, -r - 3, c);
            if (n >= 100) ctx.fill(-2, -r - 8, 2, -r - 6, c);
            m.pop();
        }
    }

    private static void notorieteEtJournal(DrawContext ctx, TextRenderer tr, float pulse) {
        int x = 6, y = 6;
        if (EtatAC.notoriete > 0) {
            boolean recherche = EtatAC.notoriete >= fr.fenua.ombre.ac.Notoriete.RECHERCHE;
            ctx.drawTextWithShadow(tr, Text.literal(recherche ? "§lRECHERCHÉ" : "Notoriété"), x, y, recherche ? EtatAC.mix(0xFFFF3020, 0xFF901010, pulse) : 0xFFD0D0D0);
            y += 10;
            for (int i = 0; i < 4; i++) {
                int x0 = x + i * 15;
                ctx.fill(x0, y, x0 + 13, y + 4, 0x90000000);
                int part = Math.max(0, Math.min(25, EtatAC.notoriete - i * 25));
                if (part > 0) ctx.fill(x0, y, x0 + Math.round(13 * part / 25f), y + 4, recherche ? 0xFFE0281E : 0xFFE08A1E);
            }
            y += 10;
        }
        if (EtatAC.objectifs.isEmpty()) return;
        if (EtatAC.objectifs.get(0).startsWith("T")) { lecon(ctx, tr, x, y + 2); return; }
        y += 2;
        ctx.drawTextWithShadow(tr, Text.literal("§6Prague, 1600"), x, y, 0xFFFFFF);
        y += 11;
        for (String o : EtatAC.objectifs) {
            boolean fait = o.startsWith("1");
            String texte = o.substring(1);
            boolean facultatif = texte.startsWith("Facultatif");
            if (facultatif) texte = texte.substring("Facultatif : ".length());
            String s = (fait ? "§a✔ §8§m" : facultatif ? "§7◇ §7§o" : "§f◆ §f") + texte;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y, 0);
            ctx.getMatrices().scale(0.85f, 0.85f, 1);
            ctx.drawTextWithShadow(tr, Text.literal(s), 0, 0, 0xFFFFFF);
            ctx.getMatrices().pop();
            y += 9;
        }
    }

    /** L'initiation : le panneau de la leçon en cours (titre doré, explications), sur fond sombre. */
    private static void lecon(DrawContext ctx, TextRenderer tr, int x, int y) {
        // Pas plus large que la place laissée à gauche de la boussole
        int largeur = Math.max(150, Math.min(250, ctx.getScaledWindowWidth() / 2 - LARGEUR_BOUSSOLE / 2 - 14));
        java.util.List<net.minecraft.text.OrderedText> lignes = new java.util.ArrayList<>();
        String titre = EtatAC.objectifs.get(0).substring(1);
        for (int i = 1; i < EtatAC.objectifs.size(); i++)
            lignes.addAll(tr.wrapLines(Text.literal("§f" + EtatAC.objectifs.get(i).substring(1)), largeur - 10));
        var titres = tr.wrapLines(Text.literal("§6§l" + titre), largeur - 10);
        int h = 6 + titres.size() * 10 + lignes.size() * 10;
        ctx.fill(x - 2, y - 2, x + largeur, y + h, 0xA0101010);
        ctx.fill(x - 2, y - 2, x, y + h, 0xFFF2C23A);
        int ly = y + 2;
        for (var t : titres) { ctx.drawTextWithShadow(tr, t, x + 5, ly, 0xFFFFFF); ly += 10; }
        ly += 3;
        for (var l : lignes) { ctx.drawTextWithShadow(tr, l, x + 5, ly, 0xFFFFFF); ly += 10; }
    }

    // ───────────── dans le monde : « ? » et « ! » au-dessus des gardes ─────────────

    public static void dessinerMonde(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || ctx.matrixStack() == null || ctx.consumers() == null || EtatAC.soupcons.isEmpty()) return;
        Vec3d cam = ctx.camera().getPos();
        float delta = ctx.tickCounter().getTickDelta(true);
        TextRenderer tr = mc.textRenderer;
        for (Map.Entry<Integer, Integer> e : EtatAC.soupcons.entrySet()) {
            Entity g = mc.world.getEntityById(e.getKey());
            if (g == null) continue;
            int n = e.getValue();
            Vec3d pos = g.getLerpedPos(delta).add(0, g.getHeight() + 0.75, 0);
            MatrixStack m = ctx.matrixStack();
            m.push();
            m.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            m.multiply(ctx.camera().getRotation());
            float echelle = 0.045f;
            m.scale(echelle, -echelle, echelle);
            String s = n >= 100 ? "!" : "?";
            Matrix4f mat = m.peek().getPositionMatrix();
            float x = -tr.getWidth(s) / 2f;
            int c = EtatAC.couleur(n);
            tr.draw(s, x, 0, (c & 0xFFFFFF) | 0x80000000, false, mat, ctx.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            tr.draw(s, x, 0, c, false, mat, ctx.consumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            m.pop();
        }
    }
}
