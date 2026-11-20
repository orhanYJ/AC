package fr.fenua.ombre;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Message du client vers le serveur : « j'ai appuyé sur la touche n° action » (et sur quelle créature). */
public record ActionPayload(int action, int cible) implements CustomPayload {
    public static final int INSTINCT = 0;
    public static final int SIFFLER = 1;
    /** Le client s'est rattrapé (rebord, course sur mur) : on oublie la hauteur de chute. */
    public static final int RATTRAPE = 2;
    /** Touche R : assassiner la créature marquée en rouge (cible = son numéro). */
    public static final int ASSASSINER = 3;

    public ActionPayload(int action) { this(action, -1); }

    public static final Id<ActionPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "action"));
    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ActionPayload::action,
            PacketCodecs.VAR_INT, ActionPayload::cible,
            ActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
