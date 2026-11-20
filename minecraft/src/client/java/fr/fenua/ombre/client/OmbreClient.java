package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Côté joueur : deux touches, modifiables dans Options › Commandes. */
public class OmbreClient implements ClientModInitializer {
    private static KeyBinding instinct;
    private static KeyBinding siffler;

    @Override
    public void onInitializeClient() {
        instinct = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ombre.instinct", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.ombre"));
        siffler = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ombre.siffler", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.ombre"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (instinct.wasPressed()) ClientPlayNetworking.send(new ActionPayload(ActionPayload.INSTINCT));
            while (siffler.wasPressed()) ClientPlayNetworking.send(new ActionPayload(ActionPayload.SIFFLER));
        });
    }
}
