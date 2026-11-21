package fr.fenua.ombre.ac;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
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
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les défis des toits, comme dans Assassin's Creed II :
 *  - les PLUMES de corbeau : quinze plumes cachées sur les hauteurs de Prague (marqueurs « ombre_plume »).
 *    Une colonne de lumière les signale à qui ne les a pas encore ; on les ramasse en passant dessus.
 *  - la COURSE DES TOITS : des balises (marqueurs « ombre_course », nommés 0, 1, 2…) à franchir dans l'ordre,
 *    chronométrées ; la 0 est le départ. On tombe dans la rue : la course est perdue. Le record est gardé.
 */
public final class Defis {
    private Defis() {}

    public static final String PLUME = "ombre_plume", COURSE = "ombre_course", PLUME_TAG = "ombre_plume:";
    public static final int PLUMES = 15;
    /** Les plumes d'entraînement du cours de rappel : elles ne comptent pas parmi les quinze de Prague. */
    public static final String ECOLE = "ombre_ecole", ECOLE_TAG = "ombre_plume_ecole:";
    private static final String RECORD = "ombre_course_record";

    private static final Map<ServerWorld, List<Entity>> PLUMES_MONDE = new HashMap<>(), BALISES = new HashMap<>();
    /** Les coureurs : prochaine balise, tick du départ. */
    private static final Map<UUID, long[]> COURSES = new HashMap<>();

    public static void tick(MinecraftServer serveur) {
        if (serveur.getTicks() % 40 == 0) {
            for (ServerWorld w : serveur.getWorlds()) {
                List<Entity> plumes = new ArrayList<>(), balises = new ArrayList<>();
                for (Entity e : w.iterateEntities()) {
                    if (e.getType() != EntityType.MARKER) continue;
                    if (e.getCommandTags().contains(PLUME)) plumes.add(e);
                    else if (e.getCommandTags().contains(COURSE)) balises.add(e);
                }
                balises.sort((a, b) -> Integer.compare(numero(a), numero(b)));
                PLUMES_MONDE.put(w, plumes);
                BALISES.put(w, balises);
            }
        }
        if (serveur.getTicks() % 5 != 0) return;
        for (ServerPlayerEntity p : serveur.getPlayerManager().getPlayerList()) {
            if (p.isSpectator()) continue;
            plumes(p);
            course(p, serveur.getTicks() % 10 == 0);
        }
    }

    private static int numero(Entity e) {
        try { return e.hasCustomName() ? Integer.parseInt(e.getCustomName().getString()) : 999; } catch (NumberFormatException x) { return 999; }
    }

    // ───────────────────────── les plumes ─────────────────────────

    public static int plumes(ServerPlayerEntity p, boolean compter) {
        int n = 0;
        for (String t : p.getCommandTags()) if (t.startsWith(PLUME_TAG)) n++;
        return n;
    }

    private static void plumes(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        for (Entity pl : PLUMES_MONDE.getOrDefault(w, List.of())) {
            String cle = (pl.getCommandTags().contains(ECOLE) ? ECOLE_TAG : PLUME_TAG) + pl.getBlockPos().asLong();
            if (p.getCommandTags().contains(cle)) continue;
            double d = p.squaredDistanceTo(pl);
            if (d > 64 * 64) continue;
            Vec3d c = pl.getPos();
            // Une plume qui tourne, et un fil de lumière qui monte vers le ciel (visible de loin)
            double a = w.getTime() * 0.25;
            w.spawnParticles(p, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)), true,
                    c.x + Math.cos(a) * 0.3, c.y + 0.6, c.z + Math.sin(a) * 0.3, 1, 0, 0, 0, 0);
            w.spawnParticles(p, ParticleTypes.END_ROD, true, c.x, c.y + 1.5, c.z, 2, 0.05, 1.2, 0.05, 0.005);
            if (d < 1.7 * 1.7) {
                p.addCommandTag(cle);
                if (cle.startsWith(ECOLE_TAG)) {
                    p.sendMessage(Text.literal("§f✒ Plume ramassée"), true);
                    w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 1f, 1.3f);
                    continue;
                }
                int n = plumes(p, true);
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 1f, 1.3f);
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7f, 0.7f);
                w.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)), c.x, c.y + 0.6, c.z, 16, 0.3, 0.3, 0.3, 0.1);
                p.sendMessage(Text.literal("§f✒ Plume de corbeau §7" + n + " / " + PLUMES), true);
                if (n == PLUMES) {
                    p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6Toutes les plumes")));
                    p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Le Corbeau n'a plus de secret pour toi")));
                    p.getInventory().offerOrDrop(new ItemStack(fr.fenua.ombre.objet.Objets.THALERS, 64));
                    w.playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
                }
            }
        }
    }

    // ───────────────────────── la course des toits ─────────────────────────

    /** La prochaine balise du coureur (ou le départ, s'il en est tout près), pour la boussole. */
    @Nullable
    public static BlockPos prochaineBalise(ServerPlayerEntity p) {
        List<Entity> b = BALISES.getOrDefault(p.getServerWorld(), List.of());
        if (b.isEmpty()) return null;
        long[] c = COURSES.get(p.getUuid());
        if (c != null && c[0] < b.size()) return b.get((int) c[0]).getBlockPos();
        return p.squaredDistanceTo(b.get(0)) < 60 * 60 ? b.get(0).getBlockPos() : null;
    }

    private static void course(ServerPlayerEntity p, boolean afficher) {
        ServerWorld w = p.getServerWorld();
        List<Entity> b = BALISES.getOrDefault(w, List.of());
        if (b.size() < 2) return;
        long[] c = COURSES.get(p.getUuid());
        long t = w.getTime();
        if (c == null) {
            Entity depart = b.get(0);
            if (p.squaredDistanceTo(depart) > 48 * 48) return;
            anneau(w, p, depart.getPos(), ParticleTypes.FLAME);
            if (p.squaredDistanceTo(depart) < 2.2 * 2.2) {
                COURSES.put(p.getUuid(), new long[]{1, t});
                p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6Course des toits")));
                p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Franchis les " + (b.size() - 1) + " balises — ne touche pas la rue !")));
                w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1f, 1.2f);
            }
            return;
        }
        int i = (int) c[0];
        double secondes = (t - c[1]) / 20.0;
        // Perdue : tombé dans la rue, ou trop long
        double plusBas = b.stream().mapToDouble(Entity::getY).min().orElse(p.getY());
        if ((p.isOnGround() && p.getY() < plusBas - 6) || secondes > 300 || p.isDead()) {
            COURSES.remove(p.getUuid());
            p.sendMessage(Text.literal("§cCourse perdue §7— retourne au départ (anneau de flammes) pour recommencer."), true);
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1f, 0.6f);
            return;
        }
        Entity balise = b.get(i);
        anneau(w, p, balise.getPos(), ParticleTypes.END_ROD);
        if (i + 1 < b.size()) anneau(w, p, b.get(i + 1).getPos(), ParticleTypes.WAX_OFF);
        if (p.squaredDistanceTo(balise.getPos().add(0, 1, 0)) < 2.4 * 2.4) {
            i++;
            c[0] = i;
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1f, 0.8f + i * 0.08f);
            if (i >= b.size()) { arrivee(p, w, t - c[1]); return; }
        }
        if (afficher) p.sendMessage(Text.literal(String.format("§6Course des toits §7— balise §f%d/%d §7— §f%.1f s", i, b.size() - 1, secondes)), true);
    }

    private static void arrivee(ServerPlayerEntity p, ServerWorld w, long ticks) {
        COURSES.remove(p.getUuid());
        Scoreboard sb = w.getScoreboard();
        ScoreboardObjective o = sb.getNullableObjective(RECORD);
        if (o == null) o = sb.addObjective(RECORD, ScoreboardCriterion.DUMMY, Text.literal("Record de la course (ticks)"),
                ScoreboardCriterion.RenderType.INTEGER, true, null);
        ScoreAccess s = sb.getOrCreateScore(p, o);
        boolean record = s.getScore() <= 0 || ticks < s.getScore();
        if (record) s.setScore((int) ticks);
        p.addCommandTag("ombre_course_finie");
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(String.format("§6%.1f s", ticks / 20.0))));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(record ? "§aNouveau record !" : String.format("§7Record : %.1f s", s.getScore() / 20.0))));
        w.playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1.2f);
    }

    /** Un anneau de particules debout, vu seulement par ce joueur. */
    private static void anneau(ServerWorld w, ServerPlayerEntity p, Vec3d c, net.minecraft.particle.ParticleEffect type) {
        for (int k = 0; k < 14; k++) {
            double a = k * Math.PI * 2 / 14 + w.getTime() * 0.05;
            w.spawnParticles(p, type, true, c.x + Math.cos(a) * 1.2, c.y + 1 + Math.sin(a) * 1.2, c.z, 1, 0, 0, 0, 0);
        }
    }

    /** Pose une plume ou une balise (marqueur invisible). */
    public static void poser(ServerWorld w, double x, double y, double z, String tag, @Nullable String nom) {
        Entity m = EntityType.MARKER.create(w);
        if (m == null) return;
        m.refreshPositionAndAngles(x, y, z, 0, 0);
        m.addCommandTag(tag);
        if (nom != null) m.setCustomName(Text.literal(nom));
        w.spawnEntity(m);
    }
}
