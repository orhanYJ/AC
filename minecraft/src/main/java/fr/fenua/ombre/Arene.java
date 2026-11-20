package fr.fenua.ombre;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * La carte : un fortin à infiltrer, construit bloc par bloc à 30 blocs devant le joueur.
 *
 * Plan vu du dessus (x vers l'est, z vers le sud, centre = 0,0) :
 *
 *   tour NO ┌───────────── palissade ─────────────┐ tour NE      (sentinelles)
 *           │      maison du capitaine (toit :      │
 *           │      la CIBLE + un garde)             │
 *           │                     grande tour 16 m ─┼─ poutre → foin (saut de la foi)
 *           │  caserne        feu de camp           │
 *           │                                       │   hautes herbes dehors, côté est
 *           │  hautes herbes          poudrière     │
 *   tour SO └──────────── porte (2 sapeurs) ────────┘ tour SE
 */
public final class Arene {
    private Arene() {}

    private static final int R = 24;          // demi-largeur de la zone aplanie
    private static final int MUR = 18;        // demi-largeur de l'enceinte
    private static final int HAUT = 26;       // hauteur dégagée

    private static ServerWorld w;
    private static int ox, oy, oz;            // origine : centre du fortin, au niveau du sol

    public static int construire(ServerCommandSource src) {
        w = src.getWorld();
        Vec3d pos = src.getPosition();
        Direction face = Direction.fromRotation(src.getRotation().y);
        BlockPos c = BlockPos.ofFloored(pos).offset(face, 30);
        ox = c.getX(); oy = c.getY(); oz = c.getZ();

        aplanir();
        sentier();
        enceinte();
        tours();
        grandeTour();
        maisonCapitaine();
        caserne();
        poudriere();
        decor();
        herbes();
        raccorder();
        gardes();

        src.sendFeedback(() -> Text.literal(
                "§6Le fortin est prêt§r, droit devant toi (centre : " + ox + " " + oy + " " + oz + ").\n"
                + "§eCible§r : le capitaine Morvan, sur le toit de la maison du nord (il brille en or avec l'Instinct).\n"
                + "§eConseils§r : passe par les hautes herbes à l'est, escalade la palissade, grimpe la grande tour "
                + "(point de vue), puis saute dans le foin. §l/time set night§r pour l'ambiance.\n"
                + "§7Difficulté Facile ou plus (en Paisible, les gardes disparaissent). /ombre aide pour les commandes."), true);
        return 1;
    }

    // ───────────────────────── outils ─────────────────────────

    private static void set(int x, int y, int z, BlockState s) {
        w.setBlockState(new BlockPos(ox + x, oy + y, oz + z), s, Block.NOTIFY_LISTENERS);
    }

    private static void set(int x, int y, int z, Block b) { set(x, y, z, b.getDefaultState()); }

    private static void remplir(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(x, y, z, s);
    }

    private static void remplir(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        remplir(x0, y0, z0, x1, y1, z1, b.getDefaultState());
    }

    /** Murs creux (sans sol ni plafond). */
    private static void murs(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        remplir(x0, y0, z0, x1, y1, z0, b);
        remplir(x0, y0, z1, x1, y1, z1, b);
        remplir(x0, y0, z0, x0, y1, z1, b);
        remplir(x1, y0, z0, x1, y1, z1, b);
    }

    private static BlockState marche(Direction d) {
        return Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, d);
    }

    private static void hauteHerbe(int x, int z) {
        set(x, -1, z, Blocks.GRASS_BLOCK);
        set(x, 0, z, Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, 1, z, Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    // ───────────────────────── construction ─────────────────────────

    /** Vide l'air au-dessus, pose de l'herbe au sol et de la terre en dessous. */
    private static void aplanir() {
        remplir(-R, 0, -R, R, HAUT, R, Blocks.AIR);
        remplir(-R, -1, -R, R, -1, R, Blocks.GRASS_BLOCK);
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++)
                for (int y = -2; y >= -4; y--) {
                    BlockPos p = new BlockPos(ox + x, oy + y, oz + z);
                    if (w.getBlockState(p).isAir() || !w.getFluidState(p).isEmpty()) set(x, y, z, Blocks.DIRT);
                }
    }

    /** Un chemin de terre de la porte jusqu'au feu de camp, et vers la maison du capitaine. */
    private static void sentier() {
        remplir(-1, -1, 2, 1, -1, R, Blocks.DIRT_PATH);
        remplir(-1, -1, -7, 1, -1, 2, Blocks.COARSE_DIRT);
        remplir(-7, -1, -7, -1, -1, -6, Blocks.COARSE_DIRT);
    }

    /** La palissade de rondins (5 blocs), pointue un rondin sur deux, porte au sud. */
    private static void enceinte() {
        murs(-MUR, 0, -MUR, MUR, 4, MUR, Blocks.SPRUCE_LOG);
        for (int i = -MUR; i <= MUR; i += 2) {
            set(i, 5, -MUR, Blocks.SPRUCE_LOG); set(i, 5, MUR, Blocks.SPRUCE_LOG);
            set(-MUR, 5, i, Blocks.SPRUCE_LOG); set(MUR, 5, i, Blocks.SPRUCE_LOG);
        }
        // La porte : 5 de large, 4 de haut, linteau en planches
        remplir(-2, 0, MUR, 2, 3, MUR, Blocks.AIR);
        remplir(-3, 4, MUR, 3, 4, MUR, Blocks.SPRUCE_PLANKS);
        remplir(-2, 5, MUR, 2, 5, MUR, Blocks.AIR);
        for (int s : new int[]{-3, 3}) {
            set(s, 0, MUR + 2, Blocks.SPRUCE_FENCE);
            set(s, 1, MUR + 2, Blocks.SPRUCE_FENCE);
            set(s, 2, MUR + 2, Blocks.LANTERN);
        }
    }

    /** Quatre tours d'angle en pierre (9 blocs) avec plate-forme et garde-corps. */
    private static void tours() {
        for (int sx : new int[]{-MUR, MUR})
            for (int sz : new int[]{-MUR, MUR}) {
                remplir(sx - 2, 0, sz - 2, sx + 2, 8, sz + 2, Blocks.COBBLESTONE);
                remplir(sx - 2, 2, sz - 2, sx + 2, 2, sz + 2, Blocks.MOSSY_COBBLESTONE);
                remplir(sx - 2, 9, sz - 2, sx + 2, 9, sz + 2, Blocks.SPRUCE_PLANKS);
                murs(sx - 2, 10, sz - 2, sx + 2, 10, sz + 2, Blocks.SPRUCE_FENCE);
                set(sx - 2, 11, sz - 2, Blocks.LANTERN);
                set(sx + 2, 11, sz + 2, Blocks.LANTERN);
            }
    }

    /** La grande tour (point de vue), 16 blocs, avec une poutre au-dessus d'un tas de foin. */
    private static void grandeTour() {
        remplir(7, 0, -3, 9, 15, -1, Blocks.STONE_BRICKS);
        for (int y = 0; y <= 15; y += 5) remplir(7, y, -3, 9, y, -1, Blocks.CHISELED_STONE_BRICKS);
        remplir(7, 8, -3, 9, 9, -1, Blocks.CRACKED_STONE_BRICKS);
        // La poutre qui dépasse vers l'est
        remplir(10, 15, -2, 12, 15, -2, Blocks.STRIPPED_SPRUCE_LOG.getDefaultState()
                .with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
        // Le foin, sous le bout de la poutre (le saut de la foi)
        remplir(11, 0, -3, 13, 0, -1, Blocks.HAY_BLOCK);
        set(12, 1, -2, Blocks.HAY_BLOCK);
        // Le point de vue : un marqueur invisible au sommet
        MarkerEntity m = EntityType.MARKER.create(w);
        if (m != null) {
            m.refreshPositionAndAngles(ox + 8.5, oy + 16, oz - 1.5, 0, 0);
            m.addCommandTag(Gardes.VUE);
            w.spawnEntity(m);
        }
    }

    /** La maison du capitaine : deux niveaux de pierre, toit plat entouré d'un parapet. */
    private static void maisonCapitaine() {
        int x0 = -10, x1 = -2, z0 = -14, z1 = -8;
        remplir(x0, 0, z0, x1, 0, z1, Blocks.COBBLESTONE);
        murs(x0, 1, z0, x1, 6, z1, Blocks.STONE_BRICKS);
        remplir(x0 + 1, 1, z0 + 1, x1 - 1, 6, z1 - 1, Blocks.AIR);
        remplir(x0, 3, z0, x1, 3, z1, Blocks.SPRUCE_PLANKS);        // plancher de l'étage
        remplir(x0 + 1, 3, z0 + 1, x0 + 1, 3, z0 + 1, Blocks.AIR);  // trappe vers l'étage
        set(x0 + 1, 1, z0 + 1, Blocks.SCAFFOLDING); set(x0 + 1, 2, z0 + 1, Blocks.SCAFFOLDING);
        remplir(x0, 7, z0, x1, 7, z1, Blocks.STONE_BRICKS);          // toit
        murs(x0, 8, z0, x1, 8, z1, Blocks.STONE_BRICK_WALL);         // parapet
        remplir(-7, 8, z1, -5, 8, z1, Blocks.AIR);                   // brèche dans le parapet, au-dessus de la porte
        remplir(-6, 1, z1, -6, 2, z1, Blocks.AIR);                   // porte
        for (int x = x0 + 2; x <= x1 - 2; x += 3) {                  // fenêtres
            set(x, 5, z0, Blocks.AIR); set(x, 5, z1, Blocks.AIR);
        }
        set(-5, 3, z1 + 1, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, false));
        set(-7, 1, z1 + 1, Blocks.BARREL);
        set(-8, 8, -12, Blocks.LANTERN);
    }

    /** La caserne : bois, toit de bardeaux à deux pentes. */
    private static void caserne() {
        int x0 = -15, x1 = -7, z0 = 0, z1 = 6;
        murs(x0, 0, z0, x1, 3, z1, Blocks.OAK_PLANKS);
        for (int x : new int[]{x0, x1})
            for (int z : new int[]{z0, z1}) remplir(x, 0, z, x, 3, z, Blocks.STRIPPED_OAK_LOG);
        remplir(-11, 0, z1, -11, 1, z1, Blocks.AIR);                // porte
        set(-13, 2, z1, Blocks.AIR); set(-9, 2, z1, Blocks.AIR);    // fenêtres
        // Pignons
        for (int x : new int[]{x0, x1}) {
            remplir(x, 4, z0 + 1, x, 4, z1 - 1, Blocks.OAK_PLANKS);
            remplir(x, 5, z0 + 2, x, 5, z1 - 2, Blocks.OAK_PLANKS);
        }
        // Toit : trois rangs qui montent de chaque côté, faîte au milieu
        for (int i = 0; i < 3; i++) {
            remplir(x0 - 1, 4 + i, z0 + i, x1 + 1, 4 + i, z0 + i, marche(Direction.SOUTH));
            remplir(x0 - 1, 4 + i, z1 - i, x1 + 1, 4 + i, z1 - i, marche(Direction.NORTH));
        }
        remplir(x0 - 1, 6, z0 + 3, x1 + 1, 6, z0 + 3, Blocks.SPRUCE_PLANKS);
        remplir(x0 - 1, 7, z0 + 3, x1 + 1, 7, z0 + 3, Blocks.SPRUCE_SLAB);
        // Intérieur : lits de camp (laine) et tonneaux
        for (int x = x0 + 1; x <= x1 - 1; x += 2) set(x, 0, z0 + 1, Blocks.BROWN_WOOL);
        set(x1 - 1, 0, z1 - 1, Blocks.BARREL);
    }

    /** La poudrière : petite case en pierre, tonneaux devant. */
    private static void poudriere() {
        murs(8, 0, 8, 12, 3, 12, Blocks.COBBLESTONE);
        remplir(8, 4, 8, 12, 4, 12, Blocks.COBBLESTONE_SLAB);
        remplir(10, 0, 8, 10, 1, 8, Blocks.AIR);
        set(9, 0, 9, Blocks.BARREL); set(11, 0, 9, Blocks.BARREL); set(11, 0, 11, Blocks.BARREL);
        set(7, 0, 7, Blocks.BARREL); set(7, 1, 7, Blocks.BARREL); set(6, 0, 7, Blocks.BARREL);
    }

    /** Feu de camp, caisses, arbres (à couvert de feuillage), lanternes. */
    private static void decor() {
        set(0, 0, 0, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{2, 1}, {-2, -1}, {1, -2}}) set(p[0], 0, p[1], Blocks.SPRUCE_LOG.getDefaultState()
                .with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
        // Pile de caisses près de la palissade ouest (un marchepied)
        remplir(-17, 0, -3, -16, 0, -2, Blocks.SPRUCE_PLANKS);
        set(-17, 1, -3, Blocks.SPRUCE_PLANKS);
        // Deux arbres dans la cour, et un dehors : on se cache sous le feuillage
        arbre(4, 12);
        arbre(-4, 9);
        arbre(21, -8);
        // Lanternes sur poteaux
        for (int[] p : new int[][]{{3, 6}, {-3, -4}, {5, -6}, {-12, 8}}) {
            set(p[0], 0, p[1], Blocks.SPRUCE_FENCE);
            set(p[0], 1, p[1], Blocks.LANTERN);
        }
    }

    private static void arbre(int x, int z) {
        remplir(x, 0, z, x, 4, z, Blocks.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int y = 3; y <= 5; y++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (y == 5 && (Math.abs(dx) == 2 || Math.abs(dz) == 2)) continue;
                    if (dx == 0 && dz == 0 && y <= 4) continue;
                    set(x + dx, y, z + dz, Blocks.OAK_LEAVES.getDefaultState()
                            .with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
                }
    }

    /** Couloirs de hautes herbes : dehors à l'est et au sud, dedans au sud-ouest. */
    private static void herbes() {
        for (int z = -16; z <= 16; z++) for (int x = 20; x <= 23; x++) if ((x * 7 + z * 3) % 11 != 0) hauteHerbe(x, z);
        for (int x = -14; x <= -5; x++) for (int z = 20; z <= 23; z++) if ((x + z * 5) % 9 != 0) hauteHerbe(x, z);
        for (int x = -16; x <= -9; x++) for (int z = 10; z <= 16; z++) if ((x * 3 + z) % 7 != 0) hauteHerbe(x, z);
        for (int x = 13; x <= 16; x++) for (int z = 13; z <= 16; z++) hauteHerbe(x, z);
    }

    /** Recalcule les raccords (barrières, murets, escaliers) une fois tout posé. */
    private static void raccorder() {
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++)
                for (int y = -1; y <= 12; y++) {
                    BlockPos p = new BlockPos(ox + x, oy + y, oz + z);
                    BlockState s = w.getBlockState(p);
                    if (s.isAir()) continue;
                    BlockState n = Block.postProcessState(s, w, p);
                    if (n != s) w.setBlockState(p, n, Block.NOTIFY_LISTENERS);
                }
    }

    // ───────────────────────── gardes ─────────────────────────

    private static void gardes() {
        Scoreboard sb = w.getScoreboard();
        Team rouges = equipe(sb, "ombre_gardes", Formatting.RED);
        Team ors = equipe(sb, "ombre_cibles", Formatting.GOLD);

        // Deux sapeurs (hache) devant la porte
        garde(EntityType.VINDICATOR, -3, 0, 21, 2, "Sapeur", rouges, false);
        garde(EntityType.VINDICATOR, 3, 0, 21, 2, "Sapeur", rouges, false);
        // Trois fusiliers (arbalète) dans la cour
        garde(EntityType.PILLAGER, 0, 0, 6, 7, "Fusilier", rouges, false);
        garde(EntityType.PILLAGER, -3, 0, -4, 6, "Fusilier", rouges, false);
        garde(EntityType.PILLAGER, 12, 0, 4, 6, "Fusilier", rouges, false);
        // Deux sentinelles sur les tours du nord
        garde(EntityType.PILLAGER, -MUR, 10, -MUR, 1, "Sentinelle", rouges, false);
        garde(EntityType.PILLAGER, MUR, 10, -MUR, 1, "Sentinelle", rouges, false);
        // La cible et son garde du corps, sur le toit de la maison du capitaine
        garde(EntityType.VINDICATOR, -6, 8, -11, 2, "Capitaine Morvan", ors, true);
        garde(EntityType.PILLAGER, -9, 8, -13, 1, "Garde du corps", rouges, false);
    }

    private static Team equipe(Scoreboard sb, String nom, Formatting couleur) {
        Team t = sb.getTeam(nom);
        if (t == null) t = sb.addTeam(nom);
        t.setColor(couleur);
        t.setFriendlyFireAllowed(false);
        return t;
    }

    private static <T extends PathAwareEntity> void garde(EntityType<T> type, int x, int y, int z, int rayon,
                                                          String nom, Team equipe, boolean cible) {
        T g = type.create(w);
        if (g == null) return;
        BlockPos p = new BlockPos(ox + x, oy + y, oz + z);
        g.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, w.random.nextFloat() * 360f, 0);
        g.initialize(w, w.getLocalDifficulty(p), SpawnReason.COMMAND, null);
        g.setPersistent();
        g.setCustomName(Text.literal(nom));
        g.setPositionTarget(p, rayon);          // il ne s'éloigne pas de son poste en temps normal
        g.addCommandTag(Gardes.GARDE);
        if (cible) g.addCommandTag(Gardes.CIBLE);
        w.spawnEntityAndPassengers(g);
        w.getScoreboard().addScoreHolderToTeam(g.getNameForScoreboard(), equipe);
    }
}
