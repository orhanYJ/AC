package fr.fenua.ombre.bloc;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Pont vers le code du joueur (côté client), qui gère les mouvements.
 * Le client le remplit au démarrage ; sur un serveur seul, il reste vide.
 */
public interface CoteClient {
    void tyrolienne(BlockPos depart, BlockPos arrivee);
    void monteCharge(BlockPos pos, Direction mur, int sommet);

    /** Rempli par OmbreClient. */
    CoteClient[] ICI = new CoteClient[1];

    /** Hauteur de la corde au-dessus du poteau. */
    double ACCROCHE = 0.9;

    /** Un point du câble de tyrolienne, de A (t = 0) à B (t = 1), avec un léger creux au milieu. */
    static Vec3d pointCable(Vec3d a, Vec3d b, double t) {
        double creux = a.distanceTo(b) * 0.03;
        return a.lerp(b, t).add(0, -creux * 4 * t * (1 - t), 0);
    }
}
