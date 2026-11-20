package fr.fenua.ombre.bloc;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Monte-charge : une plate-forme à contrepoids posée au pied d'un mur (on le pose en regardant le mur).
 * Clic droit : on coupe la corde, le contrepoids tombe, et on monte jusqu'au toit.
 */
public class MonteCharge extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<MonteCharge> CODEC = createCodec(MonteCharge::new);
    private static final VoxelShape SOL = createCuboidShape(0, 0, 0, 16, 2, 16);
    private static final VoxelShape CONTOUR = VoxelShapes.union(SOL, createCuboidShape(0, 2, 0, 16, 16, 2));

    public MonteCharge(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalFacingBlock> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
        return VoxelShapes.union(SOL, tourner(s.get(FACING)));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return SOL; }

    private static VoxelShape tourner(Direction d) {
        return switch (d) {
            case SOUTH -> createCuboidShape(0, 2, 14, 16, 16, 16);
            case EAST -> createCuboidShape(14, 2, 0, 16, 16, 16);
            case WEST -> createCuboidShape(0, 2, 0, 2, 16, 16);
            default -> createCuboidShape(0, 2, 0, 16, 16, 2);
        };
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new Entite(pos, state); }

    @Override
    protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity joueur, BlockHitResult hit) {
        Direction mur = s.get(FACING);
        int sommet = sommet(w, pos, mur);
        if (sommet < 0) {
            if (w.isClient()) joueur.sendMessage(Text.literal("§7Le monte-charge doit être posé au pied d'un mur, face à lui."), true);
            return ActionResult.SUCCESS;
        }
        if (!w.isClient()) w.playSound(null, pos, SoundEvents.ENTITY_LEASH_KNOT_BREAK, SoundCategory.BLOCKS, 1f, 0.8f);
        else if (CoteClient.ICI[0] != null) CoteClient.ICI[0].monteCharge(pos, mur, sommet);
        return ActionResult.SUCCESS;
    }

    /**
     * La hauteur (y) du haut du mur devant la plate-forme : le premier vide au-dessus d'un bloc plein.
     * -1 s'il n'y a pas de mur d'au moins 2 blocs.
     */
    public static int sommet(BlockView w, BlockPos pos, Direction mur) {
        BlockPos col = pos.offset(mur);
        for (int y = pos.getY() + 1; y <= pos.getY() + 64; y++) {
            BlockPos c = col.withY(y);
            if (w.getBlockState(c).getCollisionShape(w, c).isEmpty()
                    && !w.getBlockState(c.down()).getCollisionShape(w, c.down()).isEmpty()) {
                return y - pos.getY() >= 2 ? y : -1;
            }
        }
        return -1;
    }

    /** Sert seulement à dessiner la corde jusqu'au toit. */
    public static class Entite extends BlockEntity {
        public Entite(BlockPos pos, BlockState state) { super(OmbreBlocs.MONTE_CHARGE_ENTITE, pos, state); }
    }
}
