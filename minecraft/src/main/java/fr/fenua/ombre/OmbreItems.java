package fr.fenua.ombre;

import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Les deux objets d'assassin (l'onglet « L'Ombre » du mode créatif est dans OmbreBlocs). */
public final class OmbreItems {
    private OmbreItems() {}

    /** Une lame courte : peu de dégâts en combat ouvert, mortelle par surprise. */
    public static final Item LAME = new SwordItem(ToolMaterials.IRON, new Item.Settings()
            .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.IRON, 0, -1.8f)));

    public static final Item BOMBE_FUMEE = new BombeFumee(new Item.Settings().maxCount(16));

    public static void enregistrer() {
        Registry.register(Registries.ITEM, Identifier.of(Ombre.MOD_ID, "lame_de_nacre"), LAME);
        Registry.register(Registries.ITEM, Identifier.of(Ombre.MOD_ID, "bombe_fumee"), BOMBE_FUMEE);
    }
}
