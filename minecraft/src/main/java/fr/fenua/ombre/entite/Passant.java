package fr.fenua.ombre.entite;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Un passant du Paris de 1889 : il flâne, regarde autour de lui, s'arrête devant les gens.
 * Marcher parmi deux passants ou plus, sans courir, te fond dans la foule (voir Discretion).
 * Il s'enfuit en criant quand un assassinat a lieu près de lui, ou au bruit d'un pétard,
 * et accourt vers des écus jetés au sol.
 */
public class Passant extends PathAwareEntity {
    public static final int VARIANTES = 8;
    public static final String[] NOMS = {"Bourgeois", "Ouvrier", "Dame", "Élégante", "Marchande", "Gamin", "Artiste", "Étudiant"};
    private static final TrackedData<Integer> VARIANTE = DataTracker.registerData(Passant.class, TrackedDataHandlerRegistry.INTEGER);

    public Passant(EntityType<? extends PathAwareEntity> type, World w) { super(type, w); }

    public static DefaultAttributeContainer.Builder attributs() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.45));
        goalSelector.add(4, new WanderAroundFarGoal(this, 0.55));
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6));
        goalSelector.add(6, new LookAtEntityGoal(this, Passant.class, 5));
        goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder b) {
        super.initDataTracker(b);
        b.add(VARIANTE, 0);
    }

    public int variante() { return dataTracker.get(VARIANTE); }
    public void setVariante(int v) { dataTracker.set(VARIANTE, Math.floorMod(v, VARIANTES)); }

    @Nullable @Override
    public EntityData initialize(ServerWorldAccess w, LocalDifficulty d, SpawnReason r, @Nullable EntityData data) {
        setVariante(w.getRandom().nextInt(VARIANTES));
        return super.initialize(w, d, r, data);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variante", variante());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setVariante(nbt.getInt("Variante"));
    }

    @Override public boolean canImmediatelyDespawn(double distance) { return false; }

    /** S'enfuir loin d'un danger, en criant. */
    public void fuir(Vec3d danger) {
        Vec3d loin = NoPenaltyTargeting.findFrom(this, 16, 6, danger);
        if (loin != null) getNavigation().startMovingTo(loin.x, loin.y, loin.z, 1.5);
        if (random.nextInt(3) == 0) playSound(SoundEvents.ENTITY_VILLAGER_NO, 1f, 1.2f + random.nextFloat() * 0.3f);
    }
}
