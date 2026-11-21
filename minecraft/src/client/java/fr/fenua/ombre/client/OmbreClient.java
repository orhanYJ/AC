package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import fr.fenua.ombre.bloc.CoteClient;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
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
    private static KeyBinding instinct, siffler, aide, assassiner, roue, voyage, aigle, competences;

    /** Le nom de la touche d'assassinat (R par défaut, modifiable). */
    static String toucheAssassiner() { return assassiner.getBoundKeyLocalizedText().getString().toUpperCase(); }

    @Override
    public void onInitializeClient() {
        instinct = touche("key.ombre.instinct", GLFW.GLFW_KEY_V);
        siffler = touche("key.ombre.siffler", GLFW.GLFW_KEY_B);
        aide = touche("key.ombre.aide", GLFW.GLFW_KEY_H);
        assassiner = touche("key.ombre.assassiner", GLFW.GLFW_KEY_R);
        roue = touche("key.ombre.roue", GLFW.GLFW_KEY_G);       // (X est déjà pris par Minecraft)
        voyage = touche("key.ombre.voyage", GLFW.GLFW_KEY_J);
        aigle = touche("key.ombre.aigle", GLFW.GLFW_KEY_N);
        competences = touche("key.ombre.competences", GLFW.GLFW_KEY_K);
        // La bourse, les points, les compétences ; l'étal d'un marchand
        ClientPlayNetworking.registerGlobalReceiver(fr.fenua.ombre.ac.Profil.ProfilPayload.ID, (payload, ctx) -> ProfilClient.recevoir(payload));
     // N : même place en AZERTY et en QWERTY   // J : même place en AZERTY et en QWERTY

        // Ce que pensent les gardes, la zone, la notoriété ; le journal et les points de vue
        ClientPlayNetworking.registerGlobalReceiver(fr.fenua.ombre.ac.EtatPayload.ID, (payload, ctx) -> EtatAC.recevoir(payload));
        ClientPlayNetworking.registerGlobalReceiver(fr.fenua.ombre.ac.JournalPayload.ID, (payload, ctx) -> EtatAC.recevoir(payload));
        WorldRenderEvents.AFTER_ENTITIES.register(HudAC::dessinerMonde);
        // Les avis de recherche repérés par la vue de l'aigle
        WorldRenderEvents.AFTER_ENTITIES.register(Affiches::dessiner);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(Affiches::tick);

        // Le serveur dit qui nous a repérés ; l'invite d'assassinat s'affiche sous le viseur
        ClientPlayNetworking.registerGlobalReceiver(fr.fenua.ombre.ExecutionPayload.ID, (payload, ctx) -> { Parcours.executer(payload); Assassin.apresExecution(payload.type()); });
        ClientPlayNetworking.registerGlobalReceiver(fr.fenua.ombre.SynchroPayload.ID, (payload, ctx) -> Parcours.synchro());

        // La corde du lance-grappin, de la main jusqu'au crochet
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            net.minecraft.util.math.Vec3d ancre = Parcours.ancreGrappin();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (ancre == null || mc.player == null || ctx.matrixStack() == null || ctx.consumers() == null) return;
            net.minecraft.util.math.Vec3d cam = ctx.camera().getPos();
            net.minecraft.util.math.Vec3d main = mc.player.getLerpedPos(ctx.tickCounter().getTickDelta(true)).add(0, 1.9, 0);
            ctx.matrixStack().push();
            ctx.matrixStack().translate(-cam.x, -cam.y, -cam.z);
            RenduCordes.dessiner(ctx.matrixStack(), ctx.consumers(), mc.world, BlockPos.ORIGIN, main, ancre, false);
            ctx.matrixStack().pop();
        });
        HudRenderCallback.EVENT.register((ctx, compteur) -> {
            HudAC.dessiner(ctx, MinecraftClient.getInstance());
            Assassin.dessiner(ctx, MinecraftClient.getInstance());
            Cinema.dessiner(ctx, MinecraftClient.getInstance());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!client.isPaused()) { ProfilClient.tick(client); Parcours.tick(client); Assassin.tick(client); HudAC.tick(client); Cinema.tick(client); }
            while (assassiner.wasPressed()) Assassin.frapper();
            while (instinct.wasPressed()) ClientPlayNetworking.send(new ActionPayload(ActionPayload.INSTINCT));
            while (siffler.wasPressed()) ClientPlayNetworking.send(new ActionPayload(ActionPayload.SIFFLER));
            while (aide.wasPressed()) client.setScreen(new EcranAide());
            while (roue.wasPressed()) if (client.currentScreen == null) client.setScreen(new RoueOutils(roue));
            while (aigle.wasPressed()) if (client.currentScreen == null) Cinema.aigle(client.player);
            while (voyage.wasPressed()) if (client.currentScreen == null) client.setScreen(new EcranVoyage());
            while (competences.wasPressed()) if (client.currentScreen == null) client.setScreen(new EcranCompetences());
        });

        // Les blocs appellent ici quand le joueur part en tyrolienne ou en monte-charge
        CoteClient.ICI[0] = new CoteClient() {
            @Override public void tyrolienne(BlockPos de, BlockPos vers) { Parcours.demarrerTyrolienne(de, vers); }
            @Override public void monteCharge(BlockPos pos, Direction mur, int sommet) { Parcours.demarrerMonteCharge(pos, mur, sommet); }
            @Override public void grappin() { Parcours.lancerGrappin(); }
        };

        // L'eau du puits et de la fontaine prend la couleur de l'eau
        ColorProviderRegistry.BLOCK.register((etat, monde, pos, i) -> 0x3F76E4, OmbreBlocs.PUITS, OmbreBlocs.FONTAINE);
        // Les blocs ajourés (grilles, treillis, verre) : les trous restent transparents
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), OmbreBlocs.BALCON, OmbreBlocs.CHARPENTE,
                OmbreBlocs.REVERBERE, OmbreBlocs.LUCARNE, OmbreBlocs.COLONNE, OmbreBlocs.AFFICHE);

        // Les variantes ajourées (grilles de balcon, fleurs des étals, verre des réverbères)
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), fr.fenua.ombre.bloc.Variantes.AJOURES.toArray(new net.minecraft.block.Block[0]));

        // Les personnages et ce qu'on lance
        EntityRendererRegistry.register(fr.fenua.ombre.entite.Entites.PASSANT, ctx -> new RenduHumain<>(ctx, RenduHumain::passant));
        EntityRendererRegistry.register(fr.fenua.ombre.entite.Entites.FIGURE, ctx -> new RenduHumain<>(ctx, RenduHumain::figure));
        EntityRendererRegistry.register(fr.fenua.ombre.entite.Entites.HALLEBARDIER, ctx -> new RenduHumain<>(ctx, e -> RenduHumain.tex("hallebardier")));
        EntityRendererRegistry.register(fr.fenua.ombre.entite.Entites.ARBALETRIER, ctx -> new RenduHumain<>(ctx, e -> RenduHumain.tex("arbaletrier")));
        EntityRendererRegistry.register(fr.fenua.ombre.entite.Entites.CAPITAINE, ctx -> new RenduHumain<>(ctx, e -> RenduHumain.tex("capitaine")));
        EntityRendererRegistry.register(fr.fenua.ombre.objet.Objets.PROJECTILE, FlyingItemEntityRenderer::new);

        BlockEntityRendererFactories.register(OmbreBlocs.POTEAU_ENTITE, ctx -> new RenduCordes.Poteau());
        BlockEntityRendererFactories.register(OmbreBlocs.MONTE_CHARGE_ENTITE, ctx -> new RenduCordes.Lift());
    }

    private static KeyBinding touche(String nom, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(nom, InputUtil.Type.KEYSYM, code, "key.categories.ombre"));
    }
}
