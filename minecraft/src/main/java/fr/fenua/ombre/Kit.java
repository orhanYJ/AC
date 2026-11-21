package fr.fenua.ombre;

import fr.fenua.ombre.objet.Objets;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** /ombre kit : tout l'équipement de l'assassin d'un coup. */
public final class Kit {
    private Kit() {}

    public static void donner(ServerPlayerEntity j) {
        Object[][] kit = {{OmbreItems.LAME, 1}, {Objets.GRAPPIN, 1}, {OmbreItems.COUTEAUX, 16}, {OmbreItems.BOMBE_FUMEE, 8},
                {Objets.THALERS, 32}, {Objets.PETARD, 8}, {Objets.FLECHETTE_SOPORIFIQUE, 8}, {Objets.FLECHETTE_EMPOISONNEE, 8}, {Objets.FLECHETTE_FOLIE, 6},
                {Objets.TENUE, 2}, {Objets.PAIN_SEIGLE, 8}, {Objets.BIERE, 4}, {OmbreItems.LAME, 1}};
        for (Object[] o : kit) j.getInventory().offerOrDrop(new ItemStack((Item) o[0], (Integer) o[1]));
    }
}
