package fr.fenua.ombre;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Bombe de fumée : un nuage éclate aux pieds du joueur.
 * Les créatures à moins de 6 blocs perdent leur cible et sont aveuglées 5 s :
 * on peut alors les assassiner même de face (elles ne te « voient » plus).
 */
public class BombeFumee extends Item {
    public static final double RAYON = 6.0;

    public BombeFumee(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity joueur, Hand main) {
        ItemStack pile = joueur.getStackInHand(main);
        if (world instanceof ServerWorld sw) {
            double x = joueur.getX(), y = joueur.getY(), z = joueur.getZ();
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 60, 2.2, 1.0, 2.2, 0.02);
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 120, 2.5, 0.8, 2.5, 0.05);
            sw.playSound(null, joueur.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.2f, 0.6f);

            for (MobEntity m : sw.getEntitiesByClass(MobEntity.class, joueur.getBoundingBox().expand(RAYON), e -> e.isAlive())) {
                m.setTarget(null);
                fr.fenua.ombre.ac.Detection.fixer(m, 0f);
                m.getNavigation().stop();
                m.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0));
                m.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3));
                m.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
            }
            joueur.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1, false, false, true));
            joueur.getItemCooldownManager().set(this, 100);
            if (!joueur.getAbilities().creativeMode) pile.decrement(1);
        }
        return TypedActionResult.success(pile, world.isClient());
    }
}
