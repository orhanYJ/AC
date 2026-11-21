package fr.fenua.ombre.initiation;

import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.entite.Entites;
import fr.fenua.ombre.entite.Passant;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Ce qu'une étape a sous la main : le joueur, le monde, et des outils en coordonnées « de la cour »
 * (x = 0 à l'entrée de la cour, vers l'est ; z = 0 au mur nord ; y = 0 au sol).
 */
public final class Ctx {
    public final ServerPlayerEntity p;
    public final ServerWorld w;
    final BlockPos o;        // le coin de la cour
    final int cour;

    Ctx(ServerPlayerEntity p, ServerWorld w, BlockPos origine, int cour) {
        this.p = p; this.w = w; this.cour = cour;
        this.o = origine.add(Initiation.S[cour], 0, 0);
    }

    public double x() { return p.getX() - o.getX(); }
    public double y() { return p.getY() - o.getY(); }
    public double z() { return p.getZ() - o.getZ(); }
    public BlockPos pos(int x, int y, int z) { return o.add(x, y, z); }

    /** Le joueur est-il entre x0 et x1 (blocs compris), et au moins à la hauteur y ? */
    public boolean sur(int x0, int x1, double y) { return x() >= x0 && x() < x1 + 1 && y() >= y - 0.1; }

    /** Tout ce qui porte cette étiquette, vivant, dans la cour. */
    public List<MobEntity> vivants(String tag) {
        Box b = new Box(o.getX() - 2, o.getY() - 6, o.getZ() - 3, o.getX() + Initiation.LEN[cour] + 2, o.getY() + 30, o.getZ() + 18);
        return w.getEntitiesByClass(MobEntity.class, b, m -> m.isAlive() && m.getCommandTags().contains(tag));
    }

    public boolean morts(String tag) { return vivants(tag).isEmpty(); }

    /** Un garde de la cour l'a-t-il repéré ? */
    public boolean repere() {
        for (MobEntity m : vivants(Initiation.TAG)) if (m.getTarget() == p) return true;
        return false;
    }

    /**
     * Une recrue de la garde, sans arme (c'est l'entraînement) : avec ou sans vie propre,
     * aveugle ou non (un mannequin aveugle ne peut pas donner l'alerte).
     */
    public MobEntity garde(String tag, int x, int y, int z, float yaw, boolean bouge, boolean aveugle, int rayon) {
        MobEntity g = Entites.HALLEBARDIER.create(w);
        if (g == null) return null;
        BlockPos pos = pos(x, y, z);
        g.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        g.initialize(w, w.getLocalDifficulty(pos), SpawnReason.COMMAND, null);
        g.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        g.setHeadYaw(yaw);
        g.setBodyYaw(yaw);
        g.setPersistent();
        g.setCustomName(Text.literal(aveugle ? "Mannequin d'entraînement" : "Recrue de la garde"));
        g.setPositionTarget(pos, rayon);
        g.addCommandTag(Gardes.GARDE);
        g.addCommandTag(Initiation.TAG);
        g.addCommandTag(tag);
        if (aveugle) g.addCommandTag(Initiation.AVEUGLE);
        if (!bouge) g.setAiDisabled(true);
        w.spawnEntity(g);
        return g;
    }

    /** Un passant ; immobile (« pose ») ou flânant autour de sa place. */
    public void passant(int x, int z, float yaw, boolean bouge, int rayon) {
        Passant pa = Entites.PASSANT.create(w);
        if (pa == null) return;
        BlockPos pos = pos(x, 0, z);
        pa.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        pa.initialize(w, w.getLocalDifficulty(pos), SpawnReason.COMMAND, null);
        pa.setHeadYaw(yaw);
        pa.setBodyYaw(yaw);
        pa.setPersistent();
        pa.setPositionTarget(pos, rayon);
        pa.addCommandTag(Initiation.TAG);
        if (!bouge) pa.setAiDisabled(true);
        w.spawnEntity(pa);
    }

    /** Donner un objet s'il n'en a pas déjà. */
    public void assurer(Item item, int n) {
        if (!p.getInventory().containsAny(s -> s.isOf(item))) p.getInventory().offerOrDrop(new ItemStack(item, n));
    }

    public void dire(String s) { p.sendMessage(Text.literal("§6Maître Vojtěch §7: §f" + s), false); }
}
