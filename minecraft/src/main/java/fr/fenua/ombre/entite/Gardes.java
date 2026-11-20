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
 * La garde de la ville de Prague, en 1600 (personnages inventés). Ils reprennent le comportement
 * des vindicateurs et des pillards de Minecraft, mais ont un corps d'homme et une livrée.
 */
public final class Gardes {
    private Gardes() {}

    /** Hallebardier : morion d'acier, livrée rouge et jaune, hallebarde (une hache, pour Minecraft). Au corps à corps. */
    public static class Hallebardier extends VindicatorEntity {
        public Hallebardier(EntityType<? extends VindicatorEntity> t, World w) { super(t, w); }
        @Override
        protected void initEquipment(Random r, LocalDifficulty d) {
            equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }
    }

    /** Arbalétrier : pourpoint de cuir, cabasset, arbalète. Il tire de loin, souvent depuis les toits. */
    public static class Arbaletrier extends PillagerEntity {
        public Arbaletrier(EntityType<? extends PillagerEntity> t, World w) { super(t, w); }
    }

    /** Capitaine : noir et or, chapeau à plume, épée dorée. Les cibles de la Confrérie portent ce rang. */
    public static class Capitaine extends VindicatorEntity {
        public Capitaine(EntityType<? extends VindicatorEntity> t, World w) { super(t, w); }
        @Override
        protected void initEquipment(Random r, LocalDifficulty d) {
            equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }
    }
}
