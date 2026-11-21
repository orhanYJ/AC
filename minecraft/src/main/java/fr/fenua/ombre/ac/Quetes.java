package fr.fenua.ombre.ac;

import net.minecraft.server.network.ServerPlayerEntity;

/** Les quêtes (histoire principale et quêtes secondaires). */
public final class Quetes {
    private Quetes() {}

    /** Quelque chose s'est passé (« fouille », « tue », « parle »…) : les quêtes en cours en tiennent compte. */
    public static void evenement(ServerPlayerEntity p, String quoi, Object detail) {}
}
