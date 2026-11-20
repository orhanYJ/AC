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
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Les cachettes d'assassin, comme dans Assassin's Creed. Celui qui s'y tient est invisible
 * (voir Discretion.dansCachette), et peut y attirer un garde qui passe tout près (touche R).
 */
public final class Cachettes {
    private Cachettes() {}

    /** Le puits : une margelle de pierre et de l'eau. On saute dedans, on en ressort d'un saut. */
    public static class Puits extends Block {
        private static final VoxelShape MARGELLE = VoxelShapes.union(
                createCuboidShape(0, 0, 0, 16, 16, 2), createCuboidShape(0, 0, 14, 16, 16, 16),
                createCuboidShape(0, 0, 2, 2, 16, 14), createCuboidShape(14, 0, 2, 16, 16, 14));

        public Puits(Settings s) { super(s); }

        @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return VoxelShapes.fullCube(); }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return MARGELLE; }
    }

    /** La cabine à rideau : on entre par le rideau, on y reste debout, caché. Ouverte vers celui qui la pose. */
    public static class Cabine extends Block {
        private static final VoxelShape PAROIS = VoxelShapes.union(
                createCuboidShape(0, 0, 14, 16, 24, 16), createCuboidShape(0, 0, 0, 1, 24, 14), createCuboidShape(15, 0, 0, 16, 24, 14));
        private static final VoxelShape CONTOUR = VoxelShapes.union(PAROIS, createCuboidShape(1, 0, 0, 15, 16, 2));

        public Cabine(Settings s) {
            super(s);
            setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(Properties.HORIZONTAL_FACING); }

        /** FACING = côté du rideau : vers le joueur qui la pose. */
        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }

        @Override
        protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return Formes.tourner(CONTOUR, s.get(Properties.HORIZONTAL_FACING));
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return Formes.tourner(PAROIS, s.get(Properties.HORIZONTAL_FACING));
        }
    }
}
