package fr.fenua.ombre.ac;

import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.objet.Effets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * La détection progressive, comme dans Assassin's Creed : un garde ne te repère pas d'un coup.
 * Quand il te voit, son « soupçon » monte (plus vite si tu es près, en pleine lumière, en train de courir,
 * dans une zone interdite, ou recherché) ; à mi-chemin il s'arrête, te regarde et vient voir (jaune) ;
 * plein, il donne l'alerte (rouge). Quand il ne te voit plus, le soupçon redescend.
 * Le joueur voit tout cela à l'écran (flèches autour du viseur, « ? » et « ! » au-dessus des têtes).
 */
public final class Detection {
    private Detection() {}

    private static final Map<MobEntity, Float> SOUPCON = new WeakHashMap<>();
    private static boolean forcer;

    /** Laisse passer un setTarget voulu par le mod (alerte, propagation de l'alarme). */
    public static void forcer(Runnable r) {
        forcer = true;
        try { r.run(); } finally { forcer = false; }
    }

    /** Appelé par mixin/MobEntityMixin : faut-il empêcher ce garde de prendre ce joueur pour cible ? */
    public static boolean bloquer(MobEntity m, LivingEntity cible) {
        if (forcer || !(cible instanceof PlayerEntity p) || !Discretion.estGarde(m)) return false;
        if (m.getAttacker() == p) return false;                     // on l'a frappé : il riposte
        return niveau(m) < 1f;
    }

    public static float niveau(MobEntity m) { return SOUPCON.getOrDefault(m, 0f); }
    public static void fixer(MobEntity m, float s) { SOUPCON.put(m, s); }

    /**
     * Un corps découvert : un garde qui voit (à moins de 12 blocs) le corps d'un camarade qu'on n'avait pas encore
     * trouvé devient méfiant et va voir ; les gardes proches aussi. (Les corps restent une minute à terre.)
     */
    private static void corps(ServerWorld w, MobEntity g) {
        if (g.age % 10 != 0 || g.getTarget() != null) return;
        for (LivingEntity c : w.getEntitiesByClass(LivingEntity.class, g.getBoundingBox().expand(12),
                e -> !e.isAlive() && Discretion.estGarde(e) && !e.getCommandTags().contains("ombre_decouvert"))) {
            if (!g.canSee(c)) continue;
            c.addCommandTag("ombre_decouvert");
            for (MobEntity autre : w.getEntitiesByClass(MobEntity.class, c.getBoundingBox().expand(16), e -> e.isAlive() && Discretion.estGarde(e))) {
                fixer(autre, Math.max(niveau(autre), 0.6f));
                autre.getNavigation().startMovingTo(c.getX(), c.getY(), c.getZ(), 0.9);
            }
            w.playSound(null, g.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PILLAGER_CELEBRATE, net.minecraft.sound.SoundCategory.HOSTILE, 1f, 0.6f);
            return;
        }
    }

    /** Tous les 2 ticks, pour chaque garde du monde. */
    public static void tick(ServerWorld w, List<MobEntity> gardes) {
        for (MobEntity g : gardes) {
            corps(w, g);
            float s = niveau(g);
            if (g.getTarget() instanceof PlayerEntity) { SOUPCON.put(g, 1f); continue; }
            PlayerEntity p = w.getClosestPlayer(g, 48);
            if (p != null && (p.isCreative() || p.isSpectator())) p = null;
            double portee = p == null ? 0 : portee(g, p);
            if (p != null && portee > 0 && g.squaredDistanceTo(p) <= portee * portee && g.canSee(p)) {
                double d = Math.sqrt(g.squaredDistanceTo(p));
                // De tout près : plein en 1,6 s ; au bout de sa vue : 8 s. Deux fois plus vite en zone interdite.
                float v = (float) (0.012 + 0.05 * Math.max(0, 1 - d / portee));
                if (Zones.etat(p) == Zones.DEDANS) v *= 2;
                if (Notoriete.get(p) >= Notoriete.RECHERCHE) v *= 1.5f;
                s = Math.min(1f, s + v);
                final PlayerEntity cible = p;
                if (s >= 1f) {
                    forcer(() -> g.setTarget(cible));
                } else {
                    g.getLookControl().lookAt(p);
                    if (s >= 0.45f && g.age % 10 == 0) {                       // il vient voir
                        g.getNavigation().startMovingTo(p.getX(), p.getY(), p.getZ(), 0.75);
                    } else if (s < 0.45f) {
                        g.getNavigation().stop();                            // il s'arrête et regarde
                    }
                }
            } else {
                s = Math.max(0f, s - 0.012f);
            }
            SOUPCON.put(g, s);
        }
    }

    /** Étiquette d'un garde toujours vigilant, même hors des zones interdites (l'arène). */
    public static final String VIGILANT = "ombre_vigilant";

    /** Jusqu'où voit un garde, en blocs, avant la discrétion (accroupi, nuit, dans le dos…) et la notoriété. */
    public static final double VUE_GARDE = 13, VUE_CAPITAINE = 15, VUE_ARBALETRIER = 18;
    /** Au-delà de cette hauteur au-dessus d'un garde à pied, on est hors de sa vue. */
    public static final double TOIT = 3.5;

    /** Jusqu'où ce garde peut te remarquer maintenant (0 s'il ne peut pas). */
    private static double portee(MobEntity g, PlayerEntity p) {
        if (g.hasStatusEffect(Effets.ENDORMI) || g.getCommandTags().contains("ombre_aveugle")) return 0;   // endormi, ou mannequin
        // Comme dans Assassin's Creed : hors d'une zone interdite, les gardes ne s'occupent pas de toi — sauf si tu es
        // recherché, ou s'ils sont déjà sur leurs gardes (un vol raté, un corps découvert, un camarade qui crie).
        // Les recrues de l'initiation et les gardes de l'arène (un fortin ennemi) restent toujours vigilants.
        boolean vigilant = g.getCommandTags().contains("ombre_init") || g.getCommandTags().contains(VIGILANT) || niveau(g) >= 0.5f;
        if (!vigilant && Zones.etat(p) != Zones.DEDANS && Notoriete.get(p) < Notoriete.RECHERCHE) return 0;
        // La portée de vue, comme dans Assassin's Creed : une quinzaine de blocs devant soi (les arbalétriers des
        // toits, qui guettent, un peu plus). Avant, on prenait la portée de suivi de Minecraft : 32 pour l'arbalétrier.
        double base = g instanceof fr.fenua.ombre.entite.Gardes.Arbaletrier ? VUE_ARBALETRIER
                : g instanceof fr.fenua.ombre.entite.Gardes.Capitaine ? VUE_CAPITAINE : VUE_GARDE;
        // Un garde au sol ne lève pas les yeux : sur les toits (plus de 3,5 blocs au-dessus de lui), il ne te voit pas.
        // Seuls les arbalétriers, postés en hauteur, surveillent les toits.
        if (!(g instanceof fr.fenua.ombre.entite.Gardes.Arbaletrier) && p.getY() - g.getY() > TOIT) return 0;
        double f = p.getAttackDistanceScalingFactor(g);
        return f <= 0.001 ? 1.6 : Math.max(2.0, base * f);
    }
}
