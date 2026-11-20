package fr.fenua.ombre;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Les deux touches : Instinct (V) et Siffler (B). */
public final class Pouvoirs {
    private Pouvoirs() {}

    private static final Map<UUID, Long> PROCHAIN_INSTINCT = new HashMap<>();
    private static final Map<UUID, Long> PROCHAIN_SIFFLET = new HashMap<>();

    public static void agir(ServerPlayerEntity p, int action) {
        if (p.isSpectator()) return;
        ServerWorld w = p.getServerWorld();
        long t = w.getTime();
        if (action == ActionPayload.INSTINCT) instinct(p, w, t);
        else if (action == ActionPayload.SIFFLER) siffler(p, w, t);
    }

    /** Révèle 10 s les gardes et les monstres à 40 blocs, à travers les murs. */
    private static void instinct(ServerPlayerEntity p, ServerWorld w, long t) {
        long pret = PROCHAIN_INSTINCT.getOrDefault(p.getUuid(), 0L);
        if (t < pret) {
            p.sendMessage(Text.literal("§7Instinct : encore " + (pret - t) / 20 + " s"), true);
            return;
        }
        PROCHAIN_INSTINCT.put(p.getUuid(), t + 15 * 20);
        int n = 0;
        for (MobEntity m : w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(40),
                e -> e.isAlive() && (e instanceof HostileEntity || e.getCommandTags().contains(Gardes.GARDE)))) {
            m.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, false));
            n++;
        }
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.6f, 0.5f);
        p.sendMessage(Text.literal("§bInstinct : " + n + " ennemis révélés"), true);
    }

    /** Le garde le plus proche (12 blocs) vient voir d'où vient le bruit, sans donner l'alarme. */
    private static void siffler(ServerPlayerEntity p, ServerWorld w, long t) {
        if (t < PROCHAIN_SIFFLET.getOrDefault(p.getUuid(), 0L)) return;
        PROCHAIN_SIFFLET.put(p.getUuid(), t + 4 * 20);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 0.9f, 1.6f);
        w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(12),
                        e -> e.isAlive() && e.getTarget() == null && e.getCommandTags().contains(Gardes.GARDE))
                .stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)))
                .ifPresentOrElse(g -> {
                    g.getNavigation().startMovingTo(p.getX(), p.getY(), p.getZ(), 0.9);
                    g.getLookControl().lookAt(p.getX(), p.getEyeY(), p.getZ());
                    p.sendMessage(Text.literal("§7" + g.getName().getString() + " vient voir…"), true);
                }, () -> p.sendMessage(Text.literal("§7Personne ne t'a entendu."), true));
    }
}
