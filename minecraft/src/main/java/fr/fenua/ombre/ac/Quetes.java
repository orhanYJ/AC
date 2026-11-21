package fr.fenua.ombre.ac;

import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.entite.Entites;
import fr.fenua.ombre.entite.Figure;
import fr.fenua.ombre.entite.Ordres;
import fr.fenua.ombre.entite.Passant;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les quêtes, comme dans Assassin's Creed : une histoire principale en sept mémoires qui se suivent, et des quêtes
 * secondaires (contrats d'assassinat, lettre pressée, marchand rançonné, bourses du marché) qu'on prend auprès
 * des habitants marqués d'un « ! ». Chaque mission rapporte des thalers et des points de compétence.
 *
 * L'histoire est inventée ; Tycho Brahe, Johannes Kepler, Rodolphe II et le rabbin Loew y paraissent avec respect,
 * les cibles (la Confrérie du Corbeau) sont des personnages imaginaires.
 *
 * Où en est le joueur : des étiquettes sur lui — « ombre_h:<mémoire> » et « ombre_e:<étape> » pour l'histoire,
 * « ombre_sq:<quête>:<étape> » pour la quête secondaire en cours, « ombre_sqf:<quête> » pour celles finies,
 * « ombre_n:<compte> » pour un compte en cours (gardes tués, bourses volées).
 * Les lieux sont des repères posés par la carte (marqueurs « ombre_q_lieu » nommés).
 */
public final class Quetes {
    private Quetes() {}

    // ───────────────────────── les définitions ─────────────────────────

    public enum Sorte { PARLER, ALLER, TUER, VOLER, SYNCHRO, FOUILLER, SUIVRE, LIVRER }

    /**
     * Une étape. « cle » : qui ou quoi (un personnage, un lieu, une étiquette de cible) ; « n » : combien (tuer deux
     * gardes, voler cinq bourses) ou la durée en secondes (livrer) ; « apparait » : ce qui entre en scène au début
     * de l'étape (« pnj:id:nom@lieu », « cible:id:nom@lieu », « garde:id@lieu », « route:id:lieu1,lieu2,… ») ;
     * « recit » : ce qu'on entend en la réussissant.
     */
    public record Etape(Sorte sorte, String texte, String cle, int n, String[] apparait, String[] recit) {}

    public record Quete(String id, String titre, List<Etape> etapes, int thalers, int points, String donneur, String[] accueil) {}

    private static Etape e(Sorte s, String texte, String cle, String... recit) { return new Etape(s, texte, cle, 1, new String[0], recit); }
    private static Etape e(Sorte s, String texte, String cle, int n, String[] apparait, String... recit) { return new Etape(s, texte, cle, n, apparait, recit); }
    private static String[] ap(String... a) { return a; }

    /** L'histoire principale : sept mémoires. */
    public static final List<Quete> HISTOIRE = List.of(
            new Quete("arrivee", "Mémoire 1 — L'arrivée", List.of(
                    e(Sorte.PARLER, "Parle à Tycho Brahe, sur la grande place", "tycho",
                            "§6Tycho Brahe §7: §fAinsi, c'est toi l'Ombre dont Kepler m'a parlé. Trente ans de nuits passées à mesurer le ciel tiennent dans mes registres.",
                            "§6Tycho Brahe §7: §fOn rôde autour de ma maison. Va voir Kepler : il a vu des hommes en noir marqués d'un corbeau."),
                    e(Sorte.PARLER, "Parle à Johannes Kepler", "kepler",
                            "§6Johannes Kepler §7: §fLa Confrérie du Corbeau veut voler les registres de Tycho et les vendre au plus offrant.",
                            "§6Johannes Kepler §7: §fMonte d'abord à l'horloge astronomique : de là-haut, toute la ville s'ouvre à toi.")),
                    30, 1, null, null),
            new Quete("yeux", "Mémoire 2 — Les yeux de la ville", List.of(
                    e(Sorte.SYNCHRO, "Synchronise-toi au sommet de l'horloge astronomique", "Horloge astronomique",
                            "§7La ville entière se dévoile. Au nord, dans la rue du quartier juif, le rabbin Loew sait beaucoup de choses."),
                    e(Sorte.PARLER, "Rends visite au rabbin Loew, au nord de la ville", "loew",
                            "§6Rabbi Judah Loew §7: §fUn messager du Corbeau passe chaque jour de la grande place à la taverne du nord.",
                            "§6Rabbi Judah Loew §7: §fSuis-le sans qu'il te voie. Ce qu'il porte vaut mieux que ce qu'il dit.")),
                    40, 1, null, null),
            new Quete("messager", "Mémoire 3 — Le messager", List.of(
                    e(Sorte.SUIVRE, "Suis le messager jusqu'à la taverne, sans le perdre (moins de 24 blocs)", "messager", 1,
                            ap("pnj:messager:Messager du Corbeau@messager_1", "route:messager:messager_1,messager_2,messager_3"),
                            "§7Le messager s'arrête devant la taverne et attend quelqu'un."),
                    e(Sorte.VOLER, "Vole la lettre du messager (accroupi, dans son dos, clic droit)", "messager",
                            "§7La lettre dit : §f« Les registres partiront par l'entrepôt du port. Vrána garde la clé. — O. »")),
                    50, 2, null, null),
            new Quete("entrepot", "Mémoire 4 — L'entrepôt du port", List.of(
                    e(Sorte.TUER, "Élimine Jiří Vrána, l'intendant du Corbeau, aux entrepôts du port", "vrana", 1,
                            ap("cible:vrana:Jiří Vrána, intendant du Corbeau@entrepot", "garde:entrepot_g@entrepot_g1", "garde:entrepot_g@entrepot_g2"),
                            "§7Vrána s'effondre parmi les caisses."),
                    e(Sorte.FOUILLER, "Fouille le corps de Vrána : il porte la clé", "vrana",
                            "§7Avec la clé, un billet : §f« Kragl attend les registres dans la tour nord de Týn. »")),
                    60, 2, null, null),
            new Quete("alchimiste", "Mémoire 5 — L'alchimiste", List.of(
                    e(Sorte.TUER, "Élimine Melchior Kragl, l'alchimiste, dans la tour nord de Týn", "ombre_cible_kragl",
                            "§7Sur la table de Kragl, une lettre scellée du grand maître : §fOttokar von Rabenstein§7, conseiller à la cour.")),
                    80, 2, null, null),
            new Quete("empereur", "Mémoire 6 — L'empereur", List.of(
                    e(Sorte.PARLER, "Préviens l'empereur Rodolphe II, sur l'autre rive", "rodolphe",
                            "§6Rodolphe II §7: §fRabenstein ? Il avait ma confiance… Il se terre au sommet de la tour du pont.",
                            "§6Rodolphe II §7: §fQue les registres de Tycho restent à Prague : la science de cette ville en dépend.")),
                    40, 1, null, null),
            new Quete("maitre", "Mémoire 7 — Le grand maître", List.of(
                    e(Sorte.TUER, "Élimine Ottokar von Rabenstein, au sommet de la tour du pont", "ombre_cible_ottokar",
                            "§7Les registres de Tycho sont là, dans un coffre de chêne. Il faut les rendre."),
                    e(Sorte.PARLER, "Rapporte les registres à Johannes Kepler", "kepler",
                            "§6Johannes Kepler §7: §fTu as sauvé bien plus que des cahiers. Avec ces mesures, je trouverai la vraie forme des orbites.")),
                    150, 3, null, null));

    /** Les quêtes secondaires : on les prend auprès d'un habitant marqué d'un « ! ». */
    public static final List<Quete> SECONDAIRES = List.of(
            new Quete("hruby", "Contrat : le sergent Hrubý", List.of(
                    e(Sorte.TUER, "Élimine le sergent Hrubý, qui rançonne la rue des orfèvres", "hruby", 1,
                            ap("cible:hruby:Sergent Hrubý@contrat1", "garde:hruby_g@contrat1"),
                            "§7Le sergent Hrubý ne rançonnera plus personne.")),
                    60, 1, "marta", new String[]{"§6Veuve Marta §7: §fLe sergent Hrubý a pris tout ce que mon mari m'avait laissé. Fais qu'il ne prenne plus rien à personne."}),
            new Quete("lettre", "La lettre pressée", List.of(
                    e(Sorte.LIVRER, "Porte la lettre de Mathias à la tour du pont, en moins de 90 secondes", "livraison", 90, new String[0],
                            "§7Le portier prend la lettre et te glisse une pièce.")),
                    35, 1, "mathias", new String[]{"§6Mathias l'imprimeur §7: §fCette lettre doit être à la tour du pont avant que la cloche ne sonne. Cours !"}),
            new Quete("rancon", "Le marchand rançonné", List.of(
                    e(Sorte.TUER, "Chasse les deux gardes qui rançonnent Bohuslav", "racket", 2,
                            ap("garde:racket@donneur3", "garde:racket@donneur3"),
                            "§6Bohuslav §7: §fQue Dieu te garde ! Prends ceci, c'est bien peu.")),
                    45, 1, "bohuslav", new String[]{"§6Bohuslav le marchand §7: §fDeux gardes viennent chaque jour me prendre ma recette. Les voilà qui reviennent…"}),
            new Quete("collecteur", "Contrat : le collecteur", List.of(
                    e(Sorte.TUER, "Élimine Šťastný, le collecteur d'impôts du Corbeau, qui fait sa ronde", "collecteur", 1,
                            ap("cible:collecteur:Šťastný le collecteur@contrat2", "ronde:collecteur:contrat2,contrat3"),
                            "§7Le collecteur ne reverra pas la Confrérie.")),
                    70, 1, "ludmila", new String[]{"§6Ludmila l'aubergiste §7: §fLe collecteur du Corbeau passe chaque jour par ici. Les gardes le protègent mal quand il tourne le coin."}),
            new Quete("bourses", "Les bourses du marché", List.of(
                    e(Sorte.VOLER, "Vole cinq bourses aux passants (accroupi, dans leur dos)", "", 5, new String[0],
                            "§6Pepík §7: §fCinq ! Tu es plus fort que moi. Garde le tout, et ceci en plus.")),
                    30, 1, "pepik", new String[]{"§6Pepík le gamin §7: §fJe parie que tu ne voles pas cinq bourses sans te faire prendre !"}));

    // ───────────────────────── l'état du joueur ─────────────────────────

    private static int nombre(ServerPlayerEntity p, String prefixe, int defaut) {
        for (String t : p.getCommandTags()) if (t.startsWith(prefixe)) {
            try { return Integer.parseInt(t.substring(prefixe.length())); } catch (NumberFormatException ignore) {}
        }
        return defaut;
    }

    private static void fixer(ServerPlayerEntity p, String prefixe, int v) {
        p.getCommandTags().removeIf(t -> t.startsWith(prefixe));
        if (v >= 0) p.addCommandTag(prefixe + v);
    }

    public static int memoire(ServerPlayerEntity p) { return nombre(p, "ombre_h:", -1); }
    private static int etape(ServerPlayerEntity p) { return nombre(p, "ombre_e:", 0); }
    private static int compte(ServerPlayerEntity p) { return nombre(p, "ombre_n:", 0); }

    /** La quête secondaire en cours : { index, étape }, ou null. */
    private static int[] secondaire(ServerPlayerEntity p) {
        for (String t : p.getCommandTags()) if (t.startsWith("ombre_sq:")) {
            String[] x = t.split(":");
            for (int i = 0; i < SECONDAIRES.size(); i++) if (SECONDAIRES.get(i).id().equals(x[1])) return new int[]{i, Integer.parseInt(x[2])};
        }
        return null;
    }

    private static void fixerSecondaire(ServerPlayerEntity p, int i, int etape) {
        p.getCommandTags().removeIf(t -> t.startsWith("ombre_sq:"));
        if (i >= 0) p.addCommandTag("ombre_sq:" + SECONDAIRES.get(i).id() + ":" + etape);
    }

    /** /ombre prague : l'histoire recommence à la première mémoire. */
    public static void commencer(ServerPlayerEntity p) {
        p.getCommandTags().removeIf(t -> t.startsWith("ombre_h:") || t.startsWith("ombre_e:") || t.startsWith("ombre_sq") || t.startsWith("ombre_n:") || t.startsWith("ombre_livre:"));
        fixer(p, "ombre_h:", 0);
        fixer(p, "ombre_e:", 0);
        annoncer(p, HISTOIRE.get(0));
        debutEtape(p, HISTOIRE.get(0).etapes().get(0));
    }

    private static void annoncer(ServerPlayerEntity p, Quete q) {
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6" + q.titre().replaceFirst(" — .*", ""))));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§f" + q.titre().replaceFirst(".* — ", ""))));
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.6f, 0.8f);
    }

    // ───────────────────────── les événements ─────────────────────────

    /** Quelque chose s'est passé : « parle » (un personnage), « tue », « vol », « synchro » (nom du lieu), « fouille ». */
    public static void evenement(ServerPlayerEntity p, String quoi, Object detail) {
        int h = memoire(p);
        if (h >= 0 && h < HISTOIRE.size()) {
            List<Etape> et = HISTOIRE.get(h).etapes();
            int k = etape(p);
            if (k < et.size() && correspond(p, et.get(k), quoi, detail)) avancer(p, -1);
        }
        int[] s = secondaire(p);
        if (s != null) {
            Etape et = SECONDAIRES.get(s[0]).etapes().get(s[1]);
            if (correspond(p, et, quoi, detail)) avancer(p, s[0]);
        }
        // Prendre une quête secondaire auprès d'un habitant
        if (quoi.equals("parle") && detail instanceof Passant pa && s == null) {
            for (int i = 0; i < SECONDAIRES.size(); i++) {
                Quete q = SECONDAIRES.get(i);
                if (!pa.getCommandTags().contains("ombre_pnj." + q.donneur())) continue;
                if (p.getCommandTags().contains("ombre_sqf:" + q.id())) { p.sendMessage(Text.literal("§7Merci encore pour ton aide."), false); return; }
                for (String l : q.accueil()) p.sendMessage(Text.literal(l), false);
                p.sendMessage(Text.literal("§e⚑ Quête secondaire : " + q.titre()), false);
                fixer(p, "ombre_n:", 0);
                fixerSecondaire(p, i, 0);
                debutEtape(p, q.etapes().get(0));
                return;
            }
        }
    }

    /** L'événement remplit-il l'étape ? (Pour celles qui se comptent, on compte.) */
    private static boolean correspond(ServerPlayerEntity p, Etape e, String quoi, Object detail) {
        switch (e.sorte()) {
            case PARLER -> {
                if (!quoi.equals("parle")) return false;
                if (detail instanceof Figure f) return e.cle().equals(new String[]{"tycho", "kepler", "rodolphe", "loew"}[f.qui()]);
                return detail instanceof Entity en && en.getCommandTags().contains("ombre_pnj." + e.cle());
            }
            case TUER -> {
                if (!quoi.equals("tue") || !(detail instanceof Entity en)) return false;
                boolean bon = en.getCommandTags().contains(e.cle()) || en.getCommandTags().contains("ombre_q." + e.cle());
                if (!bon) return false;
                return compter(p, e.n());
            }
            case VOLER -> {
                if (!quoi.equals("vol")) return false;
                if (!e.cle().isEmpty() && !(detail instanceof Entity en && en.getCommandTags().contains("ombre_pnj." + e.cle()))) return false;
                return compter(p, e.n());
            }
            case SYNCHRO -> { return quoi.equals("synchro") && (e.cle().isEmpty() || e.cle().equals(detail)); }
            case FOUILLER -> {
                return quoi.equals("fouille") && (e.cle().isEmpty() || detail instanceof Entity en && en.getCommandTags().contains("ombre_q." + e.cle()));
            }
            default -> { return false; }
        }
    }

    private static boolean compter(ServerPlayerEntity p, int n) {
        int c = compte(p) + 1;
        fixer(p, "ombre_n:", c);
        if (c < n) { p.sendMessage(Text.literal("§e" + c + " / " + n), true); return false; }
        return true;
    }

    /** L'étape est réussie : son récit, puis l'étape suivante (ou la récompense). « sq » ≥ 0 : la quête secondaire n° sq. */
    private static void avancer(ServerPlayerEntity p, int sq) {
        Quete q;
        int k;
        if (sq < 0) { q = HISTOIRE.get(memoire(p)); k = etape(p); }
        else { q = SECONDAIRES.get(sq); k = secondaire(p)[1]; }
        Etape fait = q.etapes().get(k);
        for (String l : fait.recit()) p.sendMessage(Text.literal(l), false);
        p.sendMessage(Text.literal("§a✔ " + fait.texte()), true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.4f);
        fixer(p, "ombre_n:", 0);
        p.getCommandTags().removeIf(t -> t.startsWith("ombre_livre:"));
        if (k + 1 < q.etapes().size()) {
            if (sq < 0) fixer(p, "ombre_e:", k + 1); else fixerSecondaire(p, sq, k + 1);
            debutEtape(p, q.etapes().get(k + 1));
            return;
        }
        // La quête est finie : la récompense
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(sq < 0 ? "§6Mémoire accomplie" : "§eQuête accomplie")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§f" + q.titre())));
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1f);
        Profil.ajouterThalers(p, q.thalers(), q.titre());
        Profil.ajouterPoints(p, q.points(), q.titre());
        if (sq >= 0) {
            fixerSecondaire(p, -1, 0);
            p.addCommandTag("ombre_sqf:" + q.id());
            return;
        }
        int h = memoire(p) + 1;
        fixer(p, "ombre_h:", h);
        fixer(p, "ombre_e:", 0);
        if (h < HISTOIRE.size()) {
            annoncer(p, HISTOIRE.get(h));
            debutEtape(p, HISTOIRE.get(h).etapes().get(0));
        } else epilogue(p);
    }

    /** L'étape commence : ce qui doit entrer en scène entre en scène ; une livraison lance son compte à rebours. */
    private static void debutEtape(ServerPlayerEntity p, Etape e) {
        p.sendMessage(Text.literal("§6◆ §f" + e.texte()), false);
        ServerWorld w = p.getServerWorld();
        for (String a : e.apparait()) apparaitre(w, p, a);
        if (e.sorte() == Sorte.LIVRER) fixer(p, "ombre_livre:", (int) (w.getTime() / 20) + e.n());
    }

    private static void epilogue(ServerPlayerEntity p) {
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6La Confrérie du Corbeau est tombée")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Prague, 1600")));
        String[] l = {
                "§6— Épilogue —",
                "§7Tycho Brahe meurt à Prague en octobre 1601. Johannes Kepler hérite de ses registres d'observations.",
                "§7Grâce à eux, Kepler montre en 1609 que les planètes tournent autour du Soleil en suivant des ellipses.",
                "§7Rodolphe II règne à Prague jusqu'en 1611 ; sa cour fait de la ville un carrefour des arts et des sciences.",
                "§7La Confrérie du Corbeau, elle, n'a jamais existé : c'est ton histoire. Les quêtes secondaires t'attendent encore (« ! »)."};
        for (String s : l) p.sendMessage(Text.literal(s), false);
    }

    // ───────────────────────── les lieux et ceux qui entrent en scène ─────────────────────────

    public static final String LIEU = "ombre_q_lieu";
    private static final Map<ServerWorld, Map<String, Vec3d>> LIEUX = new HashMap<>();
    private static long releve = -1000;

    /** Les repères de quête du monde (relus toutes les dix secondes). */
    public static Vec3d lieu(ServerWorld w, String id) {
        Map<String, Vec3d> m = LIEUX.get(w);
        if (m == null || w.getTime() - releve > 200) {
            m = new HashMap<>();
            for (Entity e : w.iterateEntities())
                if (e.getType() == EntityType.MARKER && e.getCommandTags().contains(LIEU) && e.getCustomName() != null)
                    m.put(e.getCustomName().getString(), e.getPos());
            LIEUX.put(w, m);
            releve = w.getTime();
        }
        return m.get(id);
    }

    /** « pnj:id:nom@lieu », « cible:id:nom@lieu », « garde:id@lieu », « route:id:lieu,lieu » ou « ronde:id:lieu,lieu ». */
    private static void apparaitre(ServerWorld w, ServerPlayerEntity p, String a) {
        String[] x = a.split("@");
        String[] t = x[0].split(":");
        if (t[0].equals("route") || t[0].equals("ronde")) {
            List<Vec3d> pts = new ArrayList<>();
            for (String l : t[2].split(",")) { Vec3d v = lieu(w, l); if (v != null) pts.add(v); }
            for (Entity en : w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(300), en -> en.getCommandTags().contains("ombre_q." + t[1]) || en.getCommandTags().contains("ombre_pnj." + t[1]))) {
                en.getCommandTags().removeIf(s -> s.startsWith(Ordres.RONDE) || s.startsWith(Ordres.POSTE));
                Ordres.ronde((MobEntity) en, pts);
                if (en instanceof PathAwareEntity pa) pa.setPositionTarget(BlockPos.ORIGIN, -1);
            }
            return;
        }
        Vec3d o = x.length > 1 ? lieu(w, x[1]) : null;
        if (o == null) { p.sendMessage(Text.literal("§8(Lieu « " + (x.length > 1 ? x[1] : "?") + " » introuvable : refais /ombre prague.)"), false); return; }
        String id = t[1];
        // Déjà là (vivant) ? On ne le double pas — sauf les gardes, qu'on compte
        if (!t[0].equals("garde") && !w.getEntitiesByClass(LivingEntity.class, new Box(o, o).expand(200),
                en -> en.isAlive() && (en.getCommandTags().contains("ombre_q." + id) || en.getCommandTags().contains("ombre_pnj." + id))).isEmpty()) return;
        Vec3d pos = placeLibre(w, o);
        switch (t[0]) {
            case "pnj" -> {
                Passant pa = Entites.PASSANT.create(w);
                if (pa == null) return;
                pa.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                pa.initialize(w, w.getLocalDifficulty(BlockPos.ofFloored(pos)), SpawnReason.COMMAND, null);
                pa.setPersistent();
                pa.addCommandTag("ombre_pnj." + id);
                pa.setCustomName(Text.literal(t.length > 2 ? t[2] : id));
                pa.setCustomNameVisible(true);
                w.spawnEntity(pa);
            }
            case "cible", "garde" -> {
                boolean cible = t[0].equals("cible");
                MobEntity g = cible ? Entites.CAPITAINE.create(w) : Entites.HALLEBARDIER.create(w);
                if (g == null) return;
                g.refreshPositionAndAngles(pos.x, pos.y, pos.z, w.random.nextFloat() * 360, 0);
                g.initialize(w, w.getLocalDifficulty(BlockPos.ofFloored(pos)), SpawnReason.COMMAND, null);
                g.setPersistent();
                g.addCommandTag(Gardes.GARDE);
                g.addCommandTag("ombre_q." + id);
                if (cible) g.addCommandTag(Gardes.CIBLE);
                g.setCustomName(Text.literal(cible && t.length > 2 ? t[2] : "Hallebardier"));
                Ordres.poste(g, pos, w.random.nextFloat() * 360 - 180);
                w.spawnEntity(g);
                Team equipe = w.getScoreboard().getTeam(cible ? "ombre_cibles" : "ombre_gardes");
                if (equipe != null) w.getScoreboard().addScoreHolderToTeam(g.getNameForScoreboard(), equipe);
            }
            default -> {}
        }
    }

    /** Une place libre pour un corps d'homme, près du point voulu (pas dans une charrette ni une pile de caisses). */
    private static Vec3d placeLibre(ServerWorld w, Vec3d o) {
        for (int r = 0; r <= 4; r++)
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                Vec3d v = o.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Box b = new Box(v.x - 0.35, v.y, v.z - 0.35, v.x + 0.35, v.y + 1.9, v.z + 0.35);
                if (w.isSpaceEmpty(b) && !w.isSpaceEmpty(b.offset(0, -0.5, 0))) return v;
                if (r == 0) break;
            }
        return o;
    }

    // ───────────────────────── chaque seconde ─────────────────────────

    public static void tick(MinecraftServer s) {
        if (s.getTicks() % 20 != 10) return;
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            ServerWorld w = p.getServerWorld();
            Etape e = courante(p);
            int[] sq = secondaire(p);
            Etape es = sq == null ? null : SECONDAIRES.get(sq[0]).etapes().get(sq[1]);
            for (Etape et : new Etape[]{e, es}) {
                if (et == null) continue;
                boolean principale = et == e;
                switch (et.sorte()) {
                    case SUIVRE -> suivre(p, w, et, principale ? -1 : sq[0]);
                    case LIVRER -> livrer(p, w, et, principale ? -1 : sq[0]);
                    case SYNCHRO -> {
                        // Déjà synchronisé à ce point de vue avant l'étape : elle est faite
                        for (String t : p.getCommandTags())
                            if (t.startsWith(Gardes.VUE_SYNCHRO) && t.endsWith(":" + et.cle())) { avancer(p, principale ? -1 : sq[0]); break; }
                    }
                    case ALLER -> {
                        Vec3d l = lieu(w, et.cle());
                        if (l != null && l.squaredDistanceTo(p.getPos()) < 16) avancer(p, principale ? -1 : sq[0]);
                    }
                    default -> {}
                }
            }
        }
    }

    private static Etape courante(ServerPlayerEntity p) {
        int h = memoire(p);
        if (h < 0 || h >= HISTOIRE.size()) return null;
        List<Etape> et = HISTOIRE.get(h).etapes();
        int k = etape(p);
        return k < et.size() ? et.get(k) : null;
    }

    /** Filer quelqu'un : rester à moins de 24 blocs jusqu'à ce qu'il arrive ; trop loin, on l'a perdu et il repart. */
    private static void suivre(ServerPlayerEntity p, ServerWorld w, Etape e, int sq) {
        MobEntity cible = w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(200), en -> en.isAlive() && en.getCommandTags().contains("ombre_pnj." + e.cle()))
                .stream().findFirst().orElse(null);
        if (cible == null) { for (String a : e.apparait()) apparaitre(w, p, a); return; }
        Ordres.Ordre o = Ordres.lire(cible);
        if (o == null || o.points().isEmpty()) return;
        Vec3d fin = o.points().get(o.points().size() - 1);
        if (cible.squaredDistanceTo(fin) < 6) {
            // Arrivé : il s'arrête là et attend (on peut lui voler ce qu'il porte)
            cible.getCommandTags().removeIf(t -> t.startsWith(Ordres.RONDE));
            Ordres.poste(cible, cible.getPos(), cible.getYaw());
            avancer(p, sq);
            return;
        }
        if (cible.squaredDistanceTo(p) > 24 * 24) {
            p.sendMessage(Text.literal("§cTu l'as perdu de vue. Il reprend son chemin depuis le début."), true);
            Vec3d d = o.points().get(0);
            cible.refreshPositionAndAngles(d.x, d.y, d.z, 0, 0);
            cible.getNavigation().stop();
        }
    }

    /** Une livraison : arriver au lieu avant la fin du compte à rebours. */
    private static void livrer(ServerPlayerEntity p, ServerWorld w, Etape e, int sq) {
        int limite = nombre(p, "ombre_livre:", -1);
        if (limite < 0) { fixer(p, "ombre_livre:", (int) (w.getTime() / 20) + e.n()); return; }
        int reste = limite - (int) (w.getTime() / 20);
        Vec3d l = lieu(w, e.cle());
        if (l != null && l.squaredDistanceTo(p.getPos()) < 16) { avancer(p, sq); return; }
        if (reste <= 0) {
            p.sendMessage(Text.literal("§cTrop tard : la lettre n'est pas arrivée à temps. Retourne voir celui qui te l'a confiée."), false);
            if (sq >= 0) fixerSecondaire(p, -1, 0);
            p.getCommandTags().removeIf(t -> t.startsWith("ombre_livre:"));
        } else if (reste <= 10 || reste % 10 == 0) p.sendMessage(Text.literal("§e⌛ " + reste + " s"), true);
    }

    // ───────────────────────── le journal et la boussole ─────────────────────────

    /** Les lignes du journal : « H » un titre, « 1 »/« 0 » un objectif fait ou non. */
    public static List<String> lignes(ServerPlayerEntity p) {
        List<String> l = new ArrayList<>();
        int h = memoire(p);
        if (h >= 0 && h < HISTOIRE.size()) {
            Quete q = HISTOIRE.get(h);
            l.add("H" + q.titre());
            int k = etape(p);
            for (int i = 0; i < q.etapes().size(); i++) l.add((i < k ? "1" : "0") + q.etapes().get(i).texte() + (i == k ? suffixe(p, q.etapes().get(i)) : ""));
        } else if (h >= HISTOIRE.size()) l.add("H§aL'histoire est accomplie");
        int[] sq = secondaire(p);
        if (sq != null) {
            Quete q = SECONDAIRES.get(sq[0]);
            l.add("S" + q.titre());
            Etape e = q.etapes().get(sq[1]);
            l.add("0" + e.texte() + suffixe(p, e));
        }
        return l;
    }

    /** Le journal complet : les quêtes, puis les défis facultatifs (plumes, course des toits). */
    public static List<String> lignesEtFacultatifs(ServerPlayerEntity p) {
        List<String> l = lignes(p);
        int n = Defis.plumes(p, true);
        l.add((n >= Defis.PLUMES ? "1" : "0") + "Facultatif : plumes de corbeau (" + n + "/" + Defis.PLUMES + ")");
        l.add((p.getCommandTags().contains("ombre_course_finie") ? "1" : "0") + "Facultatif : la course des toits");
        return l;
    }

    private static String suffixe(ServerPlayerEntity p, Etape e) {
        if (e.n() > 1 && (e.sorte() == Sorte.TUER || e.sorte() == Sorte.VOLER)) return " (" + compte(p) + "/" + e.n() + ")";
        if (e.sorte() == Sorte.LIVRER) {
            int limite = nombre(p, "ombre_livre:", -1);
            if (limite >= 0) return " — " + Math.max(0, limite - (int) (p.getServerWorld().getTime() / 20)) + " s";
        }
        return "";
    }

    /** Les losanges de la boussole et de la mini-carte : où mène l'étape en cours (les « ! » des quêtes secondaires se voient sur la mini-carte). */
    public static List<Long> marqueurs(ServerPlayerEntity p) {
        List<Long> m = new ArrayList<>();
        ServerWorld w = p.getServerWorld();
        Etape e = courante(p);
        int[] sq = secondaire(p);
        for (Etape et : new Etape[]{e, sq == null ? null : SECONDAIRES.get(sq[0]).etapes().get(sq[1])}) {
            if (et == null) continue;
            Vec3d v = ou(p, w, et);
            if (v != null) m.add(BlockPos.ofFloored(v).asLong());
        }
        return m;
    }

    private static Vec3d ou(ServerPlayerEntity p, ServerWorld w, Etape e) {
        switch (e.sorte()) {
            case PARLER -> {
                for (Entity en : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(400), en -> en.isAlive())) {
                    if (en instanceof Figure f && e.cle().equals(new String[]{"tycho", "kepler", "rodolphe", "loew"}[f.qui()])) return en.getPos();
                    if (en.getCommandTags().contains("ombre_pnj." + e.cle())) return en.getPos();
                }
            }
            case TUER, VOLER, SUIVRE -> {
                if (e.cle().isEmpty()) return null;
                for (Entity en : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(400), en -> en.isAlive()
                        && (en.getCommandTags().contains(e.cle()) || en.getCommandTags().contains("ombre_q." + e.cle()) || en.getCommandTags().contains("ombre_pnj." + e.cle()))))
                    return en.getPos();
            }
            case FOUILLER -> {
                for (Entity en : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(400), en -> en.getCommandTags().contains("ombre_q." + e.cle())))
                    return en.getPos();
            }
            case SYNCHRO, ALLER, LIVRER -> {
                if (e.sorte() == Sorte.SYNCHRO) {
                    for (Entity en : w.iterateEntities())
                        if (en.getType() == EntityType.MARKER && en.getCommandTags().contains(Gardes.LIEU) && en.getCustomName() != null
                                && en.getCustomName().getString().equals(e.cle())) return en.getPos();
                    return null;
                }
                return lieu(w, e.cle());
            }
            default -> {}
        }
        return null;
    }

    /** Pour la commande /ombre memoire n : aller directement à une mémoire (pour la tester, ou la rejouer). */
    public static int aller(ServerPlayerEntity p, int h) {
        h = Math.max(0, Math.min(HISTOIRE.size() - 1, h));
        fixer(p, "ombre_h:", h);
        fixer(p, "ombre_e:", 0);
        fixer(p, "ombre_n:", 0);
        annoncer(p, HISTOIRE.get(h));
        debutEtape(p, HISTOIRE.get(h).etapes().get(0));
        return 1;
    }

    /** Les quêtes à jour pour quelqu'un qui n'a pas d'histoire en cours (un monde d'avant cette version). */
    public static boolean active(ServerPlayerEntity p) { return memoire(p) >= 0; }

    /** Un garde tué près du joueur : on lui dit (pour les contrats). */
    public static void mort(LivingEntity mort, ServerPlayerEntity tueur) {
        if (Discretion.estGarde(mort) || mort instanceof Passant) evenement(tueur, "tue", mort);
    }
}
