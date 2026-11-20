package fr.fenua.ombre;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Les assassinats à la lame de nacre.
 * - Furtif : la créature ne t'a pas pris pour cible (elle ne t'a pas repéré).
 * - Aérien : tu frappes en tombant (plus d'1,5 bloc de chute) ; la chute est annulée.
 * - Double : une lame dans chaque main, un deuxième garde distrait tout près tombe aussi.
 * Un garde qui t'a repéré, lui, encaisse un coup normal (faible).
 */
public final class Assassinat {
    private Assassinat() {}

    public static ActionResult auCoup(PlayerEntity joueur, World world, Hand main, Entity cible, @Nullable EntityHitResult hit) {
        if (world.isClient() || joueur.isSpectator()) return ActionResult.PASS;
        if (!joueur.getMainHandStack().isOf(OmbreItems.LAME)) return ActionResult.PASS;
        if (!(cible instanceof LivingEntity vivant) || cible instanceof PlayerEntity || cible instanceof WitherEntity)
            return ActionResult.PASS;

        boolean aerien = joueur.fallDistance > 1.5f && !joueur.isOnGround();
        boolean furtif = !repere(vivant, joueur);
        if (!aerien && !furtif) return ActionResult.PASS;   // combat normal

        ServerWorld sw = (ServerWorld) world;
        tuer(sw, joueur, vivant);
        String nom = aerien ? "Assassinat aérien" : "Assassinat";

        if (aerien) joueur.fallDistance = 0;

        // Double assassinat : une seconde lame dans l'autre main
        if (joueur.getOffHandStack().isOf(OmbreItems.LAME)) {
            for (LivingEntity autre : sw.getEntitiesByClass(LivingEntity.class, vivant.getBoundingBox().expand(2.5),
                    e -> e != vivant && e != joueur && e.isAlive() && !(e instanceof PlayerEntity) && !repere(e, joueur))) {
                tuer(sw, joueur, autre);
                nom = "Double assassinat";
                break;
            }
        }
        joueur.sendMessage(Text.literal("§7✦ " + nom), true);
        return ActionResult.SUCCESS;   // on remplace le coup normal
    }

    /** Vrai si la créature a le joueur pour cible (elle sait où il est). */
    public static boolean repere(LivingEntity e, PlayerEntity joueur) {
        return e instanceof MobEntity m && m.getTarget() == joueur;
    }

    private static void tuer(ServerWorld sw, PlayerEntity joueur, LivingEntity v) {
        v.damage(sw.getDamageSources().playerAttack(joueur), 1000f + v.getMaxHealth());
        sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, v.getX(), v.getBodyY(0.6), v.getZ(), 1, 0, 0, 0, 0);
        sw.spawnParticles(ParticleTypes.CRIT, v.getX(), v.getBodyY(0.5), v.getZ(), 8, 0.2, 0.3, 0.2, 0.1);
        // Un bruit sourd et bas : on ne l'entend pas de loin
        sw.playSound(null, v.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.35f, 0.6f);
    }
}
