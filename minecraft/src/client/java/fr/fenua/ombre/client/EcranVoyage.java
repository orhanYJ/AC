package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.VoyagePayload;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * La carte (touche J) : le vrai terrain vu du dessus, dessiné comme un vieux plan sur parchemin.
 * Chaque bloc du dessus donne sa couleur (toits de tuiles, crépis, pavés, eau…), ombrée selon la hauteur ;
 * ce que le jeu n'a pas encore chargé reste parchemin vierge (terra incognita).
 * Dessus : les zones interdites (en rouge), les points de vue (✦, clic ou 1 à 9 : voyage rapide),
 * les marqueurs de mission (◆) et toi (la flèche rouge, tournée où tu regardes).
 * La molette zoome, la souris (bouton gauche maintenu) fait glisser la carte. À droite, le journal.
 */
public class EcranVoyage extends Screen {
    private static final int N = 320;                          // la carte couvre 320 × 320 blocs autour de toi
    private static final int[] ZOOMS = {48, 96, 160, 320};      // largeur montrée, en blocs
    private static int zoom = 1;
    private static final int PARCHEMIN = 0xE2CDA0;

    private Identifier texture;
    private NativeImageBackedTexture image;
    private int ox, oz;                                         // coin nord-ouest de la carte, dans le monde
    private double cx, cz;                                      // centre de la vue
    private final List<int[]> zones = new ArrayList<>();       // étendards (x, z)
    private int px0, py0, pw, ph, survol = -1;
    private boolean glisse;

    public EcranVoyage() { super(Text.literal("Carte")); }

    @Override public boolean shouldPause() { return false; }

    // ───────────────────────── le relevé du terrain ─────────────────────────

    @Override
    protected void init() {
        if (texture != null) return;
        ClientPlayNetworking.send(new fr.fenua.ombre.ActionPayload(fr.fenua.ombre.ActionPayload.CARTE));
        ClientPlayerEntity p = client.player;
        ClientWorld w = client.world;
        ox = (int) Math.floor(p.getX()) - N / 2;
        oz = (int) Math.floor(p.getZ()) - N / 2;
        cx = p.getX(); cz = p.getZ();
        NativeImage img = new NativeImage(N, N, false);
        int[] hauteurs = new int[N];                            // la rangée du nord, pour l'ombrage
        BlockPos.Mutable b = new BlockPos.Mutable();
        for (int z = 0; z < N; z++) {
            for (int x = 0; x < N; x++) {
                int wx = ox + x, wz = oz + z;
                int couleur;
                int h = Integer.MIN_VALUE;
                if (!w.getChunkManager().isChunkLoaded(wx >> 4, wz >> 4)) {
                    couleur = papier(PARCHEMIN, wx, wz);
                } else {
                    int y = w.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                    int rgb = -1, fond = 0;
                    for (int k = 0; k < 48 && y > w.getBottomY(); k++, y--) {
                        BlockState s = w.getBlockState(b.set(wx, y, wz));
                        if (s.isOf(OmbreBlocs.ETENDARD)) zones.add(new int[]{wx, wz});
                        if (!s.getFluidState().isEmpty() && s.getFluidState().isIn(FluidTags.WATER)) {
                            // l'eau : plus sombre quand elle est profonde
                            while (fond < 8 && w.getFluidState(b.set(wx, y - fond - 1, wz)).isIn(FluidTags.WATER)) fond++;
                            rgb = assombrir(0x3F6FB0, 1.08 - fond * 0.06);
                            break;
                        }
                        rgb = couleur(s, w, b);
                        if (rgb >= 0) break;
                    }
                    h = y;
                    if (rgb < 0) couleur = papier(PARCHEMIN, wx, wz);
                    else {
                        // ombrage : plus clair si plus haut que le voisin du nord, plus sombre si plus bas
                        int hn = z == 0 ? h : hauteurs[x];
                        double f = hn == Integer.MIN_VALUE || fond > 0 ? 1 : h > hn ? (h - hn >= 3 ? 1.18 : 1.1) : h < hn ? (hn - h >= 3 ? 0.62 : 0.85) : 1;
                        couleur = papier(melange(assombrir(rgb, f), PARCHEMIN, 0.27), wx, wz);
                    }
                }
                hauteurs[x] = h;
                img.setColor(x, z, 0xFF000000 | (couleur & 0xFF) << 16 | (couleur & 0xFF00) | (couleur >> 16 & 0xFF));
            }
        }
        image = new NativeImageBackedTexture(img);
        texture = client.getTextureManager().registerDynamicTexture("ombre_carte", image);
    }

    /** Les blocs du mod ont leurs vraies couleurs (pour Minecraft, ils sont « gris pierre » ou transparents). */
    private static final Map<String, Integer> COULEURS = Map.ofEntries(
            Map.entry("tuiles", 0xB24832), Map.entry("ardoise", 0x464B55), Map.entry("pierre_noircie", 0x3C3A3C),
            Map.entry("pierre_de_taille", 0xC8BEA5), Map.entry("pierre_lisse", 0xCDC8B9), Map.entry("pierre_sculptee", 0xC3B9A0),
            Map.entry("pave", 0x7D7870), Map.entry("dalles_de_pierre", 0x96918A), Map.entry("crepi_jaune", 0xE6C86E),
            Map.entry("crepi_rose", 0xE1A5A0), Map.entry("crepi_bleu", 0x96AFCD), Map.entry("crepi_vert", 0xA0BE96),
            Map.entry("crepi_ocre", 0xCD9650), Map.entry("crepi", 0xE1D7BE), Map.entry("sgraffite", 0xBEB9AF),
            Map.entry("brique_ancienne", 0x96503C), Map.entry("marbre_blanc", 0xEBEBE6), Map.entry("marbre_noir", 0x28282D),
            Map.entry("meule", 0xD2B446), Map.entry("charrette", 0xC8AA46), Map.entry("parquet", 0x8C643C),
            Map.entry("boiserie", 0x8C643C), Map.entry("etendard", 0xAA1E1E), Map.entry("fontaine", 0x9AA8B8));

    /** La couleur d'un bloc vu du dessus, ou -1 s'il ne se voit pas (air, barrière, petit décor transparent). */
    private static int couleur(BlockState s, ClientWorld w, BlockPos pos) {
        if (s.isAir() || s.isOf(Blocks.BARRIER)) return -1;
        Identifier id = Registries.BLOCK.getId(s.getBlock());
        if (id.getNamespace().equals("ombre")) {
            String n = id.getPath();
            for (var e : COULEURS.entrySet()) if (n.startsWith(e.getKey())) return e.getValue();
        }
        MapColor m = s.getMapColor(w, pos);
        return m == MapColor.CLEAR ? -1 : m.color;
    }

    private static int assombrir(int rgb, double f) {
        int r = MathHelper.clamp((int) ((rgb >> 16 & 255) * f), 0, 255);
        int g = MathHelper.clamp((int) ((rgb >> 8 & 255) * f), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 255) * f), 0, 255);
        return r << 16 | g << 8 | b;
    }

    private static int melange(int a, int b, double t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Le grain du papier : une légère variation, toujours la même à un endroit donné. */
    private static int papier(int rgb, int x, int z) {
        int h = (x * 73856093) ^ (z * 19349663);
        h ^= h >>> 13;
        double f = 0.95 + ((h & 255) / 255.0) * 0.07;
        return assombrir(rgb, f);
    }

    @Override
    public void removed() {
        if (texture != null) client.getTextureManager().destroyTexture(texture);
        texture = null;
    }

    // ───────────────────────── le dessin ─────────────────────────

    private double bpp() { return ZOOMS[zoom] / (double) pw; }                 // blocs par pixel d'écran
    private int sx(double x) { return px0 + pw / 2 + (int) Math.round((x - cx) / bpp()); }
    private int sy(double z) { return py0 + ph / 2 + (int) Math.round((z - cz) / bpp()); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        pw = Math.max(160, Math.min(300, width - 190));
        ph = Math.max(120, Math.min(230, height - 56));
        px0 = width / 2 - (pw + 150) / 2;
        py0 = height / 2 - ph / 2 + 6;
        borner();

        // Le cadre : bois sombre, filet doré
        ctx.fill(px0 - 6, py0 - 6, px0 + pw + 6, py0 + ph + 6, 0xFF3A2410);
        ctx.fill(px0 - 4, py0 - 4, px0 + pw + 4, py0 + ph + 4, 0xFFB08A3C);
        ctx.fill(px0 - 3, py0 - 3, px0 + pw + 3, py0 + ph + 3, 0xFF5A3A1A);

        // Le terrain
        double larg = ZOOMS[zoom], haut = larg * ph / pw;
        float u = (float) (cx - larg / 2 - ox), v = (float) (cz - haut / 2 - oz);
        ctx.drawTexture(texture, px0, py0, pw, ph, u, v, (int) Math.round(larg), (int) Math.round(haut), N, N);

        ctx.enableScissor(px0, py0, px0 + pw, py0 + ph);
        // Un quadrillage léger, tous les 32 blocs, comme les méridiens d'un vieux plan
        for (int gx = Math.floorDiv(ox, 32) * 32; gx < ox + N; gx += 32) { int x = sx(gx); ctx.fill(x, py0, x + 1, py0 + ph, 0x18402810); }
        for (int gz = Math.floorDiv(oz, 32) * 32; gz < oz + N; gz += 32) { int y = sy(gz); ctx.fill(px0, y, px0 + pw, y + 1, 0x18402810); }
        // Les zones interdites
        for (int[] e : zones) {
            int zx = sx(e[0] + 0.5), zy = sy(e[1] + 0.5), r = (int) Math.round(fr.fenua.ombre.ac.Zones.RAYON / bpp());
            for (int dy = -r; dy <= r; dy++) {
                int dx = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
                ctx.fill(zx - dx, zy + dy, zx + dx, zy + dy + 1, 0x38C01818);
                ctx.fill(zx - dx, zy + dy, zx - dx + 1, zy + dy + 1, 0xB0A01010);
                ctx.fill(zx + dx - 1, zy + dy, zx + dx, zy + dy + 1, 0xB0A01010);
            }
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("⚑"), zx, zy - 4, 0xFFD02020);
        }
        // Les marqueurs de mission
        for (Long l : EtatAC.cibles) {
            BlockPos bp = BlockPos.fromLong(l);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("◆"), sx(bp.getX() + 0.5), sy(bp.getZ() + 0.5) - 4, 0xFFF2C23A);
        }
        // Les points de vue
        survol = -1;
        for (int i = 0; i < EtatAC.vues.size(); i++) {
            BlockPos bp = BlockPos.fromLong(EtatAC.vues.get(i));
            int x = sx(bp.getX() + 0.5), y = sy(bp.getZ() + 0.5);
            boolean s = Math.abs(mx - x) <= 6 && Math.abs(my - y) <= 6;
            if (s) survol = i;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("✦"), x, y - 4, s ? 0xFFFFFFFF : 0xFF9FD8FF);
            String nom = (i < 9 ? (i + 1) + ". " : "") + (i < EtatAC.nomsVues.size() ? EtatAC.nomsVues.get(i) : "");
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y + 6, 0);
            ctx.getMatrices().scale(0.7f, 0.7f, 1);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(nom), 0, 0, 0xFFFFFFFF);
            ctx.getMatrices().pop();
        }
        // Toi : une flèche rouge tournée où tu regardes
        ClientPlayerEntity p = client.player;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(sx(p.getX()), sy(p.getZ()), 0);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.getYaw() + 180));
        for (int r = 0; r <= 9; r++) {                           // un triangle cerné de noir, la pointe en avant
            int y = -6 + r, demi = r / 2 + 1;
            ctx.fill(-demi, y, demi + 1, y + 1, 0xFF000000);
        }
        for (int r = 1; r <= 7; r++) {
            int y = -5 + r, demi = r / 2;
            if (r >= 6) demi = Math.max(0, demi - (r - 5));     // une encoche à l'arrière
            ctx.fill(-demi, y, demi + 1, y + 1, 0xFFE02020);
        }
        ctx.getMatrices().pop();
        ctx.disableScissor();

        // Rose des vents, cartouche, échelle
        int rx = px0 + pw - 18, ry = py0 + 20;
        ctx.fill(rx - 10, ry, rx + 11, ry + 1, 0xA0402810);
        ctx.fill(rx, ry - 10, rx + 1, ry + 11, 0xA0402810);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§lN"), rx + 1, ry - 20, 0xFFB02020);
        ctx.fill(px0 + 6, py0 + 6, px0 + 118, py0 + 20, 0xC0F0E0B8);
        ctx.fill(px0 + 6, py0 + 20, px0 + 118, py0 + 21, 0xFF8A6A2A);
        ctx.drawText(textRenderer, Text.literal("§lPrague, an 1600"), px0 + 11, py0 + 9, 0xFF4A2A10, false);
        int pas = ZOOMS[zoom] <= 64 ? 10 : ZOOMS[zoom] <= 160 ? 25 : 50;
        int lb = (int) Math.round(pas / bpp());
        int ex = px0 + 8, ey = py0 + ph - 10;
        ctx.fill(ex, ey, ex + lb, ey + 2, 0xFF3A2410);
        ctx.fill(ex, ey - 3, ex + 1, ey + 2, 0xFF3A2410);
        ctx.fill(ex + lb - 1, ey - 3, ex + lb, ey + 2, 0xFF3A2410);
        ctx.drawText(textRenderer, Text.literal(pas + " blocs"), ex + lb + 4, ey - 3, 0xFF3A2410, false);

        if (EtatAC.vues.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Synchronise un point de vue pour pouvoir y revenir"), px0 + pw / 2, py0 + ph + 10, 0xFFFFFF);
        if (survol >= 0) {
            BlockPos bp = BlockPos.fromLong(EtatAC.vues.get(survol));
            int d = (int) Math.sqrt(p.getBlockPos().getSquaredDistance(bp));
            ctx.drawTooltip(textRenderer, List.of(Text.literal("§6" + EtatAC.nomsVues.get(survol)), Text.literal("§7" + d + " blocs — clic : voyage rapide")), mx, my);
        }

        // Le journal, à droite
        int jx = px0 + pw + 14, jy = py0 - 4;
        ctx.drawTextWithShadow(textRenderer, Text.literal("§6Journal"), jx, jy, 0xFFFFFF);
        jy += 14;
        if (EtatAC.objectifs.isEmpty()) ctx.drawTextWithShadow(textRenderer, Text.literal("§7Aucune mission"), jx, jy, 0xFFFFFF);
        for (String o : EtatAC.objectifs) {
            boolean fait = o.startsWith("1");
            String deb = o.startsWith("T") ? "§6" : o.startsWith("I") ? "§7" : fait ? "§a✔ §7" : "§f◆ ";
            for (var ligne : textRenderer.wrapLines(Text.literal(deb + o.substring(1)), 140)) {
                ctx.drawTextWithShadow(textRenderer, ligne, jx, jy, 0xFFFFFF);
                jy += 10;
            }
            jy += 3;
        }
        jy += 6;
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Notoriété : " + EtatAC.notoriete + " / 100"), jx, jy, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8✦ point de vue  ◆ mission  ⚑ zone interdite   molette : zoom · glisser : déplacer · 1 à 9 : voyage"),
                px0 - 4, py0 + ph + 10 + (EtatAC.vues.isEmpty() ? 11 : 0), 0xFFFFFF);
    }

    /** La vue reste sur la partie relevée de la carte. */
    private void borner() {
        double larg = ZOOMS[zoom], haut = larg * ph / pw;
        cx = MathHelper.clamp(cx, ox + larg / 2, ox + N - larg / 2);
        cz = MathHelper.clamp(cz, oz + haut / 2, oz + N - haut / 2);
    }

    // ───────────────────────── la souris et le clavier ─────────────────────────

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        zoom = MathHelper.clamp(zoom - (int) Math.signum(v), 0, ZOOMS.length - 1);
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int bouton, double dx, double dy) {
        if (bouton != 0) return false;
        glisse = true;
        cx -= dx * bpp();
        cz -= dy * bpp();
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int bouton) {
        if (bouton == 0 && glisse) { glisse = false; return true; }
        if (bouton == 0 && survol >= 0 && survol < EtatAC.vues.size()) { voyager(survol); return true; }
        return super.mouseReleased(x, y, bouton);
    }

    /** Les touches 1 à 9 : voyager au point de vue de ce numéro. */
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int i = key - GLFW.GLFW_KEY_1;
        if (i >= 0 && i < 9 && i < EtatAC.vues.size()) { voyager(i); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void voyager(int i) {
        ClientPlayNetworking.send(new VoyagePayload(EtatAC.vues.get(i)));
        close();
    }
}
