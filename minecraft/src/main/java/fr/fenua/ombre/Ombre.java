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
        fr.fenua.ombre.objet.Objets.enregistrer();
        fr.fenua.ombre.entite.Entites.enregistrer();

        // Assassinats à la lame de nacre (clic gauche sur une créature)
        AttackEntityCallback.EVENT.register(Assassinat::auCoup);

        // Saut de la foi dans le foin, roulade à l'atterrissage
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Chute::autoriserDegats);

        // Contrat rempli quand une cible tombe
        ServerLivingEntityEvents.AFTER_DEATH.register(Gardes::apresMort);

        // Chaque tick : alarme qui se propage, points de vue, course libre
        ServerTickEvents.END_SERVER_TICK.register(Gardes::tick);

        // L'initiation (la Ruelle d'Or) : on vérifie deux fois par seconde si l'étape est réussie
        ServerTickEvents.END_SERVER_TICK.register(fr.fenua.ombre.initiation.Cours::tick);

        // Les plumes de corbeau et la course des toits
        ServerTickEvents.END_SERVER_TICK.register(fr.fenua.ombre.ac.Defis::tick);

        // Touches spéciales envoyées par le client (instinct, siffler, assassiner)
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(fr.fenua.ombre.ac.EtatPayload.ID, fr.fenua.ombre.ac.EtatPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(fr.fenua.ombre.ac.JournalPayload.ID, fr.fenua.ombre.ac.JournalPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(fr.fenua.ombre.ac.VoyagePayload.ID, fr.fenua.ombre.ac.VoyagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ExecutionPayload.ID, ExecutionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SynchroPayload.ID, SynchroPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID,
                (payload, context) -> Pouvoirs.agir(context.player(), payload.action(), payload.cible()));
        ServerPlayNetworking.registerGlobalReceiver(fr.fenua.ombre.ac.VoyagePayload.ID,
                (payload, context) -> fr.fenua.ombre.ac.Voyage.partir(context.player(), payload.pos()));

        // Commandes /ombre arene et /ombre aide
        CommandRegistrationCallback.EVENT.register((dispatcher, registres, env) ->
                dispatcher.register(CommandManager.literal("ombre")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal(Aide.TEXTE), false); return 1; })
                        .then(CommandManager.literal("aide")
                                .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal(Aide.TEXTE), false); return 1; }))
                        .then(CommandManager.literal("parcours")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> CarteParcours.construire(ctx.getSource())))
                        .then(CommandManager.literal("initiation")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> fr.fenua.ombre.initiation.RuelleDOr.construire(ctx.getSource()))
                                .then(CommandManager.argument("etape", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 99))
                                        .executes(ctx -> fr.fenua.ombre.initiation.Initiation.COURS.aller(ctx.getSource().getPlayerOrThrow(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "etape")))))
                        .then(CommandManager.literal("rappel")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> fr.fenua.ombre.initiation.JardinRoyal.construire(ctx.getSource()))
                                .then(CommandManager.argument("etape", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 99))
                                        .executes(ctx -> fr.fenua.ombre.initiation.Rappel.COURS.aller(ctx.getSource().getPlayerOrThrow(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "etape")))))
                        .then(CommandManager.literal("prague")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> CartePrague.construire(ctx.getSource())))
                        .then(CommandManager.literal("scene")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("numero", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, Choregraphie.NB))
                                        .executes(ctx -> Assassinat.imposerScene(ctx.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "numero") - 1))))
                        .then(CommandManager.literal("kit")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> { Kit.donner(ctx.getSource().getPlayerOrThrow());
                                    ctx.getSource().sendFeedback(() -> Text.literal("§6Équipement de l'assassin§r reçu."), false); return 1; }))
                        .then(CommandManager.literal("vitrine")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> Vitrine.construire(ctx.getSource())))
                        .then(CommandManager.literal("arene")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> Arene.construire(ctx.getSource())))));

        LOG.info("L'Ombre — Infiltration est chargé.");
    }
}
