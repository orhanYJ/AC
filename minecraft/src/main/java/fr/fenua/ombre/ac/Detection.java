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

    /** Tous les 2 ticks, pour chaque garde du monde. */
    public static void tick(ServerWorld w, List<MobEntity> gardes) {
        for (MobEntity g : gardes) {
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

    /** Jusqu'où voit un garde, en blocs, avant la discrétion (accroupi, nuit, dans le dos…) et la notoriété. */
    public static final double VUE_GARDE = 13, VUE_CAPITAINE = 15, VUE_ARBALETRIER = 18;

    /** Jusqu'où ce garde peut te remarquer maintenant (0 s'il ne peut pas). */
    private static double portee(MobEntity g, PlayerEntity p) {
        if (g.hasStatusEffect(Effets.ENDORMI) || g.getCommandTags().contains("ombre_aveugle")) return 0;   // endormi, ou mannequin
        // La portée de vue, comme dans Assassin's Creed : une quinzaine de blocs devant soi (les arbalétriers des
        // toits, qui guettent, un peu plus). Avant, on prenait la portée de suivi de Minecraft : 32 pour l'arbalétrier.
        double base = g instanceof fr.fenua.ombre.entite.Gardes.Arbaletrier ? VUE_ARBALETRIER
                : g instanceof fr.fenua.ombre.entite.Gardes.Capitaine ? VUE_CAPITAINE : VUE_GARDE;
        // Un garde au sol lève rarement les yeux : sur les toits, il te remarque moins
        if (!(g instanceof fr.fenua.ombre.entite.Gardes.Arbaletrier) && p.getY() - g.getY() > 4) base *= 0.6;
        double f = p.getAttackDistanceScalingFactor(g);
        return f <= 0.001 ? 1.6 : Math.max(2.0, base * f);
    }
}
