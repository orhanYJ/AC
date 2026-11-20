package fr.fenua.ombre;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Message du client vers le serveur : « j'ai appuyé sur la touche n° action ». */
public record ActionPayload(int action) implements CustomPayload {
    public static final int INSTINCT = 0;
    public static final int SIFFLER = 1;

    public static final Id<ActionPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "action"));
    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC =
            PacketCodecs.VAR_INT.<RegistryByteBuf>cast().xmap(ActionPayload::new, ActionPayload::action);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
