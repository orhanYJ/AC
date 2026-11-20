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
 * Le mobilier de parkour, façon ville d'Assassin's Creed Unity.
 * Chaque bloc n'est qu'une forme : c'est le moteur de parkour (côté joueur)
 * qui s'y accroche, s'y pose ou s'y balance.
 */
public final class Mobilier {
    private Mobilier() {}

    /** Poutre : une pièce de bois étroite, posée dans le sens du regard ; on court dessus. */
    public static class Poutre extends Block {
        private static final VoxelShape X = createCuboidShape(0, 6, 5, 16, 12, 11);
        private static final VoxelShape Z = createCuboidShape(5, 6, 0, 11, 12, 16);

        public Poutre(Settings s) {
            super(s);
            setDefaultState(getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.Z));
        }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(Properties.HORIZONTAL_AXIS); }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(Properties.HORIZONTAL_AXIS, ctx.getHorizontalPlayerFacing().getAxis());
        }

        @Override
        protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return s.get(Properties.HORIZONTAL_AXIS) == Direction.Axis.X ? X : Z;
        }
    }

    /** Bloc orienté vers un mur (FACING = direction du mur). */
    public abstract static class Oriente extends Block {
        protected Oriente(Settings s) {
            super(s);
            setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(Properties.HORIZONTAL_FACING); }

        /** Posé sur le côté d'un mur, il s'y accroche ; sinon il regarde comme le joueur. */
        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            Direction cote = ctx.getSide();
            Direction mur = cote.getAxis().isHorizontal() ? cote.getOpposite() : ctx.getHorizontalPlayerFacing();
            return getDefaultState().with(Properties.HORIZONTAL_FACING, mur);
        }

        protected abstract VoxelShape formeNord();

        @Override
        protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return Formes.tourner(formeNord(), s.get(Properties.HORIZONTAL_FACING));
        }
    }

    /** Corniche : un rebord de pierre collé au mur, à saisir ou à longer. */
    public static class Corniche extends Oriente {
        public Corniche(Settings s) { super(s); }
        @Override protected VoxelShape formeNord() { return createCuboidShape(0, 11, 0, 16, 16, 7); }
    }

    /** Enseigne de boutique : une potence qui sort du mur (on s'y balance) et un panneau pendu dessous. */
    public static class Enseigne extends Oriente {
        public Enseigne(Settings s) { super(s); }
        @Override protected VoxelShape formeNord() {
            return VoxelShapes.union(createCuboidShape(7, 12, 0, 9, 14, 16), createCuboidShape(7.5, 2, 3, 8.5, 12, 14));
        }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return VoxelShapes.empty();
        }
    }

    /** Auvent de toile : on peut tomber dessus de haut sans se faire mal. */
    public static class Auvent extends Oriente {
        public Auvent(Settings s) { super(s); }
        @Override protected VoxelShape formeNord() { return createCuboidShape(0, 8, 0, 16, 12, 16); }
    }

    /** Échafaudage : un plateau de planches sur quatre poteaux, à empiler. */
    public static class Echafaudage extends Block {
        private static final VoxelShape FORME = VoxelShapes.union(
                createCuboidShape(0, 13, 0, 16, 16, 16),
                createCuboidShape(0, 0, 0, 2, 13, 2), createCuboidShape(14, 0, 0, 16, 13, 2),
                createCuboidShape(0, 0, 14, 2, 13, 16), createCuboidShape(14, 0, 14, 16, 13, 16));
        public Echafaudage(Settings s) { super(s); }
        @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORME; }
    }

    /** Fût de lampadaire, à empiler ; il s'escalade comme un mur. */
    public static class Lampadaire extends Block {
        private static final VoxelShape FORME = createCuboidShape(6, 0, 6, 10, 16, 10);
        public Lampadaire(Settings s) { super(s); }
        @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORME; }
    }

    /** Tête de lampadaire : la lanterne, dont le chapeau sert de perchoir. */
    public static class TeteLampadaire extends Block {
        private static final VoxelShape FORME = VoxelShapes.union(
                createCuboidShape(6, 0, 6, 10, 4, 10), createCuboidShape(4, 4, 4, 12, 12, 12),
                createCuboidShape(3, 12, 3, 13, 15, 13));
        public TeteLampadaire(Settings s) { super(s); }
        @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORME; }
    }
}
