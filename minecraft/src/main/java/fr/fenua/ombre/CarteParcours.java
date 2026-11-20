package fr.fenua.ombre;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.bloc.PoteauTyrolienne;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Grande carte d'entraînement au parkour, sans aucun garde : /ombre parcours.
 * Elle est construite droit devant le joueur, quelle que soit la direction où il regarde.
 *
 * On travaille en coordonnées « locales » :
 *   b = vers l'avant (0 au départ, ~195 au bout), a = vers la droite, y = hauteur au-dessus du sol.
 *
 *   b   0  Départ
 *   b   8  1. Escalade (murs de 3, 6, 10 et 15 blocs, foin au pied)
 *   b  16  2. Course libre (marches d'un bloc)
 *   b  28  3. Bonds (piliers au-dessus d'un bassin : 2, 3, 4 blocs d'écart)
 *   b  56  4. Course sur mur (au-dessus d'une fosse d'eau de 10 blocs)
 *   b  74  5. Rebond (couloir entre deux murs, au-dessus de l'eau)
 *   b  98  6. Rebords (murs de plus en plus hauts, à saisir au vol)
 *   b 116  7. Saut de la foi (tour de 22 blocs, poutre, foin) et roulade (piliers de 5, 8, 11)
 *   b 118     + monte-charge au pied de la tour, tyrolienne du sommet vers une plate-forme
 *   b  26  9. Perches (à droite des bonds) : trois barres au-dessus d'un bassin
 *   b 132  8. La ville : 36 maisons, poutres, poteaux, lampadaires, enseignes, corniches,
 *             auvents, échafaudages, un monte-charge et un perchoir au sommet de la tour
 */
public final class CarteParcours {
    private final ServerWorld w;
    private final BlockPos origine;
    private final Direction avant, droite;
    private final Random hasard = new Random(1844);

    private CarteParcours(ServerWorld w, BlockPos origine, Direction avant) {
        this.w = w;
        this.origine = origine;
        this.avant = avant;
        this.droite = avant.rotateYClockwise();
    }

    public static int construire(ServerCommandSource src) {
        Vec3d pos = src.getPosition();
        Direction face = Direction.fromRotation(src.getRotation().y);
        BlockPos depart = BlockPos.ofFloored(pos).offset(face, 4);
        CarteParcours c = new CarteParcours(src.getWorld(), depart, face);
        c.batir();

        // On amène le joueur au départ, et on y met son point de réapparition
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            BlockPos d = c.pos(0, 0, 0);
            p.teleport(src.getWorld(), d.getX() + 0.5, d.getY(), d.getZ() + 0.5, face.asRotation(), 10f);
            p.setSpawnPoint(src.getWorld().getRegistryKey(), d, face.asRotation(), true, false);
        }
        src.sendFeedback(() -> Text.literal(
                "§6Le parcours est prêt§r : 8 épreuves sur 200 blocs, sans aucun garde.\n"
                + "Suis les panneaux. Tomber dans l'eau ne fait pas mal ; si tu meurs, tu reviens au départ.\n"
                + "§7Astuce : passe en survie (/gamemode survival) pour que les chutes comptent."), true);
        return 1;
    }

    // ───────────────────────── outils ─────────────────────────

    private BlockPos pos(int a, int y, int b) {
        return origine.offset(droite, a).offset(avant, b).up(y);
    }

    private void set(int a, int y, int b, BlockState s) {
        w.setBlockState(pos(a, y, b), s, Block.NOTIFY_LISTENERS);
    }

    private void set(int a, int y, int b, Block bl) { set(a, y, b, bl.getDefaultState()); }

    private void boite(int a0, int y0, int b0, int a1, int y1, int b1, BlockState s) {
        for (int a = Math.min(a0, a1); a <= Math.max(a0, a1); a++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int b = Math.min(b0, b1); b <= Math.max(b0, b1); b++)
                    set(a, y, b, s);
    }

    private void boite(int a0, int y0, int b0, int a1, int y1, int b1, Block bl) {
        boite(a0, y0, b0, a1, y1, b1, bl.getDefaultState());
    }

    /** Une poutre de bois qui court vers l'avant (sens b) ou vers la droite (sens a). */
    private BlockState poutre(boolean versAvant) {
        return Blocks.STRIPPED_SPRUCE_LOG.getDefaultState()
                .with(PillarBlock.AXIS, versAvant ? avant.getAxis() : droite.getAxis());
    }

    /** Les quatre directions locales, en directions du monde : +b, -b, +a, -a. */
    private Direction dir(char c) {
        return switch (c) { case 'b' -> avant; case 'B' -> avant.getOpposite(); case 'a' -> droite; default -> droite.getOpposite(); };
    }

    /** Un bloc orienté vers un mur (corniche, enseigne, auvent, monte-charge). */
    private BlockState vers(Block bl, char c) { return bl.getDefaultState().with(Properties.HORIZONTAL_FACING, dir(c)); }

    /** Un bassin de 3 blocs de profondeur (on peut y tomber sans mal). */
    private void bassin(int a0, int b0, int a1, int b1) {
        boite(a0 - 1, -4, b0 - 1, a1 + 1, -1, b1 + 1, Blocks.STONE_BRICKS);
        boite(a0, -3, b0, a1, -1, b1, Blocks.WATER);
    }

    /** Un poteau avec une lanterne. */
    private void lanterne(int a, int b) {
        boite(a, 0, b, a, 1, b, Blocks.SPRUCE_LOG);
        set(a, 2, b, Blocks.LANTERN);
    }

    /** Un panneau tourné vers le joueur qui arrive, texte lumineux. */
    private void panneau(int a, int b, String l1, String l2, String l3, String l4) { panneau(a, 0, b, l1, l2, l3, l4); }

    private void panneau(int a, int y, int b, String l1, String l2, String l3, String l4) {
        BlockState s = Blocks.DARK_OAK_SIGN.getDefaultState()
                .with(SignBlock.ROTATION, RotationPropertyHelper.fromDirection(avant.getOpposite()));
        BlockPos p = pos(a, y, b);
        w.setBlockState(p, s, Block.NOTIFY_ALL);
        if (w.getBlockEntity(p) instanceof SignBlockEntity sbe) {
            SignText t = new SignText().withColor(DyeColor.YELLOW).withGlowing(true)
                    .withMessage(0, Text.literal(l1)).withMessage(1, Text.literal(l2))
                    .withMessage(2, Text.literal(l3)).withMessage(3, Text.literal(l4));
            sbe.setText(t, true);
        }
    }

    // ───────────────────────── construction ─────────────────────────

    private void batir() {
        aplanir();
        depart();
        cachettes();
        escalade();
        courseLibre();
        bonds();
        courseSurMur();
        rebond();
        rebords();
        sautDeLaFoi();
        perches();
        village();
    }

    private static final int A = 32, B0 = -3, B1 = 198, H = 30;

    private void aplanir() {
        for (int a = -A; a <= A; a++)
            for (int b = B0; b <= B1; b++) {
                for (int y = 0; y <= H; y++) {
                    BlockPos p = pos(a, y, b);
                    if (!w.getBlockState(p).isAir()) w.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
                set(a, -1, b, Blocks.GRASS_BLOCK);
                for (int y = -2; y >= -4; y--) {
                    BlockPos p = pos(a, y, b);
                    if (w.getBlockState(p).isAir() || !w.getFluidState(p).isEmpty()) set(a, y, b, Blocks.DIRT);
                }
            }
        // Le sentier qui relie les épreuves
        boite(-1, -1, 0, 1, -1, 131, Blocks.DIRT_PATH);
    }

    private void depart() {
        boite(-5, -1, -2, 5, -1, 4, Blocks.POLISHED_ANDESITE);
        panneau(-3, 3, "PARCOURS", "DE L'OMBRE", "Suis les", "panneaux");
        panneau(3, 3, "Pas de gardes", "Tomber à l'eau", "ne fait rien", "/ombre aide");
        lanterne(-5, 4);
        lanterne(5, 4);
    }

    /** 10. À gauche du départ : toutes les cachettes, côte à côte, pour les essayer. */
    private void cachettes() {
        panneau(-9, -1, "CACHETTES", "Meule, puits,", "cabine, herbes.", "R = attirer");
        boite(-17, -1, 0, -8, -1, 6, Blocks.GRASS_BLOCK);
        boite(-17, 0, 0, -16, 1, 1, OmbreBlocs.MEULE);
        set(-13, 0, 1, OmbreBlocs.PUITS);
        set(-10, 0, 1, vers(OmbreBlocs.CABINE, 'a'));
        for (int a = -17; a <= -9; a++)
            for (int b = 4; b <= 6; b++) {
                set(a, 0, b, Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
                set(a, 1, b, Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
            }
        lanterne(-8, 0);
    }

    /** 1. Quatre murs de hauteurs croissantes, avec du foin au pied pour redescendre. */
    private void escalade() {
        panneau(-2, 6, "1. ESCALADE", "Espace contre", "le mur. Z S Q D", "Maj = lâcher");
        int[][] murs = {{-11, -8, 3}, {-5, -2, 6}, {2, 5, 10}, {8, 11, 15}};
        Block[] mat = {Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.DEEPSLATE_BRICKS};
        for (int i = 0; i < murs.length; i++) {
            int[] m = murs[i];
            boite(m[0], 0, 8, m[1], m[2] - 1, 11, mat[i]);
            if (m[2] >= 6) {
                boite(m[0], 0, 13, m[1], 0, 14, Blocks.HAY_BLOCK);    // saut de la foi pour redescendre
                boite(m[0] + 1, 1, 13, m[1] - 1, 1, 13, Blocks.HAY_BLOCK);
            }
            set((m[0] + m[1]) / 2, m[2], 9, Blocks.LANTERN);
        }
    }

    /** 2. Des marches d'un bloc : en sprint, on les monte sans sauter. */
    private void courseLibre() {
        panneau(-4, 14, "2. COURSE", "LIBRE", "Sprinte : tu", "montes seul");
        for (int i = 0; i < 4; i++) boite(-3, 0, 16 + i * 2, 3, i, 17 + i * 2, Blocks.SMOOTH_STONE);
        boite(-3, 0, 24, 3, 3, 27, Blocks.SMOOTH_STONE);           // palier de départ des bonds (hauteur 4)
    }

    /** 3. Piliers au-dessus d'un bassin, écarts de 2, 3, 4 puis 3 blocs. */
    private void bonds() {
        set(-4, 4, 26, Blocks.LANTERN);
        bassin(-5, 28, 5, 48);
        int[][] piliers = {{30, 32}, {36, 38}, {43, 45}};
        for (int[] p : piliers) boite(-1, -3, p[0], 1, 3, p[1], Blocks.POLISHED_DEEPSLATE);
        boite(-3, 0, 49, 3, 3, 52, Blocks.SMOOTH_STONE);            // arrivée
        for (int i = 0; i < 3; i++) boite(-3, 0, 53 + i, 3, 2 - i, 53 + i, Blocks.SMOOTH_STONE);
        panneau(-4, 23, "3. BONDS", "Sprinte et", "saute : 2, 3", "puis 4 blocs");
    }

    /** 4. Un mur le long d'une fosse d'eau de 10 blocs : on court dessus. */
    private void courseSurMur() {
        panneau(2, 57, "4. COURSE", "SUR MUR", "Sprinte, saute", "le long, Z tenu");
        bassin(-4, 60, 3, 69);
        boite(-6, 0, 57, -5, 5, 72, Blocks.STONE_BRICKS);
        boite(-6, 2, 57, -5, 2, 72, Blocks.MOSSY_STONE_BRICKS);     // une bande pour lire la hauteur
        boite(-6, 6, 57, -5, 6, 72, Blocks.STONE_BRICK_SLAB);
        lanterne(4, 59);
        lanterne(4, 71);
    }

    /** 5. Un couloir de 3 blocs entre deux murs, au-dessus de 19 blocs d'eau : on rebondit de mur en mur. */
    private void rebond() {
        panneau(5, 74, "5. REBOND", "Sur le mur :", "Espace = saute", "vers l'autre");
        bassin(-1, 77, 1, 95);
        boite(-3, 0, 75, -2, 7, 97, Blocks.SANDSTONE);
        boite(2, 0, 75, 3, 7, 97, Blocks.SANDSTONE);
        boite(-3, 3, 75, -2, 3, 97, Blocks.CUT_SANDSTONE);
        boite(2, 3, 75, 3, 3, 97, Blocks.CUT_SANDSTONE);
        lanterne(-5, 75);
        lanterne(5, 97);
    }

    /** 6. Trois murs de plus en plus hauts : on saute, on s'accroche au bord, on se hisse. */
    private void rebords() {
        panneau(-3, 97, "6. REBORDS", "Saute au mur :", "on s'accroche.", "Espace = monter");
        panneau(3, 97, "Q / D : glisser", "Maj : lâcher", "S + Espace :", "saut arrière");
        boite(-2, 0, 98, 2, 0, 98, Blocks.SMOOTH_STONE);           // deux marches
        boite(-2, 0, 99, 2, 1, 99, Blocks.SMOOTH_STONE);
        boite(-8, 0, 100, 8, 2, 102, Blocks.SMOOTH_STONE);          // palier, hauteur 3
        bassin(-8, 103, 8, 112);
        boite(-8, -3, 105, 8, 4, 106, Blocks.STONE_BRICKS);         // mur 1 : hauteur 5
        boite(-8, -3, 109, 8, 6, 110, Blocks.STONE_BRICKS);         // mur 2 : hauteur 7
        boite(-8, 0, 113, 8, 8, 114, Blocks.STONE_BRICKS);          // mur 3 : hauteur 9
        boite(-8, 3, 109, 8, 3, 110, Blocks.MOSSY_STONE_BRICKS);
        set(-8, 9, 114, Blocks.LANTERN);
        set(8, 9, 114, Blocks.LANTERN);
    }

    /** 7. Une tour de 22 blocs, une poutre, du foin ; et trois piliers pour la roulade. */
    private void sautDeLaFoi() {
        panneau(-4, 116, "7. SAUT DE", "LA FOI", "Saute dans la", "meule, cache-toi");
        boite(-1, 0, 117, 1, 21, 119, Blocks.STONE_BRICKS);
        for (int y = 0; y <= 21; y += 4) boite(-1, y, 117, 1, y, 119, Blocks.CHISELED_STONE_BRICKS);
        boite(0, 21, 120, 0, 21, 124, poutre(true));
        boite(-2, 0, 123, 2, 1, 127, OmbreBlocs.MEULE);              // meule de foin : on s'y cache aussi
        set(-1, 22, 119, Blocks.LANTERN);
        // Monte-charge au pied de la tour (côté -a), tyrolienne du sommet vers une plate-forme à 21 blocs
        set(-2, 0, 118, vers(OmbreBlocs.MONTE_CHARGE, 'a'));
        panneau(-4, 120, "MONTE-CHARGE", "Clic droit sur", "la plate-forme", "-> au sommet");
        boite(22, 0, 116, 26, 7, 120, Blocks.SPRUCE_PLANKS);
        set(26, 8, 116, Blocks.LANTERN);
        set(1, 22, 118, OmbreBlocs.POTEAU);
        set(22, 8, 118, OmbreBlocs.POTEAU);
        PoteauTyrolienne.relier(w, pos(1, 22, 118), pos(22, 8, 118));
        panneau(0, 22, 117, "TYROLIENNE", "Clic droit sur", "le poteau.", "Maj = lâcher");
        boite(27, 0, 117, 29, 0, 119, Blocks.HAY_BLOCK);
        set(28, 1, 118, Blocks.HAY_BLOCK);
        // Roulade : trois piliers à escalader, puis on saute à côté en gardant Maj
        panneau(5, 116, "ROULADE", "Garde Maj en", "touchant le sol", "(9 blocs max)");
        int[][] p = {{117, 5}, {121, 8}, {125, 11}};
        for (int[] q : p) {
            boite(7, 0, q[0], 9, q[1] - 1, q[0] + 2, Blocks.POLISHED_ANDESITE);
            set(8, q[1], q[0] + 1, Blocks.LANTERN);
        }
    }

    /** 9. Perches au-dessus d'un bassin : on s'y balance de l'une à l'autre. */
    private void perches() {
        panneau(16, 25, "9. PERCHES", "Saute dessus,", "Z = élan et", "projection");
        boite(18, 0, 26, 22, 0, 26, Blocks.SMOOTH_STONE);
        boite(18, 0, 27, 22, 1, 27, Blocks.SMOOTH_STONE);
        boite(18, 0, 28, 22, 2, 28, Blocks.SMOOTH_STONE);
        boite(17, 0, 29, 23, 3, 32, Blocks.SMOOTH_STONE);           // départ, hauteur 4
        bassin(15, 33, 25, 51);
        BlockState barre = OmbreBlocs.PERCHE.getDefaultState().with(Properties.HORIZONTAL_AXIS, droite.getAxis());
        for (int b : new int[]{36, 41, 46}) {
            boite(18, 5, b, 22, 5, b, barre);
            boite(17, -3, b, 17, 5, b, Blocks.SPRUCE_LOG);           // montants
            boite(23, -3, b, 23, 5, b, Blocks.SPRUCE_LOG);
        }
        boite(17, 0, 52, 23, 2, 56, Blocks.SMOOTH_STONE);           // arrivée, hauteur 3
        set(17, 3, 56, Blocks.LANTERN);
        set(23, 3, 56, Blocks.LANTERN);
    }

    /** 8. Un village de toits : une grille de maisons de hauteurs variées, reliées par des poutres. */
    private void village() {
        panneau(-3, 130, "8. LA VILLE", "Toits, poutres,", "enseignes, lam-", "padaires : libre");
        lanterne(-5, 131);
        lanterne(5, 131);
        final int N = 6, CASE = 10, A0 = -30, BV = 133;
        int[][] h = new int[N][N], a0 = new int[N][N], a1 = new int[N][N], b0 = new int[N][N], b1 = new int[N][N];
        Block[] murs = {Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.COBBLESTONE,
                Blocks.BRICKS, Blocks.SANDSTONE, Blocks.MUD_BRICKS, Blocks.WHITE_TERRACOTTA};
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) {
                int sa = 6 + hasard.nextInt(3), sb = 6 + hasard.nextInt(3);
                a0[i][j] = A0 + i * CASE + hasard.nextInt(CASE - sa + 1);
                b0[i][j] = BV + j * CASE + hasard.nextInt(CASE - sb + 1);
                a1[i][j] = a0[i][j] + sa - 1;
                b1[i][j] = b0[i][j] + sb - 1;
                boolean tour = (i == 3 && j == 3);
                h[i][j] = tour ? 24 : 4 + hasard.nextInt(10);
                maison(a0[i][j], b0[i][j], a1[i][j], b1[i][j], h[i][j], murs[hasard.nextInt(murs.length)], tour);
            }
        // Poutres entre voisins (là où leurs façades se font face)
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) {
                if (i + 1 < N && hasard.nextInt(3) > 0) {
                    int lo = Math.max(b0[i][j], b0[i + 1][j]), hi = Math.min(b1[i][j], b1[i + 1][j]);
                    if (lo <= hi) {
                        int b = (lo + hi) / 2, y = Math.min(h[i][j], h[i + 1][j]) - 1;
                        boite(a1[i][j] + 1, y, b, a0[i + 1][j] - 1, y, b, poutre(false));
                    }
                }
                if (j + 1 < N && hasard.nextInt(3) > 0) {
                    int lo = Math.max(a0[i][j], a0[i][j + 1]), hi = Math.min(a1[i][j], a1[i][j + 1]);
                    if (lo <= hi) {
                        int a = (lo + hi) / 2, y = Math.min(h[i][j], h[i][j + 1]) - 1;
                        boite(a, y, b1[i][j] + 1, a, y, b0[i][j + 1] - 1, poutre(true));
                    }
                }
            }
        // Poteaux dans les rues, et quelques tas de foin pour redescendre
        for (int k = 0; k < 40; k++) {
            int a = A0 + hasard.nextInt(N * CASE), b = BV + hasard.nextInt(N * CASE);
            if (!w.getBlockState(pos(a, 0, b)).isAir()) continue;
            if (k % 3 == 0) {
                boite(a - 1, 0, b - 1, a + 1, 1, b + 1, OmbreBlocs.MEULE);
            } else {
                boite(a, 0, b, a, 2 + hasard.nextInt(6), b, Blocks.SPRUCE_LOG);
            }
        }
        // Le point de vue au sommet de la tour, avec une poutre au-dessus d'un tas de foin
        int ta = (a0[3][3] + a1[3][3]) / 2, tb = (b0[3][3] + b1[3][3]) / 2;
        set(ta, 24, tb, OmbreBlocs.PERCHOIR);
        set(a0[3][3] - 1, 0, tb, vers(OmbreBlocs.MONTE_CHARGE, 'a'));
        // Le mobilier de la ville, façade par façade
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                if (!(i == 3 && j == 3)) mobilier(a0[i][j], b0[i][j], a1[i][j], b1[i][j], h[i][j]);
        // Puits dans les rues
        for (int k = 0, n = 0; k < 60 && n < 6; k++) {
            int a = A0 + hasard.nextInt(N * CASE), b = BV + hasard.nextInt(N * CASE);
            if (w.getBlockState(pos(a, 0, b)).isAir() && w.getBlockState(pos(a, 1, b)).isAir()) { set(a, 0, b, OmbreBlocs.PUITS); n++; }
        }
        // Lampadaires dans les rues
        for (int k = 0; k < 30; k++) {
            int a = A0 + hasard.nextInt(N * CASE), b = BV + hasard.nextInt(N * CASE);
            if (!w.getBlockState(pos(a, 0, b)).isAir() || !w.getBlockState(pos(a, 4, b)).isAir()) continue;
            boite(a, 0, b, a, 2, b, OmbreBlocs.LAMPADAIRE);
            set(a, 3, b, OmbreBlocs.TETE_LAMPADAIRE);
        }
        int bord = a1[3][3];
        boite(bord + 1, 23, tb, bord + 3, 23, tb, OmbreBlocs.POUTRE.getDefaultState()
                .with(Properties.HORIZONTAL_AXIS, droite.getAxis()));
        boite(bord + 2, 0, tb - 1, bord + 4, 1, tb + 1, OmbreBlocs.MEULE);
    }

    /**
     * Sur les façades d'une maison : une corniche à mi-hauteur, une enseigne au-dessus d'un auvent,
     * et parfois un échafaudage contre un angle.
     */
    private void mobilier(int a0, int b0, int a1, int b1, int h) {
        // Façade avant (côté -b) : le mur est vers +b
        if (h >= 6) boite(a0, h / 2, b0 - 1, a1, h / 2, b0 - 1, vers(OmbreBlocs.CORNICHE, 'b'));
        if (hasard.nextBoolean()) {
            int a = a0 + 1 + hasard.nextInt(Math.max(1, a1 - a0 - 2));
            set(a, 3, b0 - 1, vers(OmbreBlocs.ENSEIGNE, 'b'));
            boite(a0 + 1, 2, b0 - 1, a1 - 1, 2, b0 - 1, vers(OmbreBlocs.AUVENT, 'b'));
        }
        // Une cabine à rideau contre la façade, ouverte sur la rue
        if (hasard.nextInt(3) == 0 && w.getBlockState(pos(a1 - 1, 0, b0 - 1)).isAir())
            set(a1 - 1, 0, b0 - 1, vers(OmbreBlocs.CABINE, 'B'));
        // Façade droite (côté +a) : le mur est vers -a
        if (h >= 8 && hasard.nextBoolean()) boite(a1 + 1, h - 3, b0, a1 + 1, h - 3, b1, vers(OmbreBlocs.CORNICHE, 'A'));
        if (hasard.nextInt(3) == 0) set(a1 + 1, 4, (b0 + b1) / 2, vers(OmbreBlocs.ENSEIGNE, 'A'));
        // Façade arrière (côté +b) : un auvent
        if (hasard.nextInt(3) == 0) boite(a0 + 1, 2, b1 + 1, a1 - 1, 2, b1 + 1, vers(OmbreBlocs.AUVENT, 'B'));
        // Échafaudage contre l'angle gauche-avant : 2 à 3 étages
        if (hasard.nextInt(3) == 0) {
            int n = 2 + hasard.nextInt(2);
            for (int k = 0; k < n; k++) set(a0 - 1, k, b0 - 1, OmbreBlocs.ECHAFAUDAGE);
        }
    }

    /** Une maison pleine, avec des bandeaux de bois tous les 4 blocs et quelques « fenêtres ». */
    private void maison(int a0, int b0, int a1, int b1, int h, Block mur, boolean tour) {
        boite(a0, 0, b0, a1, h - 1, b1, mur);
        for (int y = 3; y < h - 1; y += 4) boite(a0, y, b0, a1, y, b1, Blocks.STRIPPED_OAK_LOG);
        for (int y = 1; y < h - 1; y += 4)
            for (int a = a0 + 2; a < a1 - 1; a += 3) {
                set(a, y + 1, b0, Blocks.GLASS_PANE);
                set(a, y + 1, b1, Blocks.GLASS_PANE);
            }
        // Sur le toit : une lanterne, parfois un buisson (cachette) ou un rebord de dalles
        set(a0, h, b0, Blocks.LANTERN);
        if (!tour && hasard.nextInt(3) == 0)
            set(a1 - 1, h, b1 - 1, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
        if (tour) boite(a0, h - 1, b0, a1, h - 1, b1, Blocks.CHISELED_STONE_BRICKS);
    }
}
