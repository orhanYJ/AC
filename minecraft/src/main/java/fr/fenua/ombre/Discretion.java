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
import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.objet.Effets;

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
        if (dansCachette(joueur) || dansLaFoule(joueur)) return 0.0;
        if (observateur instanceof LivingEntity o && o.hasStatusEffect(Effets.ENDORMI)) return 0.0;   // il dort
        // Déguisé en gardien : les gardes ne te remarquent qu'à 2 blocs
        if (joueur.hasStatusEffect(Effets.DEGUISE) && observateur != null && estGarde(observateur)) return 0.0;

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

    /** Un garde : ceux du mod (gardien, gendarme, officier) ou toute créature étiquetée « ombre_garde ». */
    public static boolean estGarde(Entity e) {
        return e instanceof fr.fenua.ombre.entite.Gardes.Gardien || e instanceof fr.fenua.ombre.entite.Gardes.Gendarme
                || e instanceof fr.fenua.ombre.entite.Gardes.Officier || e.getCommandTags().contains(Gardes.GARDE);
    }

    /** Dans la foule : au moins deux passants à moins de 2,5 blocs, et on marche (on ne court pas, on ne saute pas). */
    public static boolean dansLaFoule(PlayerEntity joueur) {
        if (joueur.isSprinting() || !joueur.isOnGround()) return false;
        return joueur.getWorld().getEntitiesByClass(fr.fenua.ombre.entite.Passant.class,
                joueur.getBoundingBox().expand(2.5), e -> e.isAlive()).size() >= 2;
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

    /**
     * Dans une meule de foin, un puits, une cabine à rideau (même debout), assis sur un banc,
     * accroupi dans une charrette de foin, ou accroupi dans les hautes herbes,
     * une fougère, un buisson, ou la tête sous des feuilles.
     */
    public static boolean dansCachette(PlayerEntity joueur) {
        World w = joueur.getWorld();
        BlockPos pieds = joueur.getBlockPos();
        if (w.getBlockState(pieds).isOf(OmbreBlocs.MEULE) && w.getBlockState(pieds.up()).isOf(OmbreBlocs.MEULE)) return true;
        if (w.getBlockState(pieds).isOf(OmbreBlocs.PUITS) || w.getBlockState(pieds).getBlock() instanceof fr.fenua.ombre.bloc.Cachettes.Cabine) return true;
        // Assis sur un banc : on se fond dans le décor
        if (joueur.getVehicle() != null && joueur.getVehicle().getCommandTags().contains(fr.fenua.ombre.bloc.Decor.Banc.SIEGE)) return true;
        // Accroupi dans une charrette de foin
        if (joueur.isSneaking() && w.getBlockState(pieds).isOf(OmbreBlocs.CHARRETTE)) return true;
        if (!joueur.isSneaking()) return false;
        return cache(w.getBlockState(pieds)) || cache(w.getBlockState(pieds.up()));
    }

    private static boolean cache(BlockState s) {
        return s.isOf(Blocks.TALL_GRASS) || s.isOf(Blocks.LARGE_FERN) || s.isOf(Blocks.SWEET_BERRY_BUSH)
                || s.isOf(OmbreBlocs.MEULE) || s.isIn(BlockTags.LEAVES);
    }
}
