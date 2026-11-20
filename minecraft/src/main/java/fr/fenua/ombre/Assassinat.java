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

    // ───────────── la touche R ─────────────

    /** Portées, identiques côté joueur (pour le contour rouge) et côté serveur (pour vérifier). */
    public static final double SOL_H = 3.2, SOL_V = 1.3;          // au sol : 3,2 blocs, même niveau
    public static final double AIR_H = 3.2, AIR_MIN = 1.4, AIR_MAX = 9;   // aérien : le garde est en dessous
    public static final double CACHE_H = 3.6;                         // depuis une cachette : tout autour
    public static final String CACHETTE = "Assassinat depuis la cachette";
    public static final double REBORD_H = 2.8, REBORD_MIN = 0.8, REBORD_MAX = 3.2; // depuis un rebord : il est au-dessus

    /** Quel assassinat est possible sur cette créature, ou null. Le même calcul sert des deux côtés. */
    @Nullable
    public static String typePossible(PlayerEntity joueur, LivingEntity v, boolean repere, boolean cachette) {
        double dx = v.getX() - joueur.getX(), dz = v.getZ() - joueur.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        double dy = joueur.getY() - v.getY();
        if (dy >= AIR_MIN && dy <= AIR_MAX && h <= AIR_H) return "Assassinat aérien";
        if (repere) return null;
        if (-dy >= REBORD_MIN && -dy <= REBORD_MAX && h <= REBORD_H) return "Assassinat depuis le rebord";
        if (cachette && Math.abs(dy) <= SOL_V && h <= CACHE_H) return CACHETTE;
        if (Math.abs(dy) <= SOL_V && h <= SOL_H) return "Assassinat";
        return null;
    }

    /** Peut-on viser cette créature ? (vivante, pas un joueur, pas un boss, ni un civil ni un personnage réel) */
    public static boolean visable(Entity e) {
        return e instanceof MobEntity m && m.isAlive() && !(e instanceof WitherEntity)
                && !(e instanceof fr.fenua.ombre.entite.Passant) && !(e instanceof fr.fenua.ombre.entite.Figure)
                && !(e instanceof net.minecraft.entity.passive.IronGolemEntity);
    }

    /** Le joueur porte-t-il la lame (n'importe où dans son inventaire) ? */
    public static boolean aLaLame(PlayerEntity joueur) {
        return joueur.getInventory().containsAny(s -> s.isOf(OmbreItems.LAME));
    }

    /** Touche R : le joueur demande à assassiner la créature n° id (celle qu'il voit en rouge). */
    public static void parTouche(net.minecraft.server.network.ServerPlayerEntity joueur, int id) {
        ServerWorld sw = joueur.getServerWorld();
        Entity e = sw.getEntityById(id);
        if (!aLaLame(joueur) || !visable(e) || joueur.isSpectator()) return;
        LivingEntity v = (LivingEntity) e;
        String nom = typePossible(joueur, v, repere(v, joueur), Discretion.dansCachette(joueur));
        if (nom == null || !joueur.canSee(v)) return;

        // L'assassinat est mis en scène : la victime est figée, le joueur fait son geste, puis le coup tombe
        int type = nom.equals("Assassinat aérien") ? ExecutionPayload.AERIEN : nom.equals(CACHETTE) ? ExecutionPayload.CACHETTE
                : nom.startsWith("Assassinat depuis le rebord") ? ExecutionPayload.REBORD : ExecutionPayload.SOL;
        for (Execution x : EN_COURS) if (x.victime == v || x.joueur == joueur) return;   // déjà en cours
        joueur.fallDistance = 0;
        if (v instanceof MobEntity m) { m.setTarget(null); m.getNavigation().stop(); }
        EN_COURS.add(new Execution(joueur, v, type, nom));
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(joueur, ExecutionPayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(joueur,
                    new ExecutionPayload(type, v.getId(), v.getX(), v.getY(), v.getZ(), v.getBodyYaw()));
        sw.playSound(null, joueur.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.4f, 1.4f);
    }

    /** Un assassinat en train de se jouer. */
    private static final class Execution {
        final net.minecraft.server.network.ServerPlayerEntity joueur;
        final LivingEntity victime;
        final int type;
        final String nom;
        int t;

        Execution(net.minecraft.server.network.ServerPlayerEntity j, LivingEntity v, int type, String nom) {
            this.joueur = j; this.victime = v; this.type = type; this.nom = nom;
        }
    }

    private static final java.util.List<Execution> EN_COURS = new java.util.ArrayList<>();

    /** Chaque tick : la victime reste figée (ou est tirée), puis le coup tombe à la fin du geste. */
    public static void tickExecutions() {
        java.util.Iterator<Execution> it = EN_COURS.iterator();
        while (it.hasNext()) {
            Execution x = it.next();
            x.t++;
            LivingEntity v = x.victime;
            if (!v.isAlive() || x.joueur.isRemoved() || x.joueur.getWorld() != v.getWorld()) { it.remove(); continue; }
            if (v instanceof MobEntity m) { m.setTarget(null); m.getNavigation().stop(); }
            switch (x.type) {
                case ExecutionPayload.CACHETTE -> {
                    // On l'attire dans la cachette
                    net.minecraft.util.math.Vec3d p = v.getPos().lerp(x.joueur.getPos(), 0.3);
                    v.requestTeleport(p.x, p.y, p.z);
                }
                case ExecutionPayload.REBORD -> {
                    // On le tire par-dessus le bord : il passe devant le joueur et tombe
                    if (x.t == 3) {
                        double dx = x.joueur.getX() - v.getX(), dz = x.joueur.getZ() - v.getZ(), n = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                        v.setVelocity(dx / n * 0.45, 0.25, dz / n * 0.45);
                        v.velocityModified = true;
                    }
                }
                default -> {
                    v.setVelocity(0, Math.min(0, v.getVelocity().y), 0);
                    v.velocityModified = true;
                }
            }
            if (x.t >= ExecutionPayload.DUREE[x.type]) {
                it.remove();
                ServerWorld sw = (ServerWorld) v.getWorld();
                x.joueur.fallDistance = 0;
                tuer(sw, x.joueur, v);
                x.joueur.swingHand(Hand.MAIN_HAND, true);
                String nom = x.nom;
                if (x.joueur.getOffHandStack().isOf(OmbreItems.LAME)) {
                    for (LivingEntity autre : sw.getEntitiesByClass(LivingEntity.class, v.getBoundingBox().expand(2.5),
                            e -> e != v && visable(e) && !repere(e, x.joueur))) {
                        tuer(sw, x.joueur, autre);
                        x.joueur.swingHand(Hand.OFF_HAND, true);
                        nom = "Double assassinat";
                        break;
                    }
                }
                x.joueur.sendMessage(Text.literal("§7✦ " + nom), true);
            }
        }
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
        // La foule alentour s'enfuit ; le déguisement ne tient pas à un meurtre
        for (fr.fenua.ombre.entite.Passant pa : sw.getEntitiesByClass(fr.fenua.ombre.entite.Passant.class,
                v.getBoundingBox().expand(12), e -> e.isAlive())) pa.fuir(v.getPos());
        joueur.removeStatusEffect(fr.fenua.ombre.objet.Effets.DEGUISE);
    }
}
