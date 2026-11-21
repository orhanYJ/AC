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
 * Un passant de Prague, en 1600 : il flâne, regarde autour de lui, s'arrête devant les gens.
 * Marcher parmi deux passants ou plus, sans courir, te fond dans la foule (voir Discretion).
 * Il s'enfuit en criant quand un assassinat a lieu près de lui, ou au bruit d'un pétard,
 * et accourt vers des thalers jetés au sol.
 */
public class Passant extends PathAwareEntity {
    public static final int VARIANTES = 8;
    public static final String[] NOMS = {"Bourgeois", "Artisan", "Paysanne", "Dame de qualité", "Marchande", "Gamin", "Moine", "Étudiant"};
    private static final TrackedData<Integer> VARIANTE = DataTracker.registerData(Passant.class, TrackedDataHandlerRegistry.INTEGER);

    public Passant(EntityType<? extends PathAwareEntity> type, World w) { super(type, w); }

    public static DefaultAttributeContainer.Builder attributs() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.45));
        goalSelector.add(3, new Ordres.But(this));                 // un messager suit sa route
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

    /**
     * Le vol à la tire : accroupi, dans son dos, un clic droit lui prend sa bourse.
     * Vu de face, il crie au voleur et les gardes proches deviennent soupçonneux.
     */
    @Override
    protected net.minecraft.util.ActionResult interactMob(PlayerEntity joueur, net.minecraft.util.Hand main) {
        if (main != net.minecraft.util.Hand.MAIN_HAND) return super.interactMob(joueur, main);
        // Un marchand : un clic droit (debout) ouvre sa boutique
        if (!joueur.isSneaking()) {
            for (String t : getCommandTags()) if (t.startsWith(fr.fenua.ombre.ac.Profil.BOUTIQUE)) {
                if (!getWorld().isClient() && joueur instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                    getLookControl().lookAt(joueur);
                    playSound(SoundEvents.ENTITY_VILLAGER_AMBIENT, 0.8f, 1.1f);
                    fr.fenua.ombre.ac.Profil.envoyer(sp, Integer.parseInt(t.substring(fr.fenua.ombre.ac.Profil.BOUTIQUE.length())));
                }
                return net.minecraft.util.ActionResult.SUCCESS;
            }
            // Un habitant de l'histoire (messager, donneur de quête) : on lui parle
            if (getCommandTags().stream().anyMatch(t -> t.startsWith("ombre_pnj."))) {
                if (!getWorld().isClient() && joueur instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                    getLookControl().lookAt(joueur);
                    fr.fenua.ombre.ac.Quetes.evenement(sp, "parle", this);
                }
                return net.minecraft.util.ActionResult.SUCCESS;
            }
            return super.interactMob(joueur, main);
        }
        if (getWorld().isClient()) return net.minecraft.util.ActionResult.SUCCESS;
        if (getCommandTags().contains("ombre_vole")) {
            joueur.sendMessage(net.minecraft.text.Text.literal("§7Sa bourse est déjà vide."), true);
            return net.minecraft.util.ActionResult.SUCCESS;
        }
        boolean adroit = fr.fenua.ombre.ac.Profil.a(joueur, "voleur");
        if (adroit || fr.fenua.ombre.Discretion.dansLeDos(this, joueur)) {
            addCommandTag("ombre_vole");
            int n = (4 + random.nextInt(9)) * (adroit ? 2 : 1);
            if (joueur instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                fr.fenua.ombre.ac.Profil.ajouterThalers(sp, n, "bourse volée");
                fr.fenua.ombre.ac.Quetes.evenement(sp, "vol", this);
            }
            joueur.addCommandTag("ombre_vol");      // pour l'initiation
            playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.4f, 1.6f);
        } else {
            // Pris la main dans le sac !
            getLookControl().lookAt(joueur);
            playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.2f, 1.1f);
            joueur.sendMessage(net.minecraft.text.Text.literal("§c« Au voleur ! » — les gardes se retournent."), true);
            for (MobEntity m : getWorld().getEntitiesByClass(MobEntity.class, getBoundingBox().expand(16), fr.fenua.ombre.Discretion::estGarde)) {
                fr.fenua.ombre.ac.Detection.fixer(m, Math.max(fr.fenua.ombre.ac.Detection.niveau(m), 0.7f));
                m.getNavigation().startMovingTo(joueur.getX(), joueur.getY(), joueur.getZ(), 0.9);
            }
            fuir(joueur.getPos());
        }
        return net.minecraft.util.ActionResult.SUCCESS;
    }

    /** Jusqu'à quand il a peur (il ne retourne pas à sa conversation avant). */
    public long effrayeJusqua;

    /** S'enfuir loin d'un danger, en criant. */
    public void fuir(Vec3d danger) {
        effrayeJusqua = getWorld().getTime() + 300;
        Vec3d loin = NoPenaltyTargeting.findFrom(this, 16, 6, danger);
        if (loin != null) getNavigation().startMovingTo(loin.x, loin.y, loin.z, 1.5);
        if (random.nextInt(3) == 0) playSound(SoundEvents.ENTITY_VILLAGER_NO, 1f, 1.2f + random.nextFloat() * 0.3f);
    }
}
