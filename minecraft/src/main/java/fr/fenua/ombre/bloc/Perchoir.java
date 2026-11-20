package fr.fenua.ombre.bloc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Perchoir : un point de vue à poser au sommet de n'importe quoi.
 * Se tenir dessus synchronise : tous les gardes à 64 blocs sont révélés 30 s (voir Gardes).
 * Une colonne de lumière le signale de loin.
 */
public class Perchoir extends Block {
    private static final VoxelShape FORME = VoxelShapes.union(
            createCuboidShape(3, 0, 3, 13, 12, 13), createCuboidShape(0, 12, 0, 16, 16, 16));

    public Perchoir(Settings s) { super(s); }

    @Override
    protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORME; }

    @Override
    public void randomDisplayTick(BlockState s, World w, BlockPos p, Random r) {
        for (int i = 0; i < 3; i++)
            w.addParticle(ParticleTypes.END_ROD, p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.3,
                    p.getY() + 1.2 + r.nextDouble() * 2, p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
    }
}
