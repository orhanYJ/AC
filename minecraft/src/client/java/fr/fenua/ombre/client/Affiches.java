package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.AvisRecherche;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Les avis de recherche, repérés par la vue de l'aigle : en planant, on voit tous ceux des alentours (64 blocs),
 * marqués d'un signe doré visible à travers les murs, avec leur distance. Ils restent marqués 30 secondes
 * après qu'on est redescendu, le temps d'aller les arracher (clic droit : notoriété −30).
 */
public final class Affiches {
    private Affiches() {}

    private static final int RAYON_TRONCONS = 4;           // 4 tronçons de 16 : 64 blocs autour
    private static final List<BlockPos> VUES = new ArrayList<>();
    private static long jusqua = -1;
    private static int prochain;

    public static int nombre() { return VUES.size(); }

    /** Chaque tick : pendant la vue de l'aigle (et 30 s après), on relève les affiches une fois par seconde. */
    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) { VUES.clear(); return; }
        long t = mc.world.getTime();
        if (Cinema.plan() == Cinema.Plan.AIGLE) {
            if (t > jusqua) prochain = 0;                       // on vient de s'envoler : relevé tout de suite
            jusqua = t + 600;
        }
        if (t > jusqua) { VUES.clear(); return; }
        if (--prochain > 0) return;
        prochain = 20;
        relever(mc);
    }

    /** Les tronçons chargés autour du joueur ; une section sans affiche est sautée d'un coup (sa palette le dit). */
    private static void relever(MinecraftClient mc) {
        VUES.clear();
        int cx0 = mc.player.getBlockX() >> 4, cz0 = mc.player.getBlockZ() >> 4;
        for (int cx = cx0 - RAYON_TRONCONS; cx <= cx0 + RAYON_TRONCONS; cx++)
            for (int cz = cz0 - RAYON_TRONCONS; cz <= cz0 + RAYON_TRONCONS; cz++) {
                if (!(mc.world.getChunk(cx, cz) instanceof WorldChunk ch)) continue;
                ChunkSection[] sections = ch.getSectionArray();
                for (int i = 0; i < sections.length; i++) {
                    ChunkSection s = sections[i];
                    if (s == null || s.isEmpty() || !s.hasAny(st -> st.getBlock() instanceof AvisRecherche)) continue;
                    int y0 = ch.sectionIndexToCoord(i) << 4;
                    for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                        BlockState st = s.getBlockState(x, y, z);
                        if (st.getBlock() instanceof AvisRecherche) VUES.add(new BlockPos((cx << 4) + x, y0 + y, (cz << 4) + z));
                    }
                }
            }
    }

    /** Le signe doré au-dessus de chaque affiche, à travers les murs, et sa distance. */
    public static void dessiner(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (VUES.isEmpty() || mc.world == null || mc.player == null || ctx.matrixStack() == null || ctx.consumers() == null) return;
        Vec3d cam = ctx.camera().getPos();
        TextRenderer tr = mc.textRenderer;
        boolean aigle = Cinema.plan() == Cinema.Plan.AIGLE;
        float bat = (float) Math.sin((mc.world.getTime() + ctx.tickCounter().getTickDelta(true)) * 0.25) * 0.5f + 0.5f;
        for (BlockPos b : VUES) {
            if (!(mc.world.getBlockState(b).getBlock() instanceof AvisRecherche)) continue;   // arrachée entre-temps
            Vec3d pos = Vec3d.ofCenter(b).add(0, 0.9, 0);
            double d = pos.distanceTo(mc.player.getPos());
            MatrixStack m = ctx.matrixStack();
            m.push();
            m.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            m.multiply(ctx.camera().getRotation());
            // Vu du ciel, le signe grossit pour rester lisible
            float echelle = (float) (0.03 * Math.max(1, pos.distanceTo(cam) / 12));
            m.scale(echelle, -echelle, echelle);
            Matrix4f mat = m.peek().getPositionMatrix();
            String signe = "✉ AVIS";
            String dist = (int) d + " m";
            int or = 0xFFF2C23A, orPale = 0x90F2C23A;
            int a = (int) (0x90 + 0x6F * bat) << 24;
            tr.draw(signe, -tr.getWidth(signe) / 2f, 0, (or & 0xFFFFFF) | a, false, mat, ctx.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0x60000000, 0xF000F0);
            tr.draw(signe, -tr.getWidth(signe) / 2f, 0, or, false, mat, ctx.consumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            if (!aigle || d < 40) tr.draw(dist, -tr.getWidth(dist) / 2f, 10, orPale, false, mat, ctx.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            m.pop();
        }
    }
}
