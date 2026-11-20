package fr.fenua.ombre.entite;

import fr.fenua.ombre.Ombre;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Les personnages du mod, et leurs œufs d'apparition (onglet « Objets »). */
public final class Entites {
    private Entites() {}

    public static EntityType<Passant> PASSANT;
    public static EntityType<Figure> FIGURE;
    public static EntityType<Gardes.Hallebardier> HALLEBARDIER;
    public static EntityType<Gardes.Arbaletrier> ARBALETRIER;
    public static EntityType<Gardes.Capitaine> CAPITAINE;
    public static final List<Item> OEUFS = new ArrayList<>();

    public static void enregistrer() {
        PASSANT = type("passant", EntityType.Builder.create(Passant::new, SpawnGroup.CREATURE));
        FIGURE = type("personnage", EntityType.Builder.create(Figure::new, SpawnGroup.CREATURE));
        HALLEBARDIER = type("hallebardier", EntityType.Builder.create(Gardes.Hallebardier::new, SpawnGroup.MONSTER));
        ARBALETRIER = type("arbaletrier", EntityType.Builder.create(Gardes.Arbaletrier::new, SpawnGroup.MONSTER));
        CAPITAINE = type("capitaine", EntityType.Builder.create(Gardes.Capitaine::new, SpawnGroup.MONSTER));
        FabricDefaultAttributeRegistry.register(PASSANT, Passant.attributs());
        FabricDefaultAttributeRegistry.register(FIGURE, Figure.attributs());
        // Moins brutaux que les vindicateurs : un coup de hallebarde enlève 4 cœurs, pas 6 et demi
        FabricDefaultAttributeRegistry.register(HALLEBARDIER, VindicatorEntity.createVindicatorAttributes()
                .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0));
        FabricDefaultAttributeRegistry.register(ARBALETRIER, PillagerEntity.createPillagerAttributes());
        FabricDefaultAttributeRegistry.register(CAPITAINE, VindicatorEntity.createVindicatorAttributes()
                .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0));
        oeuf("passant", PASSANT, 0x3B3B46, 0xD8B48C);
        oeuf("personnage", FIGURE, 0x1C1C1C, 0xD4AF37);
        oeuf("hallebardier", HALLEBARDIER, 0xB52A2A, 0xF0C419);
        oeuf("arbaletrier", ARBALETRIER, 0x6B4A2B, 0xB8BCC4);
        oeuf("capitaine", CAPITAINE, 0x1C1C1C, 0xE2B13C);
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> type(String nom, EntityType.Builder<T> b) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(Ombre.MOD_ID, nom),
                b.dimensions(0.6f, 1.95f).maxTrackingRange(10).build(nom));
    }

    private static void oeuf(String nom, EntityType<? extends MobEntity> t, int c1, int c2) {
        Item i = Registry.register(Registries.ITEM, Identifier.of(Ombre.MOD_ID, "oeuf_" + nom), new SpawnEggItem(t, c1, c2, new Item.Settings()));
        OEUFS.add(i);
    }
}
