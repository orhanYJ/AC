package fr.fenua.ombre.ac;

import fr.fenua.ombre.Ombre;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Du serveur vers le joueur, 4 fois par seconde : le soupçon des gardes proches (0 à 100),
 * la zone interdite (0 aucune, 1 proche, 2 dedans) et la notoriété (0 à 100).
 */
public record EtatPayload(List<Integer> gardes, List<Integer> niveaux, int zone, int notoriete) implements CustomPayload {
    public static final Id<EtatPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "etat"));
    public static final PacketCodec<RegistryByteBuf, EtatPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), EtatPayload::gardes,
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), EtatPayload::niveaux,
            PacketCodecs.VAR_INT, EtatPayload::zone,
            PacketCodecs.VAR_INT, EtatPayload::notoriete, EtatPayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
