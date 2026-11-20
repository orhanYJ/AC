package fr.fenua.ombre;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Message du serveur vers le joueur : « ton assassinat commence ». Le joueur joue alors le geste
 * (bond, pas derrière la victime…), la pose et la caméra ; le serveur, lui, fige la victime.
 */
public record ExecutionPayload(int type, int victime, double x, double y, double z, float yaw) implements CustomPayload {
    public static final int SOL = 0, AERIEN = 1, CACHETTE = 2, REBORD = 3;
    /** Durée du geste, en ticks (20 par seconde). */
    public static final int[] DUREE = {8, 11, 9, 9};

    public static final Id<ExecutionPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "execution"));
    public static final PacketCodec<RegistryByteBuf, ExecutionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ExecutionPayload::type, PacketCodecs.VAR_INT, ExecutionPayload::victime,
            PacketCodecs.DOUBLE, ExecutionPayload::x, PacketCodecs.DOUBLE, ExecutionPayload::y, PacketCodecs.DOUBLE, ExecutionPayload::z,
            PacketCodecs.FLOAT, ExecutionPayload::yaw, ExecutionPayload::new);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
