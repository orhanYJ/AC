package fr.fenua.ombre.objet;

import fr.fenua.ombre.Gardes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Ce qu'on lance : thalers, pétard, fléchettes. Le même projectile, son effet dépend de l'objet.
 *  - Thalers : en tombant, ils tintent ; la foule accourt pour les ramasser, les gardes viennent voir.
 *  - Pétard : une détonation et de la fumée ; la foule s'enfuit, les gardes accourent voir.
 *  - Fléchette soporifique : le garde touché s'endort 30 s.
 *  - Fléchette empoisonnée : le garde touché s'effondre 4 s plus tard, sans un bruit.
 *  - Fléchette de folie : le garde touché s'en prend à ses camarades pendant 15 s.
 */
public class Projectile extends ThrownItemEntity {
    public Projectile(EntityType<? extends Projectile> type, World w) { super(type, w); }
    public Projectile(EntityType<? extends Projectile> type, LivingEntity lanceur, World w) { super(type, lanceur, w); }

    @Override protected Item getDefaultItem() { return Objets.THALERS; }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (!(hit.getEntity() instanceof LivingEntity v)) return;
        ItemStack s = getStack();
        if (s.isOf(Objets.FLECHETTE_SOPORIFIQUE)) v.addStatusEffect(new StatusEffectInstance(Effets.ENDORMI, 600, 0));
        else if (s.isOf(Objets.FLECHETTE_EMPOISONNEE)) v.addStatusEffect(new StatusEffectInstance(Effets.AGONIE, 80, 0));
        else if (s.isOf(Objets.FLECHETTE_FOLIE)) v.addStatusEffect(new StatusEffectInstance(Effets.FOLIE, 300, 0));
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        ItemStack s = getStack();
        Vec3d p = hit.getPos();
        if (s.isOf(Objets.THALERS)) {
            sw.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, s), p.x, p.y + 0.2, p.z, 14, 0.4, 0.2, 0.4, 0.15);
            sw.playSound(null, p.x, p.y, p.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7f, 1.6f);
            Distractions.attirer(sw, p, 12, 9);
        } else if (s.isOf(Objets.PETARD)) {
            sw.spawnParticles(ParticleTypes.EXPLOSION, p.x, p.y + 0.3, p.z, 1, 0, 0, 0, 0);
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.3, p.z, 30, 0.6, 0.4, 0.6, 0.03);
            sw.playSound(null, p.x, p.y, p.z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.5f, 0.8f);
            Distractions.effrayer(sw, p, 16, 18);
        }
        discard();
    }

    /** Les réactions des gardes et de la foule (voir aussi Gardes et Passant). */
    public static final class Distractions {
        private Distractions() {}

        /** Des pièces au sol : la foule s'approche, les gardes pas encore alertés viennent voir. */
        public static void attirer(ServerWorld w, Vec3d p, double foule, double gardes) {
            for (MobEntity m : w.getEntitiesByClass(MobEntity.class, new net.minecraft.util.math.Box(p, p).expand(Math.max(foule, gardes)),
                    e -> e.isAlive() && e.getTarget() == null)) {
                double d = m.getPos().distanceTo(p);
                boolean garde = m.getCommandTags().contains(Gardes.GARDE);
                if (garde ? d <= gardes : (m instanceof fr.fenua.ombre.entite.Passant && d <= foule))
                    m.getNavigation().startMovingTo(p.x + (w.random.nextDouble() - 0.5) * 2, p.y, p.z + (w.random.nextDouble() - 0.5) * 2, 0.9);
            }
        }

        /** Une détonation : la foule s'enfuit, les gardes accourent au bruit. */
        public static void effrayer(ServerWorld w, Vec3d p, double foule, double gardes) {
            for (MobEntity m : w.getEntitiesByClass(MobEntity.class, new net.minecraft.util.math.Box(p, p).expand(Math.max(foule, gardes)),
                    e -> e.isAlive())) {
                double d = m.getPos().distanceTo(p);
                if (m instanceof fr.fenua.ombre.entite.Passant pa && d <= foule) pa.fuir(p);
                else if (m.getCommandTags().contains(Gardes.GARDE) && m.getTarget() == null && d <= gardes)
                    m.getNavigation().startMovingTo(p.x, p.y, p.z, 1.15);
            }
        }
    }
}
