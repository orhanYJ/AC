package fr.fenua.ombre;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Saut de la foi (foin, feuillage) et roulade (accroupi à l'atterrissage). */
public final class Chute {
    private Chute() {}

    /** Renvoie false pour annuler les dégâts. */
    public static boolean autoriserDegats(LivingEntity entite, DamageSource source, float degats) {
        if (!(entite instanceof PlayerEntity joueur) || !source.isIn(DamageTypeTags.IS_FALL)) return true;
        if (!(joueur.getWorld() instanceof ServerWorld sw)) return true;

        BlockPos dessous = BlockPos.ofFloored(joueur.getX(), joueur.getY() - 0.2, joueur.getZ());
        BlockState bloc = sw.getBlockState(dessous);

        if (bloc.isOf(Blocks.HAY_BLOCK) || bloc.isIn(BlockTags.LEAVES)) {
            sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, bloc),
                    joueur.getX(), joueur.getY() + 0.3, joueur.getZ(), 40, 0.6, 0.4, 0.6, 0.1);
            sw.playSound(null, dessous, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1f, 0.7f);
            joueur.sendMessage(Text.literal("§7✦ Saut de la foi"), true);
            return false;
        }
        // Roulade : accroupi en touchant le sol, jusqu'à ~9 blocs de chute
        if (joueur.isSneaking() && degats <= 6f) {
            joueur.sendMessage(Text.literal("§7✦ Roulade"), true);
            return false;
        }
        return true;
    }
}
