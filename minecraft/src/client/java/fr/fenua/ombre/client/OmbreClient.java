package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import fr.fenua.ombre.VigilancePayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import fr.fenua.ombre.bloc.CoteClient;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

/** Côté joueur : les touches (modifiables dans Options › Commandes), le parkour, la cible d'assassinat, le dessin des cordes. */
public class OmbreClient implements ClientModInitializer {
    private static KeyBinding instinct, siffler, aide, assassiner;

    /** Le nom de la touche d'assassinat (R par défaut, modifiable). */
    static String toucheAssassiner() { return assassiner.getBoundKeyLocalizedText().getString().toUpperCase(); }

    @Override
    public void onInitializeClient() {
        instinct = touche("key.ombre.instinct", GLFW.GLFW_KEY_V);
        siffler = touche("key.ombre.siffler", GLFW.GLFW_KEY_B);
        aide = touche("key.ombre.aide", GLFW.GLFW_KEY_H);
        assassiner = touche("key.ombre.assassiner", GLFW.GLFW_KEY_R);

        // Le serveur dit qui nous a repérés ; l'invite d'assassinat s'affiche sous le viseur
        ClientPlayNetworking.registerGlobalReceiver(VigilancePayload.ID, (payload, ctx) -> Assassin.recevoir(payload.reperes()));
        HudRenderCallback.EVENT.register((ctx, compteur) -> Assassin.dessiner(ctx, MinecraftClient.getInstance()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!client.isPaused()) { Parcours.tick(client); Assassin.tick(client); }
            while (assassiner.wasPressed()) Assassin.frapper();
            while (instinct.wasPressed()) ClientPlayNetworking.send(new ActionPayload(ActionPayload.INSTINCT));
            while (siffler.wasPressed()) ClientPlayNetworking.send(new ActionPayload(ActionPayload.SIFFLER));
            while (aide.wasPressed()) client.setScreen(new EcranAide());
        });

        // Les blocs appellent ici quand le joueur part en tyrolienne ou en monte-charge
        CoteClient.ICI[0] = new CoteClient() {
            @Override public void tyrolienne(BlockPos de, BlockPos vers) { Parcours.demarrerTyrolienne(de, vers); }
            @Override public void monteCharge(BlockPos pos, Direction mur, int sommet) { Parcours.demarrerMonteCharge(pos, mur, sommet); }
        };

        // L'eau du puits prend la couleur de l'eau
        ColorProviderRegistry.BLOCK.register((etat, monde, pos, i) -> 0x3F76E4, OmbreBlocs.PUITS);

        BlockEntityRendererFactories.register(OmbreBlocs.POTEAU_ENTITE, ctx -> new RenduCordes.Poteau());
        BlockEntityRendererFactories.register(OmbreBlocs.MONTE_CHARGE_ENTITE, ctx -> new RenduCordes.Lift());
    }

    private static KeyBinding touche(String nom, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(nom, InputUtil.Type.KEYSYM, code, "key.categories.ombre"));
    }
}
