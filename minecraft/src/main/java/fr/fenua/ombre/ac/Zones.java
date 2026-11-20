package fr.fenua.ombre.ac;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Les zones interdites : un cercle de 14 blocs autour de chaque étendard de la garde.
 * Dedans, les gardes te repèrent deux fois plus vite. Le joueur voit une barre jaune quand il
 * s'en approche, rouge quand il y entre. Les étendards s'inscrivent ici en se chargeant.
 */
public final class Zones {
    private Zones() {}

    public static final int RAYON = 14, PROCHE = 7;
    public static final int AUCUNE = 0, PRES = 1, DEDANS = 2;
    private static final Map<RegistryKey<World>, Set<BlockPos>> ZONES = new ConcurrentHashMap<>();

    public static void ajouter(World w, BlockPos p) { ZONES.computeIfAbsent(w.getRegistryKey(), k -> ConcurrentHashMap.newKeySet()).add(p.toImmutable()); }
    public static void retirer(World w, BlockPos p) { Set<BlockPos> s = ZONES.get(w.getRegistryKey()); if (s != null) s.remove(p); }

    public static int etat(PlayerEntity p) {
        Set<BlockPos> s = ZONES.get(p.getWorld().getRegistryKey());
        if (s == null) return AUCUNE;
        double min = Double.MAX_VALUE;
        for (BlockPos b : s) {
            if (Math.abs(b.getY() - p.getY()) > 40) continue;
            double dx = b.getX() + 0.5 - p.getX(), dz = b.getZ() + 0.5 - p.getZ();
            min = Math.min(min, Math.sqrt(dx * dx + dz * dz));
        }
        return min <= RAYON ? DEDANS : min <= RAYON + PROCHE ? PRES : AUCUNE;
    }
}
