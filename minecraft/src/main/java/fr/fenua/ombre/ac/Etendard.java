package fr.fenua.ombre.ac;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * L'étendard de la garde : planté quelque part, il fait de ses 14 blocs alentour une zone interdite.
 * Son « entité de bloc » sert seulement à s'inscrire dans {@link Zones} quand il se charge
 * (et à s'en retirer quand on le casse ou que le morceau de carte se décharge).
 */
public class Etendard extends BlockWithEntity {
    public static final MapCodec<Etendard> CODEC = createCodec(Etendard::new);
    private static final VoxelShape FORME = Block.createCuboidShape(5, 0, 5, 11, 16, 11);
    public static BlockEntityType<Entite> TYPE;

    public Etendard(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState s) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORME; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(Properties.HORIZONTAL_FACING); }
    @Override public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing());
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new Entite(pos, state); }

    /** Quelques braises rouges montent de l'étendard : on le repère de loin. */
    @Override
    public void randomDisplayTick(BlockState s, World w, BlockPos p, Random r) {
        if (r.nextInt(2) != 0) return;
        w.addParticle(new DustParticleEffect(new Vector3f(0.85f, 0.1f, 0.1f), 1.2f),
                p.getX() + 0.2 + r.nextDouble() * 0.6, p.getY() + 1.2 + r.nextDouble() * 0.6, p.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.02, 0);
    }

    public static class Entite extends BlockEntity {
        public Entite(BlockPos pos, BlockState state) { super(TYPE, pos, state); }

        @Override
        public void setWorld(World w) {
            super.setWorld(w);
            if (!w.isClient()) Zones.ajouter(w, getPos());
        }

        @Override
        public void markRemoved() {
            super.markRemoved();
            if (world != null && !world.isClient()) Zones.retirer(world, getPos());
        }
    }
}
