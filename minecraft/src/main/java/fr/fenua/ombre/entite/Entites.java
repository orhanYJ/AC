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
    public static EntityType<Gardes.Gardien> GARDIEN;
    public static EntityType<Gardes.Gendarme> GENDARME;
    public static EntityType<Gardes.Officier> OFFICIER;
    public static final List<Item> OEUFS = new ArrayList<>();

    public static void enregistrer() {
        PASSANT = type("passant", EntityType.Builder.create(Passant::new, SpawnGroup.CREATURE));
        GARDIEN = type("gardien", EntityType.Builder.create(Gardes.Gardien::new, SpawnGroup.MONSTER));
        GENDARME = type("gendarme", EntityType.Builder.create(Gardes.Gendarme::new, SpawnGroup.MONSTER));
        OFFICIER = type("officier", EntityType.Builder.create(Gardes.Officier::new, SpawnGroup.MONSTER));
        FabricDefaultAttributeRegistry.register(PASSANT, Passant.attributs());
        FabricDefaultAttributeRegistry.register(GARDIEN, VindicatorEntity.createVindicatorAttributes());
        FabricDefaultAttributeRegistry.register(GENDARME, PillagerEntity.createPillagerAttributes());
        FabricDefaultAttributeRegistry.register(OFFICIER, VindicatorEntity.createVindicatorAttributes());
        oeuf("passant", PASSANT, 0x3B3B46, 0xD8B48C);
        oeuf("gardien", GARDIEN, 0x1F2A48, 0xC0C6D0);
        oeuf("gendarme", GENDARME, 0x1F2A48, 0xF0F0F0);
        oeuf("officier", OFFICIER, 0x1F2A48, 0xE2B13C);
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
