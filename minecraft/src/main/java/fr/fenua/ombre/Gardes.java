package fr.fenua.ombre;

import net.minecraft.entity.Entity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.ac.*;
import fr.fenua.ombre.entite.Figure;
import fr.fenua.ombre.entite.Passant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** Caché à plus de cette distance d'un poursuivant, il perd ta trace. */
    public static final double PERDU = 3.5;

    /** Distance à laquelle un garde qui t'a repéré prévient les autres. */
    public static final double PORTEE_ALARME = 20;

    /** Un repère nommé (marqueur) : il donne son nom aux points de vue proches. */
    public static final String LIEU = "ombre_lieu";
    /** Étiquette d'un point de vue synchronisé, sur le joueur : « ombre_vue:<position>:<nom> ». */
    public static final String VUE_SYNCHRO = "ombre_vue:";

    private static final Identifier COURSE = Identifier.of(Ombre.MOD_ID, "course_libre");
    /** Dernière alerte de chaque joueur (pour la notoriété : une alerte toutes les 30 s au plus). */
    private static final Map<UUID, Long> DERNIERE_ALERTE = new HashMap<>();
    /** Les marqueurs de mission de chaque monde (cibles vivantes, personnages à rencontrer). */
    private static final Map<ServerWorld, List<Entity>> MARQUEURS = new HashMap<>();

    public static void tick(MinecraftServer serveur) {
        Assassinat.tickExecutions();

        // Course libre : en sprintant, on monte un bloc entier sans sauter (chaque tick)
        for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
            if (p.isSprinting() && p.hasStatusEffect(fr.fenua.ombre.objet.Effets.DEGUISE)) {
                p.removeStatusEffect(fr.fenua.ombre.objet.Effets.DEGUISE);
                p.sendMessage(Text.literal("§7Déguisement perdu : un hallebardier ne court pas."), true);
            }
            EntityAttributeInstance pas = p.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (pas == null) continue;
            boolean veut = p.isSprinting() && !p.isSneaking();
            if (veut && !pas.hasModifier(COURSE))
                pas.addTemporaryModifier(new EntityAttributeModifier(COURSE, 0.5, EntityAttributeModifier.Operation.ADD_VALUE));
            else if (!veut && pas.hasModifier(COURSE))
                pas.removeModifier(COURSE);
        }

        // Toutes les 2 ticks : la détection progressive ; toutes les 10 : alarme, points de vue
        if (serveur.getTicks() % 2 == 0) {
            boolean lent = serveur.getTicks() % 10 == 0;
            for (ServerWorld w : serveur.getWorlds()) {
                List<MobEntity> gardes = new ArrayList<>();
                List<Entity> vues = new ArrayList<>(), marqueurs = new ArrayList<>();
                for (Entity e : w.iterateEntities()) {
                    if (e instanceof MobEntity m && m.isAlive() && Discretion.estGarde(m)) {
                        gardes.add(m);
                        if (m.getCommandTags().contains(CIBLE)) marqueurs.add(m);
                    }
                    else if (!lent) continue;
                    else if (e instanceof Figure f && (f.qui() == Figure.TYCHO || f.qui() == Figure.KEPLER)) marqueurs.add(f);
                    else if (e.getType() == EntityType.MARKER && e.getCommandTags().contains(VUE)) vues.add(e);
                    else if (e.getType() == EntityType.ARMOR_STAND && e.getCommandTags().contains(fr.fenua.ombre.bloc.Decor.Banc.SIEGE)
                            && !e.hasPassengers()) e.discard();   // on s'est relevé du banc
                }
                Detection.tick(w, gardes);
                if (lent) {
                    MARQUEURS.put(w, marqueurs);
                    alarme(w, gardes);
                    pointsDeVue(w, vues);
                    perchoirs(w);
                }
            }
        }

        // Quatre fois par seconde : dire à chaque joueur ce que pensent les gardes autour de lui
        if (serveur.getTicks() % 5 == 0) {
            for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
                if (!ServerPlayNetworking.canSend(p, EtatPayload.ID)) continue;
                List<Integer> ids = new ArrayList<>(), niveaux = new ArrayList<>();
                boolean cache = Discretion.dansCachette(p);
                for (MobEntity m : p.getServerWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(40),
                        m -> m.isAlive() && (m.getTarget() == p || Discretion.estGarde(m)))) {
                    boolean surMoi = m.getTarget() == p;
                    // Caché, on se fait oublier de ceux qui ne sont pas sur nous (comme dans Assassin's Creed)
                    if (cache && m.squaredDistanceTo(p) > PERDU * PERDU) {
                        if (surMoi) { m.setTarget(null); m.getNavigation().stop(); }
                        Detection.fixer(m, 0f);
                        continue;
                    }
                    int n = surMoi ? 100 : Math.round(Detection.niveau(m) * 100);
                    if (n <= 0) continue;
                    ids.add(m.getId());
                    niveaux.add(n);
                }
                ServerPlayNetworking.send(p, new EtatPayload(ids, niveaux, Zones.etat(p), Notoriete.get(p)));
            }
        }

        // Une fois par seconde : le journal de mission, les points de vue connus, les marqueurs
        if (serveur.getTicks() % 20 == 0) {
            for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
                if (!ServerPlayNetworking.canSend(p, JournalPayload.ID)) continue;
                boolean mission = p.getCommandTags().contains(Mission.ACTIVE);
                List<Long> vues = new ArrayList<>(), cibles = new ArrayList<>();
                List<String> noms = new ArrayList<>();
                for (String t : p.getCommandTags()) {
                    if (!t.startsWith(VUE_SYNCHRO)) continue;
                    String[] x = t.split(":", 3);
                    try { vues.add(Long.parseLong(x[1])); noms.add(x.length > 2 ? x[2] : "Point de vue"); } catch (NumberFormatException ignore) {}
                }
                boolean quetes = fr.fenua.ombre.ac.Quetes.active(p);
                if (quetes) cibles.addAll(fr.fenua.ombre.ac.Quetes.marqueurs(p));
                for (Entity e : quetes ? List.<Entity>of() : MARQUEURS.getOrDefault(p.getServerWorld(), List.of())) {
                    if (!e.isAlive() || e.squaredDistanceTo(p) > 300 * 300) continue;
                    if (e instanceof Figure f && (!mission || p.getCommandTags().contains(f.qui() == Figure.TYCHO ? "ombre_m_tycho" : "ombre_m_kepler"))) continue;
                    cibles.add(e.getBlockPos().asLong());
                }
                BlockPos balise = Defis.prochaineBalise(p);
                if (balise != null) cibles.add(balise.asLong());
                List<String> tuto = fr.fenua.ombre.initiation.Cours.lignes(p);
                if (tuto != null) {
                    // En initiation : le panneau des leçons, et le losange vers le but de l'étape
                    cibles.clear();
                    BlockPos but = fr.fenua.ombre.initiation.Cours.but(p);
                    if (but != null) cibles.add(but.asLong());
                }
                List<String> journal = tuto != null ? tuto : quetes ? fr.fenua.ombre.ac.Quetes.lignesEtFacultatifs(p) : mission ? Mission.lignes(p) : List.of();
                ServerPlayNetworking.send(p, new JournalPayload(journal, vues, noms, cibles));
            }
        }

        // Toutes les 10 secondes : sans alerte, la notoriété s'efface doucement (pas quand on est recherché)
        if (serveur.getTicks() % 200 == 0) {
            for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
                int n = Notoriete.get(p);
                if (n <= 0 || n >= Notoriete.RECHERCHE) continue;
                if (!p.getServerWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(48), m -> m.getTarget() == p).isEmpty()) continue;
                Notoriete.ajouter(p, -1, null);
            }
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
                if (p instanceof ServerPlayerEntity sp) {
                    long t = w.getTime();
                    if (t - DERNIERE_ALERTE.getOrDefault(sp.getUuid(), -10000L) > 600) Notoriete.ajouter(sp, 15, "alerte donnée");
                    DERNIERE_ALERTE.put(sp.getUuid(), t);
                }
                for (MobEntity autre : gardes) {
                    if (autre != g && autre.getTarget() == null && autre.squaredDistanceTo(g) < PORTEE_ALARME * PORTEE_ALARME) {
                        Detection.fixer(autre, 1f);
                        Detection.forcer(() -> autre.setTarget(p));
                    }
                }
            } else if (!voit && alerte) {
                g.removeCommandTag(ALERTE);   // il t'a perdu de vue : l'alarme retombe
            }
        }
    }

    /** Se tenir sur un point de vue révèle tous les gardes du coin pendant 30 s, et le retient pour le voyage rapide. */
    private static void pointsDeVue(ServerWorld w, List<Entity> vues) {
        for (Entity v : vues) {
            BlockPos pos = BlockPos.ofFloored(v.getPos());
            for (ServerPlayerEntity p : w.getPlayers()) {
                if (dejaVu(p, pos)) continue;
                double d = p.squaredDistanceTo(v);
                if (d < 48 * 48)   // une colonne de lumière qui attire l'œil
                    w.spawnParticles(p, ParticleTypes.END_ROD, true, v.getX(), v.getY() + 1.5, v.getZ(), 4, 0.15, 1.2, 0.15, 0.01);
                if (d < 2.5 * 2.5 && p.isOnGround())
                    synchroniser(w, p, v.getPos(), pos, v.hasCustomName() ? v.getCustomName().getString() : lieu(w, pos));
            }
        }
    }

    /** Se tenir debout sur un bloc « perchoir » ou une gargouille synchronise aussi (une fois chacun). */
    private static void perchoirs(ServerWorld w) {
        for (ServerPlayerEntity p : w.getPlayers()) {
            BlockPos dessous = p.getBlockPos().down();
            if (!p.isOnGround()) continue;
            if (!pointDeVue(w.getBlockState(dessous))) {
                dessous = BlockPos.ofFloored(p.getX(), p.getY() - 0.1, p.getZ());
                if (!pointDeVue(w.getBlockState(dessous))) continue;
            }
            BlockPos pos = dessous.up();
            if (!dejaVu(p, pos)) synchroniser(w, p, Vec3d.ofCenter(dessous), pos, lieu(w, pos));
        }
    }

    private static boolean pointDeVue(net.minecraft.block.BlockState s) {
        return s.isOf(OmbreBlocs.PERCHOIR) || s.isOf(OmbreBlocs.GARGOUILLE);
    }

    /** Ce point de vue est-il déjà synchronisé (à 3 blocs près) pour ce joueur ? */
    public static boolean dejaVu(ServerPlayerEntity p, BlockPos pos) {
        for (String t : p.getCommandTags()) {
            if (!t.startsWith(VUE_SYNCHRO)) continue;
            try {
                BlockPos b = BlockPos.fromLong(Long.parseLong(t.split(":", 3)[1]));
                if (b.getSquaredDistance(pos) <= 9) return true;
            } catch (RuntimeException ignore) {}
        }
        return false;
    }

    /** Le nom du lieu : le repère nommé le plus proche (32 blocs), sinon les coordonnées. */
    private static String lieu(ServerWorld w, BlockPos pos) {
        Entity meilleur = null;
        double dmin = 32 * 32;
        for (Entity e : w.getEntitiesByType(EntityType.MARKER, new Box(pos).expand(32), e -> e.getCommandTags().contains(LIEU) && e.hasCustomName())) {
            double d = e.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (d < dmin) { dmin = d; meilleur = e; }
        }
        return meilleur != null ? meilleur.getCustomName().getString() : "Point de vue (" + pos.getX() + ", " + pos.getZ() + ")";
    }

    private static void synchroniser(ServerWorld w, ServerPlayerEntity p, Vec3d centre, BlockPos pos, String nom) {
        p.addCommandTag(VUE_SYNCHRO + pos.asLong() + ":" + nom);
        int n = 0;
        for (MobEntity m : w.getEntitiesByClass(MobEntity.class, new Box(centre, centre).expand(64),
                e -> e.isAlive() && Discretion.estGarde(e))) {
            m.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 600, 0, false, false));
            n++;
        }
        if (ServerPlayNetworking.canSend(p, SynchroPayload.ID)) ServerPlayNetworking.send(p, new SynchroPayload());
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6Point de vue")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7" + nom + " — " + n + " gardes révélés")));
        p.sendMessage(Text.literal("§6✦ " + nom + "§7 : synchronisé. Voyage rapide possible (touche J)."), false);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 0.7f);
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 30, 1.5, 0.5, 1.5, 0.02);
        Mission.marquer(p, "ombre_m_vue");
        fr.fenua.ombre.ac.Quetes.evenement(p, "synchro", nom);
    }

    /**
     * Après une mort : un hallebardier tué laisse sa tenue ; une cible, c'est « contrat rempli » ;
     * un innocent tué, c'est la « désynchronisation » (comme dans Assassin's Creed) et la notoriété qui bondit.
     */
    public static void apresMort(LivingEntity mort, DamageSource source) {
        if (mort instanceof fr.fenua.ombre.entite.Gardes.Hallebardier && source.getAttacker() instanceof PlayerEntity
                && !mort.getCommandTags().contains(fr.fenua.ombre.initiation.Initiation.TAG))
            mort.dropStack(new net.minecraft.item.ItemStack(fr.fenua.ombre.objet.Objets.TENUE));
        if (!(mort.getWorld() instanceof ServerWorld w)) return;
        if (source.getAttacker() instanceof ServerPlayerEntity tueur) fr.fenua.ombre.ac.Quetes.mort(mort, tueur);
        if (mort instanceof Passant && source.getAttacker() instanceof ServerPlayerEntity tueur) {
            tueur.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§4Désynchronisation")));
            tueur.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7L'Ombre ne tue pas d'innocents")));
            tueur.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, false, false));
            tueur.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2, false, false));
            w.playSound(null, tueur.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 0.6f);
            Notoriete.ajouter(tueur, 50, "un innocent est mort");
            return;
        }
        if (!mort.getCommandTags().contains(CIBLE)) return;
        Text titre = Text.literal("§6Contrat rempli");
        Text sous = Text.literal("§7" + mort.getName().getString() + " est tombé");
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.squaredDistanceTo(mort) > 128 * 128) continue;
            p.networkHandler.sendPacket(new TitleS2CPacket(titre));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(sous));
            if (mort.getCommandTags().contains("ombre_cible_ottokar")) Mission.marquer(p, "ombre_m_ottokar");
            if (mort.getCommandTags().contains("ombre_cible_kragl")) Mission.marquer(p, "ombre_m_kragl");
        }
        w.playSound(null, mort.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1f);
    }
}
