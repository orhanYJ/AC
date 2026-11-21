package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.Profil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * L'arbre des compétences (touche K) : quatre branches (Ombre, Lame, Toits, Ruse), trois rangs chacune.
 * Un rang s'apprend avec des points (1, 2 puis 3) gagnés en accomplissant des missions, une fois le rang d'avant appris.
 */
public class EcranCompetences extends Screen {
    public EcranCompetences() { super(Text.literal("Compétences")); }

    private static final int COL = 112, RANG = 46;

    @Override
    protected void init() {
        clearChildren();
        int x0 = width / 2 - 2 * COL, y0 = height / 2 - 60;
        for (Profil.Competence c : Profil.COMPETENCES) {
            boolean appris = ProfilClient.a(c.id());
            boolean ouvert = c.rang() == 0 || Profil.COMPETENCES.stream().anyMatch(d -> d.branche() == c.branche() && d.rang() == c.rang() - 1 && ProfilClient.a(d.id()));
            String etiquette = appris ? "§a✔ " + c.nom() : (ouvert ? "§f" : "§8") + c.nom();
            ButtonWidget b = ButtonWidget.builder(Text.literal(etiquette),
                    w -> ClientPlayNetworking.send(new Profil.DemandePayload(Profil.DemandePayload.APPRENDRE, 0, 0, c.id())))
                    .dimensions(x0 + c.branche() * COL + 4, y0 + c.rang() * RANG, COL - 8, 20)
                    .tooltip(Tooltip.of(Text.literal("§6" + c.nom() + "\n§f" + c.effet() + "\n§7Coût : " + c.cout() + " point" + (c.cout() > 1 ? "s" : "")
                            + (ouvert ? "" : "\n§cIl faut d'abord le rang du dessus."))))
                    .build();
            b.active = !appris && ouvert && ProfilClient.points >= c.cout();
            addDrawableChild(b);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), w -> close()).dimensions(width / 2 - 40, y0 + 3 * RANG + 6, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int x0 = width / 2 - 2 * COL, y0 = height / 2 - 60;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§6✦ Compétences de l'Ombre ✦"), width / 2, y0 - 42, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Points à dépenser : §b" + ProfilClient.points
                + " §8· gagnés en accomplissant les missions"), width / 2, y0 - 30, 0xFFFFFF);
        int[] couleurs = {0xFF6A8CC8, 0xFFC84A3A, 0xFFD9A63A, 0xFF6AB06A};
        for (int b = 0; b < 4; b++) {
            int x = x0 + b * COL + COL / 2;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§l" + Profil.BRANCHES[b]), x, y0 - 14, couleurs[b]);
            for (int r = 0; r < 2; r++) ctx.fill(x - 1, y0 + r * RANG + 31, x + 1, y0 + (r + 1) * RANG - 1, 0x80FFFFFF);
        }
        // Le coût de chaque rang, sous son bouton
        for (Profil.Competence c : Profil.COMPETENCES) {
            if (ProfilClient.a(c.id())) continue;
            String t = c.cout() + " point" + (c.cout() > 1 ? "s" : "");
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x0 + c.branche() * COL + COL / 2f, y0 + c.rang() * RANG + 23, 0);
            ctx.getMatrices().scale(0.75f, 0.75f, 1);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + t), 0, 0, 0xFFFFFF);
            ctx.getMatrices().pop();
        }
    }

    @Override public boolean shouldPause() { return false; }
}
