package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import fr.fenua.ombre.OmbreItems;
import fr.fenua.ombre.objet.Objets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * La roue des outils, comme dans Assassin's Creed : on maintient la touche (G), le jeu se fige,
 * on montre un outil avec la souris et on relâche — il passe dans la main.
 */
public class RoueOutils extends Screen {
    private record Outil(Item item, String nom, String aide) {}

    private static final Outil[] OUTILS = {
            new Outil(OmbreItems.LAME, "Lame de nacre", "Assassiner (R) — deux lames : double assassinat"),
            new Outil(OmbreItems.COUTEAUX, "Couteaux de lancer", "Tuer à distance, sans bruit"),
            new Outil(Objets.FLECHETTE_SOPORIFIQUE, "Fléchette soporifique", "Le garde s'endort 30 s"),
            new Outil(Objets.FLECHETTE_EMPOISONNEE, "Fléchette empoisonnée", "Il s'effondre 4 s plus tard"),
            new Outil(OmbreItems.BOMBE_FUMEE, "Bombe de fumée", "Aveugle les gardes, efface les soupçons"),
            new Outil(Objets.PETARD, "Pétard", "Bruit et fumée : la foule s'enfuit"),
            new Outil(Objets.THALERS, "Thalers", "Jetés au sol : la foule accourt"),
            new Outil(Objets.GRAPPIN, "Lance-grappin", "Se hisser sur un toit d'un coup"),
            new Outil(Objets.TENUE, "Tenue de hallebardier", "Se déguiser en garde"),
    };
    private static final int RAYON = 64;

    private final KeyBinding touche;
    private int choix = -1;

    public RoueOutils(KeyBinding touche) {
        super(Text.literal("Roue des outils"));
        this.touche = touche;
    }

    @Override public boolean shouldPause() { return true; }   // le temps s'arrête pendant qu'on choisit

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int cx = width / 2, cy = height / 2;
        double dx = mx - cx, dy = my - cy;
        double ang = (Math.toDegrees(Math.atan2(dx, -dy)) + 360) % 360;   // 0 = en haut, sens des aiguilles
        float pas = 360f / OUTILS.length;
        choix = dx * dx + dy * dy > 18 * 18 ? (int) ((ang + pas / 2) % 360 / pas) : choix;

        // L'anneau
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            int x = cx + (int) Math.round(Math.sin(a) * RAYON), y = cy - (int) Math.round(Math.cos(a) * RAYON);
            ctx.fill(x - 1, y - 1, x + 2, y + 2, 0x40FFFFFF);
        }
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < OUTILS.length; i++) {
            double a = Math.toRadians(i * pas);
            int x = cx + (int) Math.round(Math.sin(a) * RAYON), y = cy - (int) Math.round(Math.cos(a) * RAYON);
            int n = compter(inv, OUTILS[i].item);
            boolean sel = i == choix;
            int bord = sel ? 0xFFF2C23A : 0xFF505050;
            ctx.fill(x - 15, y - 15, x + 15, y + 15, bord);
            ctx.fill(x - 14, y - 14, x + 14, y + 14, sel ? 0xE0302010 : 0xC0101010);
            ItemStack pile = new ItemStack(OUTILS[i].item, Math.max(1, n));
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y, 0);
            ctx.getMatrices().scale(1.3f, 1.3f, 1);
            ctx.drawItem(pile, -8, -8);
            ctx.getMatrices().pop();
            if (n == 0) ctx.fill(x - 14, y - 14, x + 14, y + 14, 0xB0101010);   // grisé : on n'en a pas
            else if (n > 1) ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(n)), x + 13 - textRenderer.getWidth(String.valueOf(n)), y + 5, 0xFFFFFF);
        }
        // Au centre : l'outil montré
        ctx.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0x80FFFFFF);
        if (choix >= 0) {
            Outil o = OUTILS[choix];
            boolean a = compter(inv, o.item) > 0;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal((a ? "§6" : "§8") + o.nom), cx, cy + RAYON + 24, 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(a ? "§7" + o.aide : "§8Tu n'en as pas"), cx, cy + RAYON + 36, 0xFFFFFF);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Montre un outil, relâche la touche"), cx, cy - RAYON - 30, 0xFFFFFF);
    }

    private static int compter(PlayerInventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < 36; i++) if (inv.getStack(i).isOf(item)) n += inv.getStack(i).getCount();
        if (inv.offHand.get(0).isOf(item)) n += inv.offHand.get(0).getCount();
        return n;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (touche.matchesKey(key, scan)) { choisir(); return true; }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double x, double y, int bouton) {
        choisir();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    /** L'outil montré passe dans la main : déjà dans la barre, on le sélectionne ; sinon le serveur l'y met. */
    private void choisir() {
        MinecraftClient mc = client;
        close();
        if (choix < 0 || mc == null || mc.player == null) return;
        PlayerInventory inv = mc.player.getInventory();
        Item item = OUTILS[choix].item;
        for (int i = 0; i < 9; i++) if (inv.getStack(i).isOf(item)) { inv.selectedSlot = i; son(mc); return; }
        for (int i = 9; i < 36; i++) if (inv.getStack(i).isOf(item)) {
            ClientPlayNetworking.send(new ActionPayload(ActionPayload.OUTIL, i));
            son(mc);
            return;
        }
        mc.player.sendMessage(Text.literal("§7Tu n'as pas de " + OUTILS[choix].nom.toLowerCase() + "."), true);
    }

    private static void son(MinecraftClient mc) {
        mc.player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), 0.6f, 1.4f);
    }
}
