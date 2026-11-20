package fr.fenua.ombre.bloc;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Poteau de tyrolienne. Avec une corde en main : clic droit sur un poteau, puis sur un autre (64 blocs max).
 * Les mains vides : clic droit pour glisser jusqu'à l'autre poteau (on ne remonte pas une tyrolienne).
 */
public class PoteauTyrolienne extends BlockWithEntity {
    public static final MapCodec<PoteauTyrolienne> CODEC = createCodec(PoteauTyrolienne::new);
    private static final VoxelShape FORME = createCuboidShape(5, 0, 5, 11, 16, 11);
    public static final int PORTEE = 64;

    public PoteauTyrolienne(Settings s) { super(s); }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState s) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORME; }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new Entite(pos, state); }

    /** Avec la corde en main : relier deux poteaux. */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack pile, BlockState s, World w, BlockPos pos, PlayerEntity joueur,
                                             Hand main, BlockHitResult hit) {
        if (!pile.isOf(OmbreBlocs.CORDE)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (w.isClient()) return ItemActionResult.SUCCESS;
        NbtComponent donnees = pile.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = donnees == null ? new NbtCompound() : donnees.copyNbt();
        if (!nbt.contains("poteau")) {
            nbt.putLong("poteau", pos.asLong());
            pile.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            joueur.sendMessage(Text.literal("§ePremier poteau choisi§r : clic droit sur le second."), true);
            return ItemActionResult.SUCCESS;
        }
        BlockPos premier = BlockPos.fromLong(nbt.getLong("poteau"));
        pile.remove(DataComponentTypes.CUSTOM_DATA);
        if (premier.equals(pos)) {
            joueur.sendMessage(Text.literal("§7Corde annulée."), true);
        } else if (!premier.isWithinDistance(pos, PORTEE)) {
            joueur.sendMessage(Text.literal("§cTrop loin : " + PORTEE + " blocs au plus."), true);
        } else if (relier(w, premier, pos)) {
            pile.decrementUnlessCreative(1, joueur);
            w.playSound(null, pos, SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.BLOCKS, 1f, 1f);
            joueur.sendMessage(Text.literal("§aTyrolienne tendue§r : clic droit sur le poteau le plus haut pour glisser."), true);
        } else {
            joueur.sendMessage(Text.literal("§cLe premier poteau a disparu."), true);
        }
        return ItemActionResult.SUCCESS;
    }

    /** Les mains vides : partir. */
    @Override
    protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity joueur, BlockHitResult hit) {
        if (!(w.getBlockEntity(pos) instanceof Entite e) || e.cible == null) {
            if (!w.isClient()) joueur.sendMessage(Text.literal("§7Ce poteau n'est relié à rien : relie-le avec une corde."), true);
            return ActionResult.SUCCESS;
        }
        if (e.cible.getY() > pos.getY() + 1) {
            if (w.isClient()) joueur.sendMessage(Text.literal("§7On ne remonte pas une tyrolienne : pars de l'autre poteau."), true);
            return ActionResult.SUCCESS;
        }
        if (w.isClient() && CoteClient.ICI[0] != null) CoteClient.ICI[0].tyrolienne(pos, e.cible);
        return ActionResult.SUCCESS;
    }

    /** Casser un poteau détache la corde à l'autre bout. */
    @Override
    protected void onStateReplaced(BlockState s, World w, BlockPos pos, BlockState nouveau, boolean bouge) {
        if (!s.isOf(nouveau.getBlock()) && w.getBlockEntity(pos) instanceof Entite e && e.cible != null
                && w.getBlockEntity(e.cible) instanceof Entite autre && pos.equals(autre.cible)) {
            autre.relier(null, false);
        }
        super.onStateReplaced(s, w, pos, nouveau, bouge);
    }

    /** Relie deux poteaux ; le premier « porte » la corde (c'est lui qui la dessine). */
    public static boolean relier(World w, BlockPos a, BlockPos b) {
        if (w.getBlockEntity(a) instanceof Entite ea && w.getBlockEntity(b) instanceof Entite eb) {
            ea.relier(b, true);
            eb.relier(a, false);
            return true;
        }
        return false;
    }

    /** Ce que retient chaque poteau : l'autre bout, et s'il dessine la corde. */
    public static class Entite extends BlockEntity {
        @Nullable public BlockPos cible;
        public boolean porteCorde;

        public Entite(BlockPos pos, BlockState state) { super(OmbreBlocs.POTEAU_ENTITE, pos, state); }

        public void relier(@Nullable BlockPos c, boolean porte) {
            cible = c;
            porteCorde = porte && c != null;
            markDirty();
            if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }

        @Override
        protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
            super.writeNbt(nbt, reg);
            if (cible != null) nbt.putLong("cible", cible.asLong());
            nbt.putBoolean("porte", porteCorde);
        }

        @Override
        protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
            super.readNbt(nbt, reg);
            cible = nbt.contains("cible") ? BlockPos.fromLong(nbt.getLong("cible")) : null;
            porteCorde = nbt.getBoolean("porte");
        }

        @Nullable @Override
        public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }

        @Override
        public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup reg) { return createNbt(reg); }
    }
}
