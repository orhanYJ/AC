package fr.fenua.ombre.ac;

import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.Ombre;
import fr.fenua.ombre.OmbreItems;
import fr.fenua.ombre.objet.Objets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Le profil de l'assassin, comme dans Assassin's Creed : sa bourse (des thalers), ses points de compétence
 * et les compétences apprises. Il sert aussi les marchands (boutiques) et la fouille des corps.
 *
 * Tout est rangé sur le joueur : la bourse et les points dans le tableau des scores (« ombre_thalers »,
 * « ombre_points »), chaque compétence apprise dans une étiquette « ombre_comp:<id> ».
 */
public final class Profil {
    private Profil() {}

    // ───────────────────────── les compétences ─────────────────────────

    /** Une compétence : sa branche (0 à 3), son rang (0 à 2 : il faut le rang d'avant), son coût en points. */
    public record Competence(String id, int branche, int rang, String nom, String effet) {
        public int cout() { return rang + 1; }
    }

    public static final String[] BRANCHES = {"Ombre", "Lame", "Toits", "Ruse"};
    public static final List<Competence> COMPETENCES = List.of(
            new Competence("velours", 0, 0, "Pas de velours", "Accroupi, les gardes te remarquent bien moins loin."),
            new Competence("foule", 0, 1, "Visage dans la foule", "Un seul passant près de toi suffit pour t'y fondre."),
            new Competence("nuit", 0, 2, "Enfant de la nuit", "Dans le noir, tu es presque invisible."),
            new Competence("chaine", 1, 0, "Enchaînement", "Cinq secondes, au lieu de deux et demie, pour enchaîner un autre assassinat."),
            new Competence("double", 1, 1, "Double lame", "L'assassinat double, même sans seconde lame en main."),
            new Competence("robuste", 1, 2, "Robustesse", "Quatre cœurs de plus."),
            new Competence("grimpeur", 2, 0, "Grimpeur", "Tu grimpes et longes les murs plus vite."),
            new Competence("chat", 2, 1, "Pieds de chat", "Les chutes font moitié moins mal, et trois blocs de plus sans mal."),
            new Competence("bond", 2, 2, "Bondissant", "Tes sauts portent plus haut."),
            new Competence("fouille", 3, 0, "Détrousseur", "Deux fois plus de butin sur les corps."),
            new Competence("voleur", 3, 1, "Doigts de fée", "Vol à la tire : bourses doublées, et jamais pris, même de face."),
            new Competence("marchand", 3, 2, "Marchandeur", "Tout coûte un quart de moins chez les marchands."));

    public static final String COMP = "ombre_comp:";

    public static boolean a(LivingEntity p, String id) { return p.getCommandTags().contains(COMP + id); }

    public static Competence competence(String id) {
        for (Competence c : COMPETENCES) if (c.id().equals(id)) return c;
        return null;
    }

    /** Apprendre une compétence (écran des compétences, touche K). */
    public static void apprendre(ServerPlayerEntity p, String id) {
        Competence c = competence(id);
        if (c == null || a(p, id)) return;
        for (Competence d : COMPETENCES)
            if (d.branche() == c.branche() && d.rang() == c.rang() - 1 && !a(p, d.id())) {
                p.sendMessage(Text.literal("§cIl faut d'abord « " + d.nom() + " »."), true);
                return;
            }
        if (points(p) < c.cout()) { p.sendMessage(Text.literal("§cPas assez de points de compétence."), true); return; }
        ajouterPoints(p, -c.cout(), null);
        p.addCommandTag(COMP + id);
        appliquer(p);
        p.sendMessage(Text.literal("§6✦ Compétence apprise : " + c.nom()), true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.3f);
        envoyer(p, -1);
    }

    private static final Identifier MOD_VIE = Identifier.of(Ombre.MOD_ID, "robustesse"),
            MOD_CHUTE = Identifier.of(Ombre.MOD_ID, "pieds_de_chat"), MOD_CHUTE2 = Identifier.of(Ombre.MOD_ID, "pieds_de_chat_2"),
            MOD_SAUT = Identifier.of(Ombre.MOD_ID, "bondissant");

    /** Les compétences qui changent le corps : on pose (ou retire) leurs modificateurs. */
    public static void appliquer(ServerPlayerEntity p) {
        modif(p, EntityAttributes.GENERIC_MAX_HEALTH, MOD_VIE, a(p, "robuste"), 8, EntityAttributeModifier.Operation.ADD_VALUE);
        modif(p, EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, MOD_CHUTE, a(p, "chat"), 3, EntityAttributeModifier.Operation.ADD_VALUE);
        modif(p, EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER, MOD_CHUTE2, a(p, "chat"), -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modif(p, EntityAttributes.GENERIC_JUMP_STRENGTH, MOD_SAUT, a(p, "bond"), 0.08, EntityAttributeModifier.Operation.ADD_VALUE);
    }

    private static void modif(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id, boolean veut, double v, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance i = p.getAttributeInstance(attr);
        if (i == null) return;
        if (veut && !i.hasModifier(id)) i.addPersistentModifier(new EntityAttributeModifier(id, v, op));
        else if (!veut && i.hasModifier(id)) i.removeModifier(id);
    }

    // ───────────────────────── la bourse et les points ─────────────────────────

    private static ScoreboardObjective objectif(MinecraftServer s, String nom, String titre) {
        Scoreboard sb = s.getScoreboard();
        ScoreboardObjective o = sb.getNullableObjective(nom);
        if (o == null) o = sb.addObjective(nom, ScoreboardCriterion.DUMMY, Text.literal(titre), ScoreboardCriterion.RenderType.INTEGER, false, null);
        return o;
    }

    private static int lire(ServerPlayerEntity p, String nom, String titre) {
        var sc = p.getServer().getScoreboard().getScore(ScoreHolder.fromName(p.getNameForScoreboard()), objectif(p.getServer(), nom, titre));
        return sc == null ? 0 : sc.getScore();
    }

    private static void ecrire(ServerPlayerEntity p, String nom, String titre, int v) {
        p.getServer().getScoreboard().getOrCreateScore(ScoreHolder.fromName(p.getNameForScoreboard()), objectif(p.getServer(), nom, titre)).setScore(v);
    }

    public static int thalers(ServerPlayerEntity p) { return lire(p, "ombre_thalers", "Thalers"); }
    public static int points(ServerPlayerEntity p) { return lire(p, "ombre_points", "Points de compétence"); }

    /** Gagner (ou dépenser, si n < 0) des thalers ; « pourquoi » s'affiche, sauf s'il est null. */
    public static void ajouterThalers(ServerPlayerEntity p, int n, String pourquoi) {
        ecrire(p, "ombre_thalers", "Thalers", Math.max(0, thalers(p) + n));
        if (pourquoi != null) p.sendMessage(Text.literal((n >= 0 ? "§6+" : "§c") + n + " thalers §7— " + pourquoi), true);
        if (n > 0) p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 0.7f);
    }

    public static void ajouterPoints(ServerPlayerEntity p, int n, String pourquoi) {
        ecrire(p, "ombre_points", "Points de compétence", Math.max(0, points(p) + n));
        if (pourquoi != null && n > 0) {
            p.sendMessage(Text.literal("§b+" + n + " point" + (n > 1 ? "s" : "") + " de compétence §7— " + pourquoi + " §8(touche K)"), false);
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.6f);
        }
    }

    /** Une fois par seconde : le profil part vers chaque joueur (bourse, points, compétences) ; les corps s'effacent. */
    public static void tick(MinecraftServer s) {
        if (s.getTicks() % 20 != 0) return;
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            // La première fois : deux points pour commencer
            if (p.addCommandTag("ombre_profil")) ajouterPoints(p, 2, null);
            appliquer(p);
            envoyer(p, -1);
        }
    }

    /** Le profil vers le joueur ; « boutique » ≥ 0 ouvre l'écran de ce marchand. */
    public static void envoyer(ServerPlayerEntity p, int boutique) {
        if (!ServerPlayNetworking.canSend(p, ProfilPayload.ID)) return;
        StringBuilder sb = new StringBuilder();
        for (Competence c : COMPETENCES) if (a(p, c.id())) sb.append(c.id()).append(',');
        ServerPlayNetworking.send(p, new ProfilPayload(thalers(p), points(p), sb.toString(), boutique));
    }

    // ───────────────────────── les marchands ─────────────────────────

    /** Une marchandise : l'objet, combien, le prix en thalers. */
    public record Offre(Item objet, int nombre, int prix) {}

    public static final String BOUTIQUE = "ombre_boutique:";
    public static final String[] MARCHANDS = {"Armurier", "Apothicaire", "Fripier"};
    public static final List<List<Offre>> BOUTIQUES = List.of(
            List.of(new Offre(OmbreItems.COUTEAUX, 5, 30), new Offre(Objets.FLECHETTE_SOPORIFIQUE, 4, 40),
                    new Offre(Objets.FLECHETTE_EMPOISONNEE, 3, 60), new Offre(Objets.FLECHETTE_FOLIE, 2, 70),
                    new Offre(Objets.PETARD, 4, 25), new Offre(OmbreItems.BOMBE_FUMEE, 3, 45), new Offre(Objets.GRAPPIN, 1, 150)),
            List.of(new Offre(Items.POTION, 1, 35), new Offre(Objets.PAIN_EPICES, 3, 10), new Offre(Objets.FROMAGE, 2, 12),
                    new Offre(Objets.VIN, 1, 8), new Offre(Objets.FLECHETTE_SOPORIFIQUE, 4, 40), new Offre(Objets.FLECHETTE_EMPOISONNEE, 3, 60)),
            List.of(new Offre(Objets.TENUE, 1, 90), new Offre(Objets.THALERS, 10, 12), new Offre(Objets.PETARD, 4, 25),
                    new Offre(Objets.PAIN_SEIGLE, 3, 8), new Offre(Objets.BIERE, 2, 8)));

    public static int prix(ServerPlayerEntity p, Offre o) { return a(p, "marchand") ? (int) Math.ceil(o.prix() * 0.75) : o.prix(); }

    /** Le joueur achète la marchandise n° i du marchand n° b. */
    public static void acheter(ServerPlayerEntity p, int b, int i) {
        if (b < 0 || b >= BOUTIQUES.size() || i < 0 || i >= BOUTIQUES.get(b).size()) return;
        // Il faut être près d'un marchand de cette sorte
        boolean pres = !p.getServerWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(6),
                e -> e.getCommandTags().contains(BOUTIQUE + b)).isEmpty();
        if (!pres) return;
        Offre o = BOUTIQUES.get(b).get(i);
        int prix = prix(p, o);
        if (thalers(p) < prix) { p.sendMessage(Text.literal("§cPas assez de thalers (" + prix + ")."), true); return; }
        ajouterThalers(p, -prix, null);
        ItemStack st = new ItemStack(o.objet(), o.nombre());
        if (o.objet() == Items.POTION)
            st = net.minecraft.component.type.PotionContentsComponent.createStack(Items.POTION, net.minecraft.potion.Potions.HEALING);
        p.getInventory().offerOrDrop(st);
        p.sendMessage(Text.literal("§6Acheté : " + st.getName().getString() + (o.nombre() > 1 ? " ×" + o.nombre() : "") + " §7(−" + prix + " thalers)"), true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 0.6f, 1.1f);
        envoyer(p, -1);
    }

    // ───────────────────────── la fouille des corps ─────────────────────────

    /** Combien de temps (en ticks) le corps d'un garde reste à terre avant de s'effacer. */
    public static final int CORPS = 1200;
    public static final String FOUILLE = "ombre_fouille";

    /** Un clic droit sur le corps d'un garde : sa bourse et ce qu'il portait. */
    public static void fouiller(ServerPlayerEntity p, int id) {
        Entity e = p.getServerWorld().getEntityById(id);
        if (!(e instanceof LivingEntity corps) || corps.isAlive() || !Discretion.estGarde(corps) || corps.squaredDistanceTo(p) > 16) return;
        if (!corps.addCommandTag(FOUILLE)) { p.sendMessage(Text.literal("§7Déjà fouillé."), true); return; }
        boolean riche = corps.getCommandTags().contains(fr.fenua.ombre.Gardes.CIBLE) || corps instanceof fr.fenua.ombre.entite.Gardes.Capitaine;
        int fois = a(p, "fouille") ? 2 : 1;
        var r = corps.getRandom();
        int or = (riche ? 40 + r.nextInt(40) : 6 + r.nextInt(16)) * fois;
        List<String> pris = new ArrayList<>();
        Item[] butins = {OmbreItems.COUTEAUX, Objets.FLECHETTE_SOPORIFIQUE, Objets.PETARD, Objets.PAIN_SEIGLE, Objets.FLECHETTE_EMPOISONNEE, OmbreItems.BOMBE_FUMEE};
        for (int k = 0; k < fois; k++) {
            if (r.nextInt(3) == 0) continue;
            Item i = butins[r.nextInt(butins.length)];
            int n = 1 + r.nextInt(2);
            p.getInventory().offerOrDrop(new ItemStack(i, n));
            pris.add(new ItemStack(i).getName().getString() + (n > 1 ? " ×" + n : ""));
        }
        ajouterThalers(p, or, null);
        p.sendMessage(Text.literal("§6Fouillé : +" + or + " thalers" + (pris.isEmpty() ? "" : " §7· " + String.join(", ", pris))), true);
        p.getWorld().playSound(null, corps.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.8f, 0.9f);
        ((ServerWorld) corps.getWorld()).spawnParticles(ParticleTypes.WAX_ON, corps.getX(), corps.getY() + 0.3, corps.getZ(), 8, 0.4, 0.1, 0.4, 0);
        fr.fenua.ombre.ac.Quetes.evenement(p, "fouille", null);
        envoyer(p, -1);
    }

    // ───────────────────────── les messages ─────────────────────────

    /** Du serveur vers le joueur : la bourse, les points, les compétences (« id,id,… ») ; boutique ≥ 0 ouvre son écran. */
    public record ProfilPayload(int thalers, int points, String competences, int boutique) implements CustomPayload {
        public static final Id<ProfilPayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "profil"));
        public static final PacketCodec<RegistryByteBuf, ProfilPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ProfilPayload::thalers, PacketCodecs.VAR_INT, ProfilPayload::points,
                PacketCodecs.STRING, ProfilPayload::competences, PacketCodecs.VAR_INT, ProfilPayload::boutique, ProfilPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Du joueur vers le serveur : acheter (a = boutique, b = marchandise), apprendre (texte = compétence), fouiller (a = corps). */
    public record DemandePayload(int sorte, int a, int b, String texte) implements CustomPayload {
        public static final int ACHETER = 0, APPRENDRE = 1, FOUILLER = 2;
        public static final Id<DemandePayload> ID = new Id<>(Identifier.of(Ombre.MOD_ID, "demande"));
        public static final PacketCodec<RegistryByteBuf, DemandePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, DemandePayload::sorte, PacketCodecs.VAR_INT, DemandePayload::a,
                PacketCodecs.VAR_INT, DemandePayload::b, PacketCodecs.STRING, DemandePayload::texte, DemandePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void recevoir(ServerPlayerEntity p, DemandePayload d) {
        switch (d.sorte()) {
            case DemandePayload.ACHETER -> acheter(p, d.a(), d.b());
            case DemandePayload.APPRENDRE -> apprendre(p, d.texte());
            case DemandePayload.FOUILLER -> fouiller(p, d.a());
            default -> {}
        }
    }
}
