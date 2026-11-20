package fr.fenua.ombre;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce qui tourne en continu sur le serveur :
 * l'alarme qui gagne de garde en garde, les points de vue, la course libre,
 * et le « contrat rempli » quand une cible meurt.
 *
 * Les gardes de l'arène portent l'étiquette « ombre_garde » ; la cible, « ombre_cible ».
 * (On peut en créer d'autres avec /tag @e[...] add ombre_garde.)
 */
public final class Gardes {
    private Gardes() {}

    public static final String GARDE = "ombre_garde";
    public static final String CIBLE = "ombre_cible";
    public static final String ALERTE = "ombre_alerte";
    public static final String VUE = "ombre_vue";

    /** Distance à laquelle un garde qui t'a repéré prévient les autres. */
    public static final double PORTEE_ALARME = 20;

    private static final Identifier COURSE = Identifier.of(Ombre.MOD_ID, "course_libre");
    /** Points de vue déjà synchronisés : « uuid du joueur / uuid du marqueur ». */
    private static final Set<String> SYNCHRO = new HashSet<>();

    public static void tick(MinecraftServer serveur) {
        // Course libre : en sprintant, on monte un bloc entier sans sauter (chaque tick)
        for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
            EntityAttributeInstance pas = p.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (pas == null) continue;
            boolean veut = p.isSprinting() && !p.isSneaking();
            if (veut && !pas.hasModifier(COURSE))
                pas.addTemporaryModifier(new EntityAttributeModifier(COURSE, 0.5, EntityAttributeModifier.Operation.ADD_VALUE));
            else if (!veut && pas.hasModifier(COURSE))
                pas.removeModifier(COURSE);
        }

        if (serveur.getTicks() % 10 != 0) return;   // le reste, deux fois par seconde
        for (ServerWorld w : serveur.getWorlds()) {
            List<MobEntity> gardes = new ArrayList<>();
            List<Entity> vues = new ArrayList<>();
            for (Entity e : w.iterateEntities()) {
                if (e instanceof MobEntity m && e.getCommandTags().contains(GARDE) && m.isAlive()) gardes.add(m);
                else if (e.getType() == EntityType.MARKER && e.getCommandTags().contains(VUE)) vues.add(e);
            }
            alarme(w, gardes);
            pointsDeVue(w, vues);
        }
    }

    /** Un garde qui vient de te repérer sonne l'alarme ; les autres accourent. */
    private static void alarme(ServerWorld w, List<MobEntity> gardes) {
        for (MobEntity g : gardes) {
            boolean voit = g.getTarget() instanceof PlayerEntity;
            boolean alerte = g.getCommandTags().contains(ALERTE);
            if (voit && !alerte) {
                g.addCommandTag(ALERTE);
                PlayerEntity p = (PlayerEntity) g.getTarget();
                w.playSound(null, g.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.HOSTILE, 1.5f, 1.4f);
                w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, g.getX(), g.getEyeY() + 0.6, g.getZ(), 3, 0.2, 0.1, 0.2, 0);
                p.sendMessage(Text.literal("§c⚠ Repéré par " + g.getName().getString() + " !"), true);
                for (MobEntity autre : gardes) {
                    if (autre != g && autre.getTarget() == null && autre.squaredDistanceTo(g) < PORTEE_ALARME * PORTEE_ALARME) {
                        autre.setTarget(p);
                    }
                }
            } else if (!voit && alerte) {
                g.removeCommandTag(ALERTE);   // il t'a perdu de vue : l'alarme retombe
            }
        }
    }

    /** Se tenir sur un point de vue révèle tous les gardes du coin pendant 30 s. */
    private static void pointsDeVue(ServerWorld w, List<Entity> vues) {
        for (Entity v : vues) {
            for (ServerPlayerEntity p : w.getPlayers()) {
                String cle = p.getUuidAsString() + "/" + v.getUuidAsString();
                boolean deja = SYNCHRO.contains(cle);
                if (!deja) {
                    double d = p.squaredDistanceTo(v);
                    if (d < 48 * 48)   // une colonne de lumière qui attire l'œil
                        w.spawnParticles(p, ParticleTypes.END_ROD, true, v.getX(), v.getY() + 1.5, v.getZ(), 4, 0.15, 1.2, 0.15, 0.01);
                    if (d < 2.5 * 2.5 && p.isOnGround()) {
                        SYNCHRO.add(cle);
                        synchroniser(w, p, v);
                    }
                }
            }
        }
    }

    private static void synchroniser(ServerWorld w, ServerPlayerEntity p, Entity v) {
        int n = 0;
        for (MobEntity m : w.getEntitiesByClass(MobEntity.class, v.getBoundingBox().expand(64),
                e -> e.isAlive() && e.getCommandTags().contains(GARDE))) {
            m.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 600, 0, false, false));
            n++;
        }
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6Point de vue")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Synchronisé — " + n + " gardes révélés")));
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 0.7f);
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 30, 1.5, 0.5, 1.5, 0.02);
    }

    /** Une cible tombe : contrat rempli. */
    public static void apresMort(LivingEntity mort, DamageSource source) {
        if (!(mort.getWorld() instanceof ServerWorld w) || !mort.getCommandTags().contains(CIBLE)) return;
        Text titre = Text.literal("§6Contrat rempli");
        Text sous = Text.literal("§7" + mort.getName().getString() + " est tombé");
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.squaredDistanceTo(mort) > 128 * 128) continue;
            p.networkHandler.sendPacket(new TitleS2CPacket(titre));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(sous));
        }
        w.playSound(null, mort.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1f);
    }
}
