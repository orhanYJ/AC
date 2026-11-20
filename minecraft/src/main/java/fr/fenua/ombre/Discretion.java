package fr.fenua.ombre;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * La discrétion : à quelle distance un garde te repère.
 * Minecraft multiplie la portée de vue d'une créature par ce facteur
 * (1 = normal, 0 = seulement à 2 blocs, le minimum du jeu).
 */
public final class Discretion {
    private Discretion() {}

    /** Angle de vue des gardes : au-delà de ±70° de leur regard, ils voient mal. */
    private static final double COS_CONE = Math.cos(Math.toRadians(70));

    public static double facteur(PlayerEntity joueur, Entity observateur) {
        double f = 1.0;
        if (dansCachette(joueur)) return 0.0;

        if (joueur.isSneaking()) f *= 0.75;          // s'ajoute au ×0,8 de Minecraft
        if (joueur.isSprinting()) f *= 1.3;          // courir attire l'œil

        World w = joueur.getWorld();
        int lumiere = w.getLightLevel(BlockPos.ofFloored(joueur.getEyePos()));
        if (lumiere <= 4) f *= 0.55;                 // dans le noir
        else if (lumiere <= 7) f *= 0.8;             // dans la pénombre

        if (observateur instanceof LivingEntity obs) {
            if (obs.hasStatusEffect(StatusEffects.BLINDNESS)) f *= 0.1;   // pris dans la fumée
            if (observateur instanceof HostileEntity && dansLeDos(obs, joueur)) f *= 0.3;
        }
        return f;
    }

    /** Vrai si le joueur est en dehors du cône de vue de l'observateur. */
    public static boolean dansLeDos(LivingEntity obs, PlayerEntity joueur) {
        double dx = joueur.getX() - obs.getX(), dz = joueur.getZ() - obs.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) return false;
        float yaw = obs.getHeadYaw() * MathHelper.RADIANS_PER_DEGREE;
        double rx = -MathHelper.sin(yaw), rz = MathHelper.cos(yaw);
        return (dx * rx + dz * rz) / len < COS_CONE;
    }

    /** Accroupi dans les hautes herbes, une fougère, un buisson, ou la tête sous des feuilles. */
    public static boolean dansCachette(PlayerEntity joueur) {
        if (!joueur.isSneaking()) return false;
        World w = joueur.getWorld();
        BlockPos pieds = joueur.getBlockPos();
        return cache(w.getBlockState(pieds)) || cache(w.getBlockState(pieds.up()));
    }

    private static boolean cache(BlockState s) {
        return s.isOf(Blocks.TALL_GRASS) || s.isOf(Blocks.LARGE_FERN) || s.isOf(Blocks.SWEET_BERRY_BUSH)
                || s.isOf(Blocks.HAY_BLOCK) || s.isIn(BlockTags.LEAVES);
    }
}
