package fr.fenua.ombre.client;

import fr.fenua.ombre.ac.Profil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

/** L'étal d'un marchand : ses marchandises, leur prix ; un clic achète (si la bourse le permet). */
public class EcranBoutique extends Screen {
    private final int boutique;

    public EcranBoutique(int boutique) {
        super(Text.literal(Profil.MARCHANDS[Math.floorMod(boutique, Profil.MARCHANDS.length)]));
        this.boutique = Math.floorMod(boutique, Profil.BOUTIQUES.size());
    }

    private int prix(Profil.Offre o) { return ProfilClient.a("marchand") ? (int) Math.ceil(o.prix() * 0.75) : o.prix(); }

    @Override
    protected void init() {
        clearChildren();
        List<Profil.Offre> offres = Profil.BOUTIQUES.get(boutique);
        int x = width / 2 - 120, y = height / 2 - offres.size() * 12;
        for (int i = 0; i < offres.size(); i++) {
            Profil.Offre o = offres.get(i);
            final int n = i;
            String nom = new ItemStack(o.objet()).getName().getString();
            if (o.objet() == Items.POTION) nom = "Potion de soin";
            ButtonWidget b = ButtonWidget.builder(Text.literal(nom + (o.nombre() > 1 ? " ×" + o.nombre() : "") + "   §6" + prix(o) + " thalers"),
                    w -> ClientPlayNetworking.send(new Profil.DemandePayload(Profil.DemandePayload.ACHETER, boutique, n, "")))
                    .dimensions(x + 22, y + i * 24, 218, 20).build();
            b.active = ProfilClient.thalers >= prix(o);
            addDrawableChild(b);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Partir"), w -> close()).dimensions(width / 2 - 40, y + offres.size() * 24 + 8, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        List<Profil.Offre> offres = Profil.BOUTIQUES.get(boutique);
        int x = width / 2 - 120, y = height / 2 - offres.size() * 12;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§6" + getTitle().getString()), width / 2, y - 34, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Ta bourse : §6⛁ " + ProfilClient.thalers + " thalers"
                + (ProfilClient.a("marchand") ? " §8(marchandeur : −25 %)" : "")), width / 2, y - 20, 0xFFFFFF);
        for (int i = 0; i < offres.size(); i++) {
            ItemStack st = new ItemStack(offres.get(i).objet());
            ctx.drawItem(st, x, y + i * 24 + 2);
        }
    }

    @Override public boolean shouldPause() { return false; }
}
