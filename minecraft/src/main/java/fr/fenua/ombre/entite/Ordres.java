package fr.fenua.ombre.entite;

import fr.fenua.ombre.ac.Detection;
import fr.fenua.ombre.objet.Effets;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Les ordres d'un garde, comme dans Assassin's Creed : il n'erre pas au hasard.
 *  - En poste : il tient sa place (une porte, un carrefour, un toit), tourné vers ce qu'il garde, et regarde
 *    de temps en temps à gauche et à droite.
 *  - En ronde : il suit un circuit fixe, d'un point à l'autre, en marchant, et s'arrête un moment à chaque point.
 * Dès qu'il te remarque (le moindre soupçon), il oublie ses ordres : la détection et le combat le mènent.
 * Le calme revenu, il retourne à son poste ou reprend sa ronde.
 *
 * Les ordres sont gardés dans les étiquettes du garde (elles sont sauvegardées avec lui) :
 * « ombre_poste:x,y,z,lacet » et « ombre_ronde:x,y,z;x,y,z;… ».
 */
public final class Ordres {
    private Ordres() {}

    public static final String POSTE = "ombre_poste:", RONDE = "ombre_ronde:";

    /** Un ordre lu : un poste (un seul point et son lacet) ou une ronde (plusieurs points). */
    public record Ordre(List<Vec3d> points, float lacet, boolean poste) {}

    private static final Map<MobEntity, Ordre> LUS = new WeakHashMap<>();

    public static void poste(MobEntity m, Vec3d p, float lacet) {
        m.addCommandTag(POSTE + f(p.x) + "," + f(p.y) + "," + f(p.z) + "," + f(lacet));
        LUS.remove(m);
    }

    public static void ronde(MobEntity m, List<Vec3d> points) {
        StringBuilder sb = new StringBuilder(RONDE);
        for (int i = 0; i < points.size(); i++) {
            Vec3d p = points.get(i);
            if (i > 0) sb.append(';');
            sb.append(f(p.x)).append(',').append(f(p.y)).append(',').append(f(p.z));
        }
        m.addCommandTag(sb.toString());
        LUS.remove(m);
    }

    private static String f(double v) { return String.valueOf(Math.round(v * 10) / 10.0); }

    /** L'ordre de ce garde, ou null s'il n'en a pas (il se comporte alors comme avant). */
    public static Ordre lire(MobEntity m) {
        if (LUS.containsKey(m)) return LUS.get(m);
        Ordre o = null;
        try {
            for (String t : m.getCommandTags()) {
                if (t.startsWith(POSTE)) {
                    String[] q = t.substring(POSTE.length()).split(",");
                    o = new Ordre(List.of(new Vec3d(Double.parseDouble(q[0]), Double.parseDouble(q[1]), Double.parseDouble(q[2]))),
                            Float.parseFloat(q[3]), true);
                } else if (t.startsWith(RONDE)) {
                    List<Vec3d> pts = new ArrayList<>();
                    for (String p : t.substring(RONDE.length()).split(";")) {
                        String[] q = p.split(",");
                        pts.add(new Vec3d(Double.parseDouble(q[0]), Double.parseDouble(q[1]), Double.parseDouble(q[2])));
                    }
                    if (!pts.isEmpty()) o = new Ordre(pts, 0, false);
                }
            }
        } catch (RuntimeException e) { o = null; }
        LUS.put(m, o);
        return o;
    }

    /** Le but « tenir son poste / faire sa ronde », placé avant l'errance de Minecraft. */
    public static class But extends Goal {
        private final MobEntity m;
        private Ordre o;
        private int idx, attente, regard, coince;
        private float ecart;
        private double meilleure = Double.MAX_VALUE;

        public But(MobEntity m) {
            this.m = m;
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        /** Rien ne le trouble : pas de cible, pas le moindre soupçon, pas endormi. */
        private boolean calme() {
            return m.getTarget() == null && Detection.niveau(m) < 0.01f && !m.hasStatusEffect(Effets.ENDORMI)
                    && !m.hasStatusEffect(Effets.FOLIE) && !m.hasNoGravity();
        }

        @Override public boolean canStart() { o = lire(m); return o != null && calme(); }
        @Override public boolean shouldContinue() { return o != null && calme(); }

        @Override
        public void start() {
            if (o.poste()) return;
            // On reprend la ronde au point le plus proche
            double mieux = Double.MAX_VALUE;
            for (int i = 0; i < o.points().size(); i++) {
                double d = o.points().get(i).squaredDistanceTo(m.getPos());
                if (d < mieux) { mieux = d; idx = i; }
            }
        }

        @Override public void stop() { m.getNavigation().stop(); }

        @Override
        public void tick() {
            Vec3d but = o.points().get(idx);
            double dx = but.x - m.getX(), dz = but.z - m.getZ();
            boolean arrive = dx * dx + dz * dz < (o.poste() ? 1.0 : 2.0) && Math.abs(but.y - m.getY()) < 2;
            if (!arrive) {
                attente = 0;
                // Il n'avance plus vers son but depuis 5 s (un poste hors d'atteinte, une rue barrée) :
                // il tient son poste là où il est, ou passe au point suivant de sa ronde
                double d = dx * dx + dz * dz + (but.y - m.getY()) * (but.y - m.getY());
                if (d < meilleure - 0.5) { meilleure = d; coince = 0; }
                else if (++coince > 100) {
                    coince = 0; meilleure = Double.MAX_VALUE;
                    if (o.poste()) {
                        m.getCommandTags().removeIf(t -> t.startsWith(POSTE));
                        poste(m, m.getPos(), o.lacet());
                        o = lire(m);
                    } else idx = (idx + 1) % o.points().size();
                    return;
                }
                if (m.getNavigation().isIdle() || m.age % 40 == 0) m.getNavigation().startMovingTo(but.x, but.y, but.z, o.poste() ? 0.7 : 0.55);
                return;
            }
            coince = 0; meilleure = Double.MAX_VALUE;
            if (o.poste()) {
                m.getNavigation().stop();
                // Il regarde droit devant lui, puis un moment à gauche, à droite…
                if (--regard <= 0) {
                    regard = 60 + m.getRandom().nextInt(80);
                    int k = m.getRandom().nextInt(4);
                    ecart = k == 1 ? -55 : k == 3 ? 55 : 0;
                }
                regarderVers(o.lacet() + ecart);
                return;
            }
            // En ronde : une pause au point, en regardant la rue, puis le point suivant
            if (attente == 0) attente = 30 + m.getRandom().nextInt(50);
            m.getNavigation().stop();
            Vec3d suivant = o.points().get((idx + 1) % o.points().size());
            float vers = (float) Math.toDegrees(Math.atan2(-(suivant.x - m.getX()), suivant.z - m.getZ()));
            regarderVers(vers + (attente > 25 ? 40 : 0));
            if (--attente <= 0) idx = (idx + 1) % o.points().size();
        }

        private void regarderVers(float lacet) {
            double r = Math.toRadians(lacet);
            m.getLookControl().lookAt(m.getX() - Math.sin(r) * 5, m.getEyeY(), m.getZ() + Math.cos(r) * 5, 10, 30);
            // Le corps suit doucement la tête
            m.setBodyYaw(m.bodyYaw + MathHelper.wrapDegrees(lacet - m.bodyYaw) * 0.1f);
        }
    }
}
