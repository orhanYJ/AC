package fr.fenua.ombre.ac;

import fr.fenua.ombre.Ombre;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Du joueur vers le serveur : « emmène-moi à ce point de vue » (sa position). */
public record VoyagePayload(long pos) implements CustomPayload {
    public static final Id<VoyagePayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "voyage"));
    public static final PacketCodec<RegistryByteBuf, VoyagePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, VoyagePayload::pos, VoyagePayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
