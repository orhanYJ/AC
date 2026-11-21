package fr.fenua.ombre.ac;

import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.Gardes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Le voyage rapide : on retourne d'un coup à un point de vue déjà synchronisé (touche J).
 * Comme dans Assassin's Creed, c'est impossible en plein combat ou sous le regard d'un garde.
 */
public final class Voyage {
    private Voyage() {}

    public static void partir(ServerPlayerEntity p, long cle) {
        ServerWorld w = p.getServerWorld();
        BlockPos but = BlockPos.fromLong(cle);
        if (!Gardes.dejaVu(p, but)) return;
        boolean menace = !w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(48),
                m -> m.isAlive() && (m.getTarget() == p || (Discretion.estGarde(m) && Detection.niveau(m) >= 0.45f))).isEmpty();
        if (menace) {
            p.sendMessage(Text.literal("§cImpossible de voyager : des gardes te cherchent."), true);
            return;
        }
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 25, 0.4, 0.8, 0.4, 0.02);
        p.teleport(w, but.getX() + 0.5, but.getY(), but.getZ() + 0.5, p.getYaw(), 10f);
        p.fallDistance = 0;
        p.addCommandTag("ombre_voyage");      // pour l'initiation
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 25, 0, false, false));
        w.playSound(null, but, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.8f, 0.8f);
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Voyage rapide")));
    }
}
