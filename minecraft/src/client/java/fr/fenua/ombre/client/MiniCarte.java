package fr.fenua.ombre.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * La mini-carte, en bas à droite de l'écran (comme dans Assassin's Creed) : 96 × 96 blocs autour du joueur, le nord
 * en haut, relevés une fois par seconde avec les couleurs du parchemin de la grande carte (touche J).
 * Dessus : le joueur (flèche), l'objectif en cours (losange doré), les gardes qui t'ont remarqué ou qu'on a marqués
 * (points rouges), les marchands (vert) et les habitants qui offrent une quête (« ! » jaune).
 */
public final class MiniCarte {
    private MiniCarte() {}

    private static final int N = 96;                   // blocs couverts
    private static final int TAILLE = 74;              // côté à l'écran (pixels de l'interface)
    private static NativeImage image;
    private static NativeImageBackedTexture tex;
    private static Identifier id;
    private static int ox, oz, compte;

    public static boolean montree = true;

    /** Chaque tick : une fois par seconde, on relève le terrain autour du joueur. */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || !montree) return;
        if (compte++ % 20 != 0) return;
        if (image == null) {
            image = new NativeImage(N, N, false);
            tex = new NativeImageBackedTexture(image);
            id = mc.getTextureManager().registerDynamicTexture("ombre_minicarte", tex);
        }
        ClientWorld w = mc.world;
        ox = mc.player.getBlockX() - N / 2;
        oz = mc.player.getBlockZ() - N / 2;
        BlockPos.Mutable b = new BlockPos.Mutable();
        int[] nord = new int[N];
        for (int z = 0; z < N; z++) for (int x = 0; x < N; x++) {
            int wx = ox + x, wz = oz + z, c;
            int h = Integer.MIN_VALUE;
            if (!w.getChunkManager().isChunkLoaded(wx >> 4, wz >> 4)) c = EcranVoyage.PARCHEMIN;
            else {
                int y = w.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1, rgb = -1;
                boolean eau = false;
                for (int k = 0; k < 40 && y > w.getBottomY(); k++, y--) {
                    BlockState s = w.getBlockState(b.set(wx, y, wz));
                    if (s.getFluidState().isIn(FluidTags.WATER)) { rgb = 0x3F6FB0; eau = true; break; }
                    rgb = EcranVoyage.couleur(s, w, b);
                    if (rgb >= 0) break;
                }
                h = y;
                if (rgb < 0) c = EcranVoyage.PARCHEMIN;
                else {
                    int hn = z == 0 ? h : nord[x];
                    double f = eau || hn == Integer.MIN_VALUE ? 1 : h > hn ? 1.12 : h < hn ? 0.8 : 1;
                    c = EcranVoyage.melange(EcranVoyage.assombrir(rgb, f), EcranVoyage.PARCHEMIN, 0.25);
                }
            }
            nord[x] = h;
            image.setColor(x, z, 0xFF000000 | (c & 0xFF) << 16 | (c & 0xFF00) | (c >> 16 & 0xFF));
        }
        tex.upload();
    }

    /** En bas à droite, au-dessus de la barre d'objets. */
    public static void dessiner(DrawContext ctx, MinecraftClient mc) {
        if (!montree || id == null || mc.player == null || mc.world == null || mc.options.hudHidden || Cinema.actif()) return;
        int x0 = ctx.getScaledWindowWidth() - TAILLE - 8, y0 = ctx.getScaledWindowHeight() - TAILLE - 30;
        ctx.fill(x0 - 3, y0 - 3, x0 + TAILLE + 3, y0 + TAILLE + 3, 0xFF5A4630);
        ctx.fill(x0 - 2, y0 - 2, x0 + TAILLE + 2, y0 + TAILLE + 2, 0xFFC8B080);
        // Le terrain suit le joueur au bloc près (la texture, elle, est relevée une fois par seconde)
        double px = mc.player.getX(), pz = mc.player.getZ();
        float u = (float) (px - ox - N / 2.0 + (N - N * 0.8) / 2), v = (float) (pz - oz - N / 2.0 + (N - N * 0.8) / 2);
        int vue = (int) (N * 0.8);
        ctx.drawTexture(id, x0, y0, TAILLE, TAILLE, u, v, vue, vue, N, N);
        double echelle = TAILLE / (double) vue;
        // Les marqueurs
        for (Long l : EtatAC.cibles) {
            BlockPos c = BlockPos.fromLong(l);
            point(ctx, mc, x0, y0, echelle, c.getX() + 0.5 - px, c.getZ() + 0.5 - pz, "◆", 0xFFF2C23A, true);
        }
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || !l.isAlive() || e.squaredDistanceTo(mc.player) > 60 * 60) continue;
            double dx = e.getX() - px, dz = e.getZ() - pz;
            if (fr.fenua.ombre.Discretion.estGardeClient(e)) {
                boolean connu = EtatAC.soupcons.containsKey(e.getId()) || l.hasStatusEffect(StatusEffects.GLOWING) || e.squaredDistanceTo(mc.player) < 14 * 14;
                if (connu) point(ctx, mc, x0, y0, echelle, dx, dz, "•", EtatAC.soupcons.getOrDefault(e.getId(), 0) >= 100 ? 0xFFFF2020 : 0xFFC03028, false);
            } else if (e.hasCustomName()) {
                String n = e.getCustomName().getString();
                if (n.startsWith("! ") || n.startsWith("§e!")) point(ctx, mc, x0, y0, echelle, dx, dz, "!", 0xFFFFE040, false);
                else for (String m : fr.fenua.ombre.ac.Profil.MARCHANDS) if (n.equals(m)) point(ctx, mc, x0, y0, echelle, dx, dz, "●", 0xFF40B040, false);
            }
        }
        // Le joueur : une flèche tournée comme lui, au centre
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x0 + TAILLE / 2f, y0 + TAILLE / 2f, 200);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(mc.player.getYaw() + 180));
        ctx.drawText(mc.textRenderer, Text.literal("▲"), -3, -4, 0xFFFFFFFF, true);
        ctx.getMatrices().pop();
        ctx.drawText(mc.textRenderer, Text.literal("N"), x0 + TAILLE / 2 - 2, y0 - 1, 0xFF5A4630, false);
    }

    /** Un marqueur à (dx, dz) du joueur ; « bord » : s'il sort de la carte, on le pose sur le bord. */
    private static void point(DrawContext ctx, MinecraftClient mc, int x0, int y0, double echelle, double dx, double dz, String s, int c, boolean bord) {
        double sx = TAILLE / 2.0 + dx * echelle, sy = TAILLE / 2.0 + dz * echelle;
        if (sx < 2 || sy < 2 || sx > TAILLE - 2 || sy > TAILLE - 2) {
            if (!bord) return;
            double k = (TAILLE / 2.0 - 3) / Math.max(Math.abs(sx - TAILLE / 2.0), Math.abs(sy - TAILLE / 2.0));
            sx = TAILLE / 2.0 + (sx - TAILLE / 2.0) * k;
            sy = TAILLE / 2.0 + (sy - TAILLE / 2.0) * k;
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 190);
        ctx.drawText(mc.textRenderer, Text.literal(s), x0 + (int) sx - mc.textRenderer.getWidth(s) / 2, y0 + (int) sy - 4, c, true);
        ctx.getMatrices().pop();
    }
}
