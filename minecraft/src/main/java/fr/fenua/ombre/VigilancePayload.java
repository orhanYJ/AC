package fr.fenua.ombre;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Message du serveur vers le joueur : les créatures proches qui l'ont repéré.
 * Le joueur ne le sait pas tout seul (ce que « pense » un garde n'existe que sur le serveur) ;
 * il en a besoin pour savoir qui entourer de rouge.
 */
public record VigilancePayload(List<Integer> reperes) implements CustomPayload {
    public static final Id<VigilancePayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "vigilance"));
    public static final PacketCodec<RegistryByteBuf, VigilancePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), VigilancePayload::reperes,
            VigilancePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
