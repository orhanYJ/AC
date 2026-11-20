package fr.fenua.ombre.ac;

import fr.fenua.ombre.Ombre;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Du serveur vers le joueur : le journal de mission (« 1 » ou « 0 » devant chaque objectif, fait ou non)
 * les points de vue synchronisés (positions et noms), pour la boussole et le voyage rapide,
 * et les marqueurs de mission (cibles vivantes, personnages à rencontrer) pour la boussole.
 */
public record JournalPayload(List<String> objectifs, List<Long> vues, List<String> nomsVues, List<Long> cibles) implements CustomPayload {
    public static final Id<JournalPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "journal"));
    public static final PacketCodec<RegistryByteBuf, JournalPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.collect(PacketCodecs.toList()), JournalPayload::objectifs,
            PacketCodecs.VAR_LONG.collect(PacketCodecs.toList()), JournalPayload::vues,
            PacketCodecs.STRING.collect(PacketCodecs.toList()), JournalPayload::nomsVues,
            PacketCodecs.VAR_LONG.collect(PacketCodecs.toList()), JournalPayload::cibles, JournalPayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
