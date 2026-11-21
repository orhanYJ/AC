package fr.fenua.ombre.objet;

import fr.fenua.ombre.Ombre;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/** Les effets du mod : endormi (fléchette soporifique), agonie (fléchette empoisonnée), déguisé. */
public final class Effets {
    private Effets() {}

    /** Endormi : le garde ne bouge plus, ne voit plus rien, et ronfle. */
    public static class Endormi extends StatusEffect {
        public Endormi() { super(StatusEffectCategory.HARMFUL, 0x6C7FB8); }
        @Override public boolean canApplyUpdateEffect(int duree, int force) { return true; }
        @Override
        public boolean applyUpdateEffect(LivingEntity e, int force) {
            if (e instanceof MobEntity m) {
                m.setTarget(null);
                m.getNavigation().stop();
                m.setVelocity(0, Math.min(0, m.getVelocity().y), 0);
                m.setPitch(40);
            }
            if (e.age % 30 == 0 && e.getWorld() instanceof ServerWorld sw)
                sw.spawnParticles(ParticleTypes.NOTE, e.getX(), e.getEyeY() + 0.5, e.getZ(), 1, 0.2, 0.1, 0.2, 0);
            return true;
        }
    }

    /** Agonie : quelques secondes plus tard, la victime s'effondre, sans un bruit. */
    public static class Agonie extends StatusEffect {
        public Agonie() { super(StatusEffectCategory.HARMFUL, 0x3C7A2B); }
        @Override public boolean canApplyUpdateEffect(int duree, int force) { return duree == 1; }
        @Override
        public boolean applyUpdateEffect(LivingEntity e, int force) {
            e.damage(e.getDamageSources().magic(), 1000f);
            return true;
        }
    }

    /**
     * Folie (fléchette de folie) : le garde touché s'en prend à ses camarades. Chaque seconde, il cherche
     * le garde le plus proche (16 blocs) et l'attaque ; celui-ci se défend. Quand l'effet passe, il se calme.
     */
    public static class Folie extends StatusEffect {
        public Folie() { super(StatusEffectCategory.HARMFUL, 0xB0204A); }
        @Override public boolean canApplyUpdateEffect(int duree, int force) { return duree % 20 == 0 || duree == 1; }
        @Override
        public boolean applyUpdateEffect(LivingEntity e, int force) {
            if (!(e instanceof MobEntity m) || !(e.getWorld() instanceof ServerWorld sw)) return true;
            var effet = m.getStatusEffect(FOLIE);
            if (effet != null && effet.getDuration() <= 1) { m.setTarget(null); return true; }
            MobEntity proie = null;
            double best = 16 * 16;
            for (MobEntity autre : sw.getEntitiesByClass(MobEntity.class, m.getBoundingBox().expand(16),
                    o -> o != m && o.isAlive() && fr.fenua.ombre.Discretion.estGarde(o))) {
                double d = autre.squaredDistanceTo(m);
                if (d < best) { best = d; proie = autre; }
            }
            if (proie != null) {
                m.setTarget(proie);
                if (proie.getTarget() == null) proie.setTarget(m);              // il se défend
            }
            sw.spawnParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getEyeY() + 0.4, m.getZ(), 2, 0.3, 0.2, 0.3, 0);
            return true;
        }
    }

    /** Déguisé : en tenue de hallebardier, les gardes ne te remarquent qu'à 2 blocs (voir Discretion). */
    public static class Deguise extends StatusEffect {
        public Deguise() { super(StatusEffectCategory.BENEFICIAL, 0x2B3A67); }
    }

    public static RegistryEntry<StatusEffect> ENDORMI, AGONIE, DEGUISE, FOLIE;

    public static void enregistrer() {
        ENDORMI = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "endormi"), new Endormi());
        AGONIE = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "agonie"), new Agonie());
        FOLIE = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "folie"), new Folie());
        DEGUISE = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "deguise"), new Deguise());
    }
}
