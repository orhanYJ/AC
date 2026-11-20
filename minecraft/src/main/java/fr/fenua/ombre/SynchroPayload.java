package fr.fenua.ombre;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Message du serveur vers le joueur : « point de vue synchronisé » (la caméra fait un tour). */
public record SynchroPayload() implements CustomPayload {
    public static final Id<SynchroPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "synchro"));
    public static final PacketCodec<RegistryByteBuf, SynchroPayload> CODEC = PacketCodec.unit(new SynchroPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
