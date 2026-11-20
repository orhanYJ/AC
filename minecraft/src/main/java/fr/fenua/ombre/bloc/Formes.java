package fr.fenua.ombre.bloc;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/** Outil : tourner une forme dessinée « face au nord » vers une autre direction. */
public final class Formes {
    private Formes() {}

    public static VoxelShape tourner(VoxelShape nord, Direction d) {
        VoxelShape res = VoxelShapes.empty();
        for (Box b : nord.getBoundingBoxes()) {
            Box r = switch (d) {
                case EAST -> new Box(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
                case SOUTH -> new Box(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
                case WEST -> new Box(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
                default -> b;
            };
            res = VoxelShapes.union(res, VoxelShapes.cuboid(r));
        }
        return res;
    }
}
