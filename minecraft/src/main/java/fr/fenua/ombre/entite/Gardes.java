package fr.fenua.ombre.entite;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

/**
 * Les forces de l'ordre de 1889 (personnages inventés). Ils reprennent le comportement des
 * vindicateurs et des pillards de Minecraft, mais ont un corps d'homme et un uniforme.
 */
public final class Gardes {
    private Gardes() {}

    /** Gardien de la paix : tunique bleu nuit, képi, sabre. Combat au corps à corps. */
    public static class Gardien extends VindicatorEntity {
        public Gardien(EntityType<? extends VindicatorEntity> t, World w) { super(t, w); }
        @Override
        protected void initEquipment(Random r, LocalDifficulty d) {
            equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    /** Gendarme : habit à baudriers blancs, bicorne, fusil (une arbalète, pour Minecraft). Il tire de loin. */
    public static class Gendarme extends PillagerEntity {
        public Gendarme(EntityType<? extends PillagerEntity> t, World w) { super(t, w); }
    }

    /** Officier : épaulettes d'or, képi galonné, sabre doré. C'est souvent lui, la cible. */
    public static class Officier extends VindicatorEntity {
        public Officier(EntityType<? extends VindicatorEntity> t, World w) { super(t, w); }
        @Override
        protected void initEquipment(Random r, LocalDifficulty d) {
            equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }
    }
}
