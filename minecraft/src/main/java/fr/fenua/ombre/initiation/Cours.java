package fr.fenua.ombre.initiation;

import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.ac.Detection;
import fr.fenua.ombre.ac.Notoriete;
import fr.fenua.ombre.objet.Effets;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Un cours : une suite de cours fermées par des grilles, et les étapes qui s'y jouent.
 * Il y en a deux : l'initiation (la Ruelle d'Or) et le cours de rappel (le Jardin royal).
 * Le cours que suit un joueur est noté par l'étiquette « ombre_cours:<nom> » ; son étape, dans le tableau
 * des scores ; l'emplacement des cours, dans les données du monde.
 */
public final class Cours {
    public static final List<Cours> TOUS = new ArrayList<>();
    private static final String ACTIF = "ombre_cours:";

    public final String id, nom, commande, titreFin, messageFin, accueil;
    public final int[] len, debuts;
    public final String[] noms;
    public final int longueur;
    public final List<Etape> etapes;
    /** La cour où la notoriété ne doit pas être remise à zéro (on y apprend à la faire baisser), ou -1. */
    public final int courNotoriete;

    public Cours(String id, String nom, String commande, int[] len, String[] noms, List<Etape> etapes, int courNotoriete,
                 String titreFin, String messageFin, String accueil) {
        this.id = id; this.nom = nom; this.commande = commande; this.len = len; this.noms = noms; this.etapes = etapes;
        this.courNotoriete = courNotoriete; this.titreFin = titreFin; this.messageFin = messageFin; this.accueil = accueil;
        debuts = new int[len.length];
        int x = 0;
        for (int i = 0; i < len.length; i++) { debuts[i] = x; x += len[i] + 1; }
        longueur = x - 1;
        TOUS.add(this);
    }

    // ───────────────────────── quel cours, quelle étape ─────────────────────────

    /** Le cours que suit ce joueur, ou null. */
    @Nullable
    public static Cours actif(ServerPlayerEntity p) {
        for (Cours c : TOUS) if (p.getCommandTags().contains(ACTIF + c.id)) return c;
        // Les joueurs d'avant les cours multiples : l'initiation, s'ils y avaient une étape en cours
        for (Cours c : TOUS) { int e = c.etape(p); if (e >= 0 && e < c.etapes.size()) return c; }
        return null;
    }

    private ScoreboardObjective objectif(Scoreboard sb) {
        String n = "ombre_" + id;
        ScoreboardObjective o = sb.getNullableObjective(n);
        if (o == null) o = sb.addObjective(n, ScoreboardCriterion.DUMMY, Text.literal(nom), ScoreboardCriterion.RenderType.INTEGER, true, null);
        return o;
    }

    public int etape(ServerPlayerEntity p) {
        Scoreboard sb = p.getServerWorld().getScoreboard();
        ScoreboardObjective o = sb.getNullableObjective("ombre_" + id);
        if (o == null) return -1;
        var s = sb.getScore(p, o);
        return s == null ? -1 : s.getScore();
    }

    private void fixer(ServerPlayerEntity p, int e) {
        Scoreboard sb = p.getServerWorld().getScoreboard();
        ScoreAccess s = sb.getOrCreateScore(p, objectif(sb));
        s.setScore(e);
    }

    /** Les lignes du panneau de leçon (« T » le titre, « I » les explications), ou null. */
    @Nullable
    public static List<String> lignes(ServerPlayerEntity p) {
        Cours c = actif(p);
        if (c == null) return null;
        int e = c.etape(p);
        if (e < 0 || e >= c.etapes.size() || c.origine(p.getServerWorld()) == null) return null;
        Etape s = c.etapes.get(e);
        List<String> l = new ArrayList<>();
        l.add("T" + c.nom + " " + (e + 1) + "/" + c.etapes.size() + " — " + s.titre);
        for (String x : s.lignes) l.add("I" + x);
        return l;
    }

    /** Le but de l'étape en cours (pour la boussole), ou null. */
    @Nullable
    public static BlockPos but(ServerPlayerEntity p) {
        Cours c = actif(p);
        if (c == null) return null;
        int e = c.etape(p);
        BlockPos o = c.origine(p.getServerWorld());
        if (e < 0 || e >= c.etapes.size() || o == null || c.etapes.get(e).but == null) return null;
        int[] b = c.etapes.get(e).but;
        return o.add(c.debuts[c.etapes.get(e).cour] + b[0], b[1], b[2]);
    }

    // ───────────────────────── la boucle ─────────────────────────

    private static final Map<UUID, Long> DEPUIS = new HashMap<>();

    /** Deux fois par seconde. */
    public static void tick(MinecraftServer serveur) {
        if (serveur.getTicks() % 10 != 0) return;
        ServerWorld monde = serveur.getOverworld();
        if (serveur.getTicks() % 40 == 0) {
            // Pas de monstres sauvages dans les cours (un monde plat fait naître des slimes partout)
            for (Cours c : TOUS) {
                BlockPos o = c.origine(monde);
                if (o == null || monde.getPlayers(p -> actif(p) == c).isEmpty()) continue;
                net.minecraft.util.math.Box b = new net.minecraft.util.math.Box(o.getX() - 3, o.getY() - 5, o.getZ() - 3,
                        o.getX() + c.longueur + 3, o.getY() + 30, o.getZ() + 18);
                for (MobEntity m : monde.getEntitiesByClass(MobEntity.class, b,
                        m -> m instanceof net.minecraft.entity.mob.Monster && !m.getCommandTags().contains(Initiation.TAG))) m.discard();
            }
        }
        for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
            Cours c = actif(p);
            if (c == null) continue;
            int e = c.etape(p);
            if (e < 0 || e >= c.etapes.size() || p.isDead() || p.isSpectator()) continue;
            BlockPos o = c.origine(p.getServerWorld());
            if (o == null) continue;
            Etape s = c.etapes.get(e);
            Ctx x = new Ctx(p, p.getServerWorld(), o, c, s.cour);
            long t = p.getServerWorld().getTime();
            if (t - DEPUIS.getOrDefault(p.getUuid(), 0L) < 20) continue;   // laisser l'étape s'installer
            // Loin de sa cour (téléporté ailleurs), on ne juge rien : ses gardes, hors de portée, sembleraient morts
            if (x.x() < -24 || x.x() > c.len[s.cour] + 24 || Math.abs(x.z() - 7) > 40 || Math.abs(x.y()) > 40) continue;
            if (s.ratee != null && s.ratee.test(x)) { c.echec(p, e, o); continue; }
            if (s.reussie.test(x)) c.avancer(p, e, o);
        }
    }

    private void avancer(ServerPlayerEntity p, int e, BlockPos o) {
        Etape s = etapes.get(e);
        ServerWorld w = p.getServerWorld();
        if (s.ouvre) ouvrir(w, o, s.cour);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.3f);
        int n = e + 1;
        fixer(p, n);
        if (n >= etapes.size()) {
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6" + titreFin)));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7L'Ombre est prête — /ombre prague")));
            p.addCommandTag("ombre_" + id + "_fini");
            p.removeCommandTag(ACTIF + id);
            w.playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
            p.sendMessage(Text.literal("§6Maître Vojtěch §7: §f" + messageFin), false);
            return;
        }
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 10));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§a✔ " + s.titre)));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Suite : " + etapes.get(n).titre)));
        commencer(p, n, o, false);
    }

    /** Repéré pendant une étape de discrétion : retour à l'entrée de la cour, tout recommence. */
    private void echec(ServerPlayerEntity p, int e, BlockPos o) {
        int cour = etapes.get(e).cour, premiere = e;
        while (premiere > 0 && etapes.get(premiere - 1).cour == cour) premiere--;
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 10));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§cRepéré !")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Retour à l'entrée de la cour — essaie encore")));
        p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1f, 1.4f);
        fixer(p, premiere);
        commencer(p, premiere, o, true);
    }

    /** Le joueur aborde l'étape n. Au début d'une cour : point de réapparition, cour remise à neuf. */
    void commencer(ServerPlayerEntity p, int n, BlockPos o, boolean ramener) {
        ServerWorld w = p.getServerWorld();
        Etape s = etapes.get(n);
        Ctx c = new Ctx(p, w, o, this, s.cour);
        boolean premiere = n == 0 || etapes.get(n - 1).cour != s.cour;
        if (premiere) {
            BlockPos entree = c.pos(1, 0, 7);
            p.setSpawnPoint(w.getRegistryKey(), entree, -90f, true, false);
            for (MobEntity m : c.vivants(Initiation.TAG)) m.discard();
            if (s.cour != courNotoriete && Notoriete.get(p) > 0) Notoriete.ajouter(p, -100, null);
            p.removeStatusEffect(Effets.DEGUISE);
        }
        if (ramener) {
            BlockPos entree = c.pos(1, 0, 7);
            p.teleport(w, entree.getX() + 0.5, entree.getY(), entree.getZ() + 0.5, -90f, 0f);
            p.fallDistance = 0;
        }
        s.debut.accept(c);
        for (MobEntity m : c.vivants(Initiation.TAG)) { Detection.fixer(m, 0f); m.removeCommandTag(Gardes.ALERTE); }
        DEPUIS.put(p.getUuid(), w.getTime());
        if (premiere) p.sendMessage(Text.literal("§6━━ " + noms[s.cour] + " ━━"), false);
        p.sendMessage(Text.literal("§e" + (n + 1) + ". " + s.titre + " §7— les explications sont en haut à gauche"), false);
    }

    /** La grille au bout d'une cour s'ouvre. */
    void ouvrir(ServerWorld w, BlockPos o, int cour) {
        int x = debuts[cour] + len[cour];
        for (int z = 6; z <= 8; z++)
            for (int y = 0; y <= 3; y++) {
                BlockPos b = o.add(x, y, z);
                if (w.getBlockState(b).isOf(Blocks.IRON_BARS)) w.setBlockState(b, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        BlockPos m = o.add(x, 1, 7);
        w.playSound(null, m, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.2f, 0.7f);
        w.playSound(null, m, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1f, 0.8f);
        w.spawnParticles(ParticleTypes.CLOUD, m.getX() + 0.5, m.getY() + 1, m.getZ() + 0.5, 20, 0.3, 1, 1, 0.02);
    }

    /** « /ombre <cours> <n> » : aller directement à l'étape n (de 1 à N). */
    public int aller(ServerPlayerEntity p, int n) {
        BlockPos o = origine(p.getServerWorld());
        if (o == null) {
            p.sendMessage(Text.literal("§cConstruis d'abord ce cours : " + commande), false);
            return 0;
        }
        activer(p);
        int e = Math.max(0, Math.min(etapes.size() - 1, n - 1));
        int premiere = e;
        while (premiere > 0 && etapes.get(premiere - 1).cour == etapes.get(e).cour) premiere--;
        for (int k = 0; k < etapes.get(e).cour; k++) ouvrir(p.getServerWorld(), o, k);   // les grilles d'avant
        fixer(p, premiere);
        commencer(p, premiere, o, true);
        return 1;
    }

    private void activer(ServerPlayerEntity p) {
        for (Cours c : TOUS) p.removeCommandTag(ACTIF + c.id);
        p.addCommandTag(ACTIF + id);
    }

    /** Démarrer ce cours depuis le début (après la construction). */
    public void demarrer(ServerPlayerEntity p, BlockPos o) {
        for (String t : new ArrayList<>(p.getCommandTags()))
            if (t.startsWith(Gardes.VUE_SYNCHRO)) p.removeCommandTag(t);
        p.removeCommandTag("ombre_" + id + "_fini");
        activer(p);
        fixer(p, 0);
        commencer(p, 0, o, true);
        p.sendMessage(Text.literal("§6Maître Vojtěch §7: §f" + accueil), false);
    }

    public int nombre() { return etapes.size(); }

    // ───────────────────────── l'emplacement, gardé avec le monde ─────────────────────────

    public static final class Emplacement extends PersistentState {
        @Nullable BlockPos origine;
        static final Type<Emplacement> TYPE = new Type<>(Emplacement::new, Emplacement::lire, null);

        static Emplacement lire(NbtCompound nbt, RegistryWrapper.WrapperLookup r) {
            Emplacement e = new Emplacement();
            if (nbt.contains("origine")) e.origine = BlockPos.fromLong(nbt.getLong("origine"));
            return e;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup r) {
            if (origine != null) nbt.putLong("origine", origine.asLong());
            return nbt;
        }
    }

    private Emplacement emplacement(ServerWorld w) {
        return w.getServer().getOverworld().getPersistentStateManager().getOrCreate(Emplacement.TYPE, "ombre_" + id);
    }

    @Nullable
    public BlockPos origine(ServerWorld w) {
        if (w != w.getServer().getOverworld()) return null;
        return emplacement(w).origine;
    }

    void retenir(ServerWorld w, BlockPos o) {
        Emplacement e = emplacement(w);
        e.origine = o;
        e.markDirty();
    }
}
