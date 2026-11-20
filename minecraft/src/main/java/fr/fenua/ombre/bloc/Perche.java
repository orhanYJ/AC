package fr.fenua.ombre.bloc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Perche : une barre horizontale, sans collision. On la saisit au vol (les mains passent à côté),
 * on se balance, et on est projeté en avant (Espace ou Z), ou on lâche (Maj).
 * Elle se pose en travers du regard.
 */
public class Perche extends Block {
    private static final VoxelShape AXE_X = createCuboidShape(0, 6, 6, 16, 10, 10);
    private static final VoxelShape AXE_Z = createCuboidShape(6, 6, 0, 10, 10, 16);

    public Perche(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(Properties.HORIZONTAL_AXIS); }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_AXIS,
                ctx.getHorizontalPlayerFacing().rotateYClockwise().getAxis());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
        return s.get(Properties.HORIZONTAL_AXIS) == Direction.Axis.X ? AXE_X : AXE_Z;
    }
}
