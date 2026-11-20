package fr.fenua.ombre.bloc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Meule de foin, comme dans Assassin's Creed : on traverse la paille (pas de collision),
 * on peut y plonger de très haut sans se faire mal (saut de la foi), et on s'y cache même debout.
 * À empiler sur 2 blocs de haut pour qu'un homme y disparaisse entièrement.
 */
public class Meule extends Block {
    public Meule(Settings s) { super(s); }

    /** Dans la paille, on avance moins vite, et la chute est amortie. */
    @Override
    protected void onEntityCollision(BlockState s, World w, BlockPos p, Entity e) {
        e.slowMovement(s, new Vec3d(0.8, 0.5, 0.8));
    }
}
