package fr.fenua.ombre;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Message du serveur vers le joueur : « ton assassinat commence ». Le joueur joue alors le geste
 * (sa course, ses poses, la caméra) et anime le corps de la victime ; le serveur, lui, la déplace selon la
 * chorégraphie. « yaw » est l'axe du joueur vers la victime (voir Choregraphie).
 */
public record ExecutionPayload(int type, int victime, double x, double y, double z, float yaw) implements CustomPayload {
    /** Quelques scènes (toutes sont dans Choregraphie). */
    public static final int SOL = Choregraphie.DOS, AERIEN = Choregraphie.AERIEN, CACHETTE = Choregraphie.CACHETTE,
            REBORD = Choregraphie.REBORD, PROJECTION = Choregraphie.PROJECTION;
    /** Durée du geste, en ticks (20 par seconde). */
    public static final int[] DUREE = Choregraphie.DUREE;

    public static final Id<ExecutionPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "execution"));
    public static final PacketCodec<RegistryByteBuf, ExecutionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ExecutionPayload::type, PacketCodecs.VAR_INT, ExecutionPayload::victime,
            PacketCodecs.DOUBLE, ExecutionPayload::x, PacketCodecs.DOUBLE, ExecutionPayload::y, PacketCodecs.DOUBLE, ExecutionPayload::z,
            PacketCodecs.FLOAT, ExecutionPayload::yaw, ExecutionPayload::new);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
