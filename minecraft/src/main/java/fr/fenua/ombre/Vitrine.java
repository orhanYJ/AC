package fr.fenua.ombre;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * /ombre vitrine : expose tous les blocs du mod en rangées, devant un mur de pierre de taille,
 * pour les voir d'un coup. Les blocs muraux (balcons, enseignes, affiches…) s'accrochent au mur.
 */
public final class Vitrine {
    private Vitrine() {}

    public static int construire(ServerCommandSource src) {
        ServerWorld w = src.getWorld();
        BlockPos o = BlockPos.ofFloored(src.getPosition()).add(2, 0, 3);
        List<Block> blocs = new ArrayList<>();
        for (Block b : Registries.BLOCK) if (Registries.BLOCK.getId(b).getNamespace().equals(Ombre.MOD_ID)) blocs.add(b);
        int parRangee = 20, rangees = (blocs.size() + parRangee - 1) / parRangee;
        BlockState fond = Registries.BLOCK.get(net.minecraft.util.Identifier.of(Ombre.MOD_ID, "pierre_de_taille")).getDefaultState();
        BlockState sol = Registries.BLOCK.get(net.minecraft.util.Identifier.of(Ombre.MOD_ID, "parquet")).getDefaultState();
        for (int r = 0; r < rangees; r++) {
            int z = o.getZ() + r * 5;
            for (int i = 0; i < parRangee * 2 + 1; i++) {
                for (int y = 0; y <= 3; y++) w.setBlockState(new BlockPos(o.getX() + i, o.getY() + y, z - 1), fond, Block.NOTIFY_LISTENERS);
                for (int dz = -1; dz <= 3; dz++) {
                    w.setBlockState(new BlockPos(o.getX() + i, o.getY() - 1, z + dz), sol, Block.NOTIFY_LISTENERS);
                    for (int y = 0; y <= 3; y++)
                        if (dz >= 0) w.setBlockState(new BlockPos(o.getX() + i, o.getY() + y, z + dz), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
        for (int k = 0; k < blocs.size(); k++) {
            BlockState s = blocs.get(k).getDefaultState();
            if (s.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING))
                s = s.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, Direction.NORTH);
            BlockPos p = new BlockPos(o.getX() + (k % parRangee) * 2 + 1, o.getY(), o.getZ() + (k / parRangee) * 5);
            w.setBlockState(p, s, Block.NOTIFY_ALL);
        }
        src.sendFeedback(() -> Text.literal("§6Vitrine§r : " + blocs.size() + " blocs du mod, en " + rangees + " rangées devant toi."), true);
        return 1;
    }
}
