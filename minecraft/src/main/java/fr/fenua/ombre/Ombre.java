package fr.fenua.ombre;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée du mod « L'Ombre — Infiltration ».
 * Chaque mécanique vit dans sa propre classe ; ici, on les branche sur le jeu.
 */
public class Ombre implements ModInitializer {
    public static final String MOD_ID = "ombre";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        OmbreItems.enregistrer();
        fr.fenua.ombre.bloc.OmbreBlocs.enregistrer();

        // Assassinats à la lame de nacre (clic gauche sur une créature)
        AttackEntityCallback.EVENT.register(Assassinat::auCoup);

        // Saut de la foi dans le foin, roulade à l'atterrissage
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Chute::autoriserDegats);

        // Contrat rempli quand une cible tombe
        ServerLivingEntityEvents.AFTER_DEATH.register(Gardes::apresMort);

        // Chaque tick : alarme qui se propage, points de vue, course libre
        ServerTickEvents.END_SERVER_TICK.register(Gardes::tick);

        // Touches spéciales envoyées par le client (instinct, siffler, assassiner)
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VigilancePayload.ID, VigilancePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID,
                (payload, context) -> Pouvoirs.agir(context.player(), payload.action(), payload.cible()));

        // Commandes /ombre arene et /ombre aide
        CommandRegistrationCallback.EVENT.register((dispatcher, registres, env) ->
                dispatcher.register(CommandManager.literal("ombre")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal(Aide.TEXTE), false); return 1; })
                        .then(CommandManager.literal("aide")
                                .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal(Aide.TEXTE), false); return 1; }))
                        .then(CommandManager.literal("parcours")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> CarteParcours.construire(ctx.getSource())))
                        .then(CommandManager.literal("arene")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> Arene.construire(ctx.getSource())))));

        LOG.info("L'Ombre — Infiltration est chargé.");
    }
}
