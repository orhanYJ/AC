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

    /** Déguisé : en tenue de gardien, les gardes ne te remarquent qu'à 2 blocs (voir Discretion). */
    public static class Deguise extends StatusEffect {
        public Deguise() { super(StatusEffectCategory.BENEFICIAL, 0x2B3A67); }
    }

    public static RegistryEntry<StatusEffect> ENDORMI, AGONIE, DEGUISE;

    public static void enregistrer() {
        ENDORMI = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "endormi"), new Endormi());
        AGONIE = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "agonie"), new Agonie());
        DEGUISE = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Ombre.MOD_ID, "deguise"), new Deguise());
    }
}
