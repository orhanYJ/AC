package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.VoyagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte (touche J) : un parchemin où figurent les points de vue synchronisés (✦),
 * les marqueurs de mission (◆) et toi (●). Un clic sur un point de vue : voyage rapide.
 * À droite, le journal de la mission.
 */
public class EcranVoyage extends Screen {
    private int px0, py0, pw, ph, survol = -1;
    private double minX, minZ, echelle;

    public EcranVoyage() { super(Text.literal("Carte")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        pw = Math.min(260, width - 180); ph = Math.min(200, height - 40);
        px0 = width / 2 - (pw + 150) / 2; py0 = height / 2 - ph / 2;
        // Le parchemin
        ctx.fill(px0 - 3, py0 - 3, px0 + pw + 3, py0 + ph + 3, 0xFF5A3A1A);
        ctx.fill(px0, py0, px0 + pw, py0 + ph, 0xFFE6D3A3);
        for (int i = 0; i < 6; i++) ctx.fill(px0, py0 + i * ph / 6, px0 + pw, py0 + i * ph / 6 + 1, 0x18603010);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§6Prague, an 1600"), px0 + pw / 2, py0 - 14, 0xFFFFFF);

        var p = client.player;
        List<double[]> pts = new ArrayList<>();
        for (Long l : EtatAC.vues) { BlockPos b = BlockPos.fromLong(l); pts.add(new double[]{b.getX(), b.getZ()}); }
        for (Long l : EtatAC.cibles) { BlockPos b = BlockPos.fromLong(l); pts.add(new double[]{b.getX(), b.getZ()}); }
        pts.add(new double[]{p.getX(), p.getZ()});
        minX = Double.MAX_VALUE; minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] q : pts) { minX = Math.min(minX, q[0]); maxX = Math.max(maxX, q[0]); minZ = Math.min(minZ, q[1]); maxZ = Math.max(maxZ, q[1]); }
        double marge = 24;
        minX -= marge; minZ -= marge; maxX += marge; maxZ += marge;
        echelle = Math.min((pw - 16) / Math.max(1, maxX - minX), (ph - 16) / Math.max(1, maxZ - minZ));

        // Les marqueurs de mission
        for (Long l : EtatAC.cibles) {
            BlockPos b = BlockPos.fromLong(l);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("◆"), sx(b.getX()), sy(b.getZ()) - 4, 0xFFD4A020);
        }
        // Les points de vue : survol et clic
        survol = -1;
        for (int i = 0; i < EtatAC.vues.size(); i++) {
            BlockPos b = BlockPos.fromLong(EtatAC.vues.get(i));
            int x = sx(b.getX()), y = sy(b.getZ());
            boolean s = Math.abs(mx - x) <= 6 && Math.abs(my - y) <= 6;
            if (s) survol = i;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("✦"), x, y - 4, s ? 0xFFFFFFFF : 0xFF2A5A9A);
            String nom = (i < 9 ? (i + 1) + ". " : "") + (i < EtatAC.nomsVues.size() ? EtatAC.nomsVues.get(i) : "");
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y + 6, 0);
            ctx.getMatrices().scale(0.7f, 0.7f, 1);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(nom), 0, 0, s ? 0xFFFFFFFF : 0xFF3A2A1A);
            ctx.getMatrices().pop();
        }
        // Toi : une flèche dans la direction du regard
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("●"), sx(p.getX()), sy(p.getZ()) - 4, 0xFFC02020);

        if (EtatAC.vues.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Synchronise un point de vue"), px0 + pw / 2, py0 + ph / 2 - 10, 0xFFFFFF);
        if (survol >= 0) {
            BlockPos b = BlockPos.fromLong(EtatAC.vues.get(survol));
            int d = (int) Math.sqrt(p.getBlockPos().getSquaredDistance(b));
            ctx.drawTooltip(textRenderer, List.of(Text.literal("§6" + EtatAC.nomsVues.get(survol)), Text.literal("§7" + d + " m — clic : voyage rapide")), mx, my);
        }

        // Le journal, à droite
        int jx = px0 + pw + 14, jy = py0;
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
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8✦ point de vue  ◆ mission  ● toi   —   clic ou 1 à 9 : voyage rapide"), px0, py0 + ph + 8, 0xFFFFFF);
    }

    private int sx(double x) { return px0 + 8 + (int) Math.round((x - minX) * echelle); }
    private int sy(double z) { return py0 + 8 + (int) Math.round((z - minZ) * echelle); }

    @Override
    public boolean mouseClicked(double x, double y, int bouton) {
        if (survol >= 0 && survol < EtatAC.vues.size()) { voyager(survol); return true; }
        return super.mouseClicked(x, y, bouton);
    }

    /** Les touches 1 à 9 : voyager au point de vue de ce numéro. */
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int i = key - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
        if (i >= 0 && i < 9 && i < EtatAC.vues.size()) { voyager(i); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void voyager(int i) {
        ClientPlayNetworking.send(new VoyagePayload(EtatAC.vues.get(i)));
        close();
    }
}
