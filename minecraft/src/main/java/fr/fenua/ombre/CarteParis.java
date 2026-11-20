package fr.fenua.ombre;

import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.bloc.PoteauTyrolienne;
import fr.fenua.ombre.entite.Entites;
import fr.fenua.ombre.entite.Passant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

/**
 * /ombre paris : un quartier de Paris en 1889, de 144 × 144 blocs, au nord-est du joueur.
 *
 * Une grille de 4 × 4 îlots de 26 blocs, séparés par des rues de 8 (chaussée pavée, trottoirs).
 *  - Douze îlots haussmanniens : façades en pierre de Paris, devantures et enseignes, balcons filants
 *    au 2ᵉ et au dernier étage, volets, corniche, gouttières, mansardes de zinc ou d'ardoise avec
 *    lucarnes et cheminées ; une cour au milieu (puits, linge, gouttières).
 *  - La place du marché (îlot 1,2) : étals, fontaine, arbres, et un clocher (gargouilles = point de vue).
 *  - Le square de la tour (îlot 2,1) : la tour de fer (67 blocs), deux plates-formes, un perchoir
 *    au sommet, des meules au pied, et une tyrolienne de la 2ᵉ plate-forme jusqu'au clocher.
 *  - Une foule, des gardiens dans les rues, des gendarmes sur les toits, et la cible sur la tour.
 */
public final class CarteParis {
    private static final int ILOT = 26, RUE = 8, PAS = ILOT + RUE, N = 4, TAILLE = N * PAS + RUE;
    private static final int HAUT_MAX = 32;

    private final ServerWorld w;
    private final int ox, oy, oz;            // coin nord-ouest de la carte, au niveau du sol
    private final Random r = new Random(1889);
    private final int[][] hauteur = new int[TAILLE][TAILLE];   // hauteur des toits (pour les poutres)

    private CarteParis(ServerWorld w, int ox, int oy, int oz) { this.w = w; this.ox = ox; this.oy = oy; this.oz = oz; }

    public static int construire(ServerCommandSource src) {
        BlockPos p = BlockPos.ofFloored(src.getPosition());
        CarteParis c = new CarteParis(src.getWorld(), p.getX() - 4, p.getY(), p.getZ() - (TAILLE - 4));
        c.batir();
        if (src.getEntity() instanceof ServerPlayerEntity j) {
            BlockPos d = new BlockPos(c.ox + 4, c.oy, c.oz + TAILLE - 4);
            j.teleport(src.getWorld(), d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 180f, 0f);
            j.setSpawnPoint(src.getWorld().getRegistryKey(), d, 180f, true, false);
            Kit.donner(j);
        }
        src.sendFeedback(() -> Text.literal("§6Paris, 1889.§r Un quartier de 144 × 144 blocs s'élève au nord-est.\n"
                + "§eCible§r : le comte de Valmont, sur la première plate-forme de la tour de fer (il brille en or avec l'Instinct).\n"
                + "§eConseils§r : fonds-toi dans la foule, grimpe par les gouttières, passe de toit en toit par les poutres, "
                + "synchronise au clocher (gargouilles) ou au sommet de la tour, et redescends en tyrolienne.\n"
                + "§7/time set night pour les réverbères. Touche H : toutes les commandes."), true);
        return 1;
    }

    // ───────────────────────── outils ─────────────────────────

    private static Block b(String nom) { return Registries.BLOCK.get(Identifier.of(Ombre.MOD_ID, nom)); }

    private void set(int x, int y, int z, BlockState s) {
        w.setBlockState(new BlockPos(ox + x, oy + y, oz + z), s, Block.NOTIFY_LISTENERS);
    }
    private void set(int x, int y, int z, Block bl) { set(x, y, z, bl.getDefaultState()); }
    private boolean vide(int x, int y, int z) { return w.getBlockState(new BlockPos(ox + x, oy + y, oz + z)).isAir(); }

    private void boite(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }
    private void boite(int x0, int y0, int z0, int x1, int y1, int z1, Block bl) { boite(x0, y0, z0, x1, y1, z1, bl.getDefaultState()); }

    private static BlockState vers(Block bl, Direction d) {
        BlockState s = bl.getDefaultState();
        return s.contains(Properties.HORIZONTAL_FACING) ? s.with(Properties.HORIZONTAL_FACING, d) : s;
    }

    private <T> T hasard(T[] t) { return t[r.nextInt(t.length)]; }

    // ───────────────────────── construction ─────────────────────────

    private void batir() {
        aplanir();
        rues();
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) {
                int bx = RUE + i * PAS, bz = RUE + j * PAS;
                if (i == 1 && j == 2) marche(bx, bz);
                else if (i == 2 && j == 1) tour(bx, bz);
                else ilot(bx, bz);
            }
        poutresEtLinge();
        mobilierDeRue();
        tyrolienne();
        gens();
    }

    private void aplanir() {
        for (int x = 0; x < TAILLE; x++)
            for (int z = 0; z < TAILLE; z++) {
                for (int y = 0; y <= HAUT_MAX; y++)
                    if (!vide(x, y, z)) set(x, y, z, Blocks.AIR);
                for (int y = -4; y <= -2; y++) set(x, y, z, Blocks.DIRT);
            }
    }

    /** Chaussée pavée au milieu, trottoirs de 2 blocs le long des îlots. */
    private void rues() {
        BlockState pave = b("pave_parisien").getDefaultState(), trottoir = b("trottoir").getDefaultState();
        for (int x = 0; x < TAILLE; x++)
            for (int z = 0; z < TAILLE; z++) {
                int tx = x % PAS, tz = z % PAS;
                boolean rueX = tx < RUE, rueZ = tz < RUE;
                if (!rueX && !rueZ) continue;
                boolean bordX = rueX && (tx <= 1 || tx >= RUE - 2), bordZ = rueZ && (tz <= 1 || tz >= RUE - 2);
                boolean trot = (rueX && !rueZ && bordX) || (rueZ && !rueX && bordZ) || (rueX && rueZ && bordX && bordZ);
                set(x, -1, z, trot ? trottoir : pave);
            }
    }

    // ───────────────────────── les îlots haussmanniens ─────────────────────────

    private void ilot(int bx, int bz) {
        // Le sol de la cour
        boite(bx, -1, bz, bx + ILOT - 1, -1, bz + ILOT - 1, b("pave_parisien"));
        // Les quatre côtés, découpés en immeubles de 6 à 9 de large
        rangee(bx, bz, ILOT, Direction.NORTH);                 // côté nord (façades sur la rue du nord)
        rangee(bx, bz + ILOT - 8, ILOT, Direction.SOUTH);
        rangee(bx, bz + 8, ILOT - 16, Direction.WEST);
        rangee(bx + ILOT - 8, bz + 8, ILOT - 16, Direction.EAST);
        cour(bx + 8, bz + 8);
    }

    /** Une rangée d'immeubles le long d'un côté d'îlot. (x0, z0) : coin nord-ouest de la bande de 8 de profondeur. */
    private void rangee(int x0, int z0, int longueur, Direction facade) {
        boolean ns = facade.getAxis() == Direction.Axis.Z;
        int u = 0;
        while (u < longueur) {
            int l = Math.min(longueur - u, 6 + r.nextInt(4));
            if (longueur - u - l < 6 && longueur - u - l > 0) l = longueur - u;
            int etages = 3 + r.nextInt(3);
            if (ns) immeuble(x0 + u, z0, x0 + u + l - 1, z0 + 7, facade, etages);
            else immeuble(x0, z0 + u, x0 + 7, z0 + u + l - 1, facade, etages);
            u += l;
        }
    }

    private static final String[] MURS = {"pierre_de_paris", "pierre_de_paris", "pierre_de_paris_lisse", "pierre_de_paris", "brique_parisienne", "crepi"};
    private static final String[] VOLETS = {"volets", "volets_vert", "volets_bleu", "volets_gris", "volets_blanc", "volets_brun", "volets_rouge"};
    private static final String[] AUVENTS = {"auvent", "auvent_raye_rouge_blanc", "auvent_raye_vert_blanc", "auvent_raye_bleu_blanc", "auvent_green",
            "auvent_blue", "auvent_raye_jaune_blanc", "auvent_brown", "auvent_raye_noir_blanc", "auvent_raye_rouge_jaune"};
    private static final String[] ENSEIGNES = {"enseigne_boulangerie", "enseigne_cafe", "enseigne_pharmacie", "enseigne_horloger", "enseigne_librairie",
            "enseigne_boucherie", "enseigne_chapelier", "enseigne_barbier", "enseigne_cordonnier", "enseigne_fleuriste", "enseigne_tailleur", "enseigne_marchand_vin"};
    private static final String[] AFFICHES = {"affiche_theatre", "affiche_cirque", "affiche_exposition", "affiche_bal", "affiche_chemin_de_fer",
            "affiche_ballon", "affiche_recherche", "affiche_chocolat", "affiche_opera", "affiche_savon", "affiche_velocipede", "affiche_proclamation"};

    /**
     * Un immeuble plein (x0..x1, z0..z1), sa façade côté « facade », « etages » étages au-dessus
     * d'un rez-de-chaussée de 4 blocs, puis une mansarde.
     */
    private void immeuble(int x0, int z0, int x1, int z1, Direction facade, int etages) {
        int h = 4 + etages * 3;
        Block mur = b(hasard(MURS));
        boite(x0, 0, z0, x1, h - 1, z1, mur);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) hauteur[x][z] = h;

        // La façade : on la parcourt de gauche à droite
        boolean ns = facade.getAxis() == Direction.Axis.Z;
        int debut = ns ? x0 : z0, fin = ns ? x1 : z1;
        int bord = switch (facade) { case NORTH -> z0; case SOUTH -> z1; case WEST -> x0; default -> x1; };
        int dx = facade.getOffsetX(), dz = facade.getOffsetZ();
        Direction versMur = facade.getOpposite();
        Block volets = b(hasard(VOLETS)), auvent = b(hasard(AUVENTS)), enseigne = b(hasard(ENSEIGNES));
        Block balcon = r.nextInt(5) == 0 ? b("balcon_vert_de_gris") : OmbreBlocs.BALCON;
        Block corniche = r.nextBoolean() ? OmbreBlocs.CORNICHE : b("corniche_sandstone");
        boolean devantureVerte = r.nextBoolean();
        int porte = debut + 1 + r.nextInt(Math.max(1, fin - debut - 1));
        int posEnseigne = debut + 1 + r.nextInt(Math.max(1, fin - debut - 1));
        for (int u = debut; u <= fin; u++) {
            int x = ns ? u : bord, z = ns ? bord : u;
            int xo = x + dx, zo = z + dz;                       // la case devant la façade, dans la rue
            boolean angle = u == debut;
            // Rez-de-chaussée : devanture, vitrines, porte
            for (int y = 0; y <= 2; y++) set(x, y, z, b(devantureVerte ? "devanture" : "devanture_rouge"));
            if (u == porte) { set(x, 0, z, Blocks.AIR); set(x, 1, z, Blocks.AIR); }
            else if ((u - debut) % 2 == 1) { set(x, 1, z, Blocks.GLASS_PANE); set(x, 2, z, Blocks.GLASS_PANE); }
            set(x, 3, z, b("pierre_de_paris_sculptee"));
            if (angle) {
                for (int y = 0; y < h; y++) set(xo, y, zo, vers(OmbreBlocs.GOUTTIERE, versMur));
                continue;
            }
            if (u == posEnseigne) set(xo, 3, zo, vers(enseigne, versMur));
            else if (u != porte && r.nextInt(3) > 0) set(xo, 3, zo, vers(auvent, versMur));
            else if (r.nextInt(4) == 0) set(xo, 1, zo, vers(b(hasard(AFFICHES)), versMur));
            // Étages : fenêtres, balcons au 2ᵉ et au dernier étage, volets ailleurs
            for (int k = 0; k < etages; k++) {
                int yb = 4 + k * 3;
                boolean fenetre = (u - debut) % 2 == 1;
                if (fenetre) { set(x, yb + 1, z, Blocks.GLASS_PANE); set(x, yb + 2, z, Blocks.GLASS_PANE); }
                if (k == 1 || k == etages - 1) set(xo, yb, zo, vers(balcon, versMur));
                else if (fenetre && r.nextInt(3) > 0) { set(xo, yb + 1, zo, vers(volets, versMur)); set(xo, yb + 2, zo, vers(volets, versMur)); }
            }
            set(xo, h - 1, zo, vers(corniche, versMur));
        }
        toit(x0, z0, x1, z1, h, facade);
    }

    /** Une mansarde : un rang d'escaliers tout autour, un second rang en retrait, des lucarnes, des cheminées. */
    private void toit(int x0, int z0, int x1, int z1, int h, Direction facade) {
        boolean zinc = r.nextBoolean();
        Block plein = b(zinc ? "zinc" : "ardoise"), marche = b(zinc ? "zinc_escalier" : "ardoise_escalier");
        for (int etage = 0; etage < 2; etage++) {
            int y = h + etage, a0 = x0 + etage, a1 = x1 - etage, c0 = z0 + etage, c1 = z1 - etage;
            if (a0 >= a1 || c0 >= c1) break;
            boite(a0 + 1, y, c0 + 1, a1 - 1, y, c1 - 1, plein);
            for (int x = a0; x <= a1; x++) {
                set(x, y, c0, marche.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
                set(x, y, c1, marche.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            }
            for (int z = c0 + 1; z <= c1 - 1; z++) {
                set(a0, y, z, marche.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
                set(a1, y, z, marche.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            }
        }
        // Lucarnes sur le versant de la façade
        boolean ns = facade.getAxis() == Direction.Axis.Z;
        int bord = switch (facade) { case NORTH -> z0; case SOUTH -> z1; case WEST -> x0; default -> x1; };
        int debut = ns ? x0 : z0, fin = ns ? x1 : z1;
        for (int u = debut + 2; u <= fin - 2; u += 3) {
            if (ns) set(u, h, bord, vers(OmbreBlocs.LUCARNE, facade));
            else set(bord, h, u, vers(OmbreBlocs.LUCARNE, facade));
        }
        // Cheminées sur les murs mitoyens
        Block chem = r.nextBoolean() ? OmbreBlocs.CHEMINEE : b("cheminee_terre_cuite");
        int cx0 = ns ? x0 + 1 : (x0 + x1) / 2, cz0 = ns ? (z0 + z1) / 2 : z0 + 1;
        int cx1 = ns ? x1 - 1 : (x0 + x1) / 2, cz1 = ns ? (z0 + z1) / 2 : z1 - 1;
        for (int[] c : new int[][]{{cx0, cz0}, {cx1, cz1}}) {
            set(c[0], h + 2, c[1], b("brique_parisienne"));
            set(c[0], h + 3, c[1], chem);
        }
        // Parfois un drapeau sur le toit
        if (r.nextInt(6) == 0) set((x0 + x1) / 2, h + 2, (z0 + z1) / 2, OmbreBlocs.DRAPEAU);
    }

    /** La cour : un puits, un arbre, du linge entre les façades, des gouttières pour grimper. */
    private void cour(int cx, int cz) {
        set(cx + 4, 0, cz + 4, OmbreBlocs.PUITS);
        arbre(cx + 7, cz + 7);
        for (int x = cx; x <= cx + 9; x++) set(x, 7, cz + 2, vers(OmbreBlocs.CORDE_LINGE, Direction.NORTH));
        for (int y = 0; y < 13; y++) {
            set(cx, y, cz + 5, vers(OmbreBlocs.GOUTTIERE, Direction.WEST));
            set(cx + 9, y, cz + 6, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
        }
        if (r.nextBoolean()) boite(cx + 1, 0, cz + 7, cx + 2, 1, cz + 8, OmbreBlocs.MEULE);
    }

    private void arbre(int x, int z) {
        boite(x, 0, z, x, 4, z, Blocks.OAK_LOG);
        BlockState f = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int a = -2; a <= 2; a++) for (int c = -2; c <= 2; c++) for (int y = 3; y <= 6; y++) {
            if (Math.abs(a) + Math.abs(c) > (y == 6 ? 1 : 3) || (a == 0 && c == 0 && y <= 4)) continue;
            set(x + a, y, z + c, f);
        }
    }

    // ───────────────────────── la place du marché et son clocher ─────────────────────────

    private static final String[] ETALS = {"etal", "etal_fruits", "etal_poisson", "etal_fleurs", "etal_pain", "etal_etoffes", "etal_legumes"};

    private void marche(int bx, int bz) {
        boite(bx, -1, bz, bx + ILOT - 1, -1, bz + ILOT - 1, b("pave_parisien"));
        // Le clocher, au nord de la place
        int x0 = bx + 10, x1 = bx + 15, z0 = bz, z1 = bz + 5, h = 30;
        boite(x0, 0, z0, x1, h - 1, z1, b("pierre_de_paris"));
        for (int y = 5; y < h; y += 6) boite(x0, y, z0, x1, y, z1, b("pierre_de_paris_sculptee"));
        boite(x0 + 2, 23, z1, x0 + 3, 26, z1, Blocks.AIR);                       // les ouïes du clocher
        set(x0 + 2, 20, z1 + 1, vers(OmbreBlocs.HORLOGE, Direction.NORTH));
        set(x0 + 3, 20, z1 + 1, vers(OmbreBlocs.HORLOGE, Direction.NORTH));
        for (int x = x0; x <= x1; x++) { set(x, h, z0, Blocks.STONE_BRICK_WALL); set(x, h, z1, Blocks.STONE_BRICK_WALL); }
        for (int z = z0; z <= z1; z++) { set(x0, h, z, Blocks.STONE_BRICK_WALL); set(x1, h, z, Blocks.STONE_BRICK_WALL); }
        // Gargouilles aux angles, tournées vers la place : ce sont des points de vue
        set(x0 - 1, h - 1, z1, vers(OmbreBlocs.GARGOUILLE, Direction.EAST));
        set(x1 + 1, h - 1, z1, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        set(x0 - 1, h - 1, z0, vers(OmbreBlocs.GARGOUILLE, Direction.EAST));
        set(x1 + 1, h - 1, z0, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        for (int y = 0; y < h; y++) set(x0 - 1, y, z0 + 2, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
        // Le saut de la foi : une charrette et des meules au pied du clocher
        boite(x0 + 1, 0, z1 + 2, x0 + 4, 1, z1 + 3, OmbreBlocs.MEULE);
        set(x0 + 5, 0, z1 + 2, vers(OmbreBlocs.CHARRETTE, Direction.NORTH));
        // Les étals en cercle autour de la fontaine
        int cx = bx + 13, cz = bz + 15;
        set(cx, 0, cz, OmbreBlocs.FONTAINE);
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI * 2 / 12;
            int x = cx + (int) Math.round(Math.cos(a) * 7), z = cz + (int) Math.round(Math.sin(a) * 6);
            if (z <= z1 + 3) continue;
            Direction d = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? (Math.cos(a) > 0 ? Direction.WEST : Direction.EAST)
                    : (Math.sin(a) > 0 ? Direction.NORTH : Direction.SOUTH);
            set(x, 0, z, vers(b(ETALS[k % ETALS.length]), d));
        }
        for (int[] p : new int[][]{{bx + 2, bz + 9}, {bx + 23, bz + 9}, {bx + 2, bz + 23}, {bx + 23, bz + 23}}) arbre(p[0], p[1]);
        set(bx + 6, 0, bz + 22, vers(OmbreBlocs.BANC, Direction.SOUTH));
        set(bx + 19, 0, bz + 22, vers(b("banc_spruce"), Direction.SOUTH));
        set(bx + 13, 0, bz + 24, vers(b("banc_birch"), Direction.SOUTH));
        set(bx + 1, 0, bz + 15, OmbreBlocs.COLONNE);
        set(bx + 24, 0, bz + 15, OmbreBlocs.COLONNE);
        set(bx + 4, 0, bz + 13, vers(OmbreBlocs.CABINE, Direction.EAST));
        set(bx + 21, 0, bz + 13, vers(b("cabine_red"), Direction.WEST));
        clocherX = (x0 + x1) / 2; clocherZ = (z0 + z1) / 2; clocherH = h;
    }

    private int clocherX, clocherZ, clocherH, tourX, tourZ;

    // ───────────────────────── la tour de fer ─────────────────────────

    private void tour(int bx, int bz) {
        boite(bx, -1, bz, bx + ILOT - 1, -1, bz + ILOT - 1, Blocks.GRASS_BLOCK);
        for (int i = 0; i < ILOT; i++) { set(bx + i, -1, bz + 12, Blocks.GRAVEL); set(bx + 12, -1, bz + i, Blocks.GRAVEL); }
        Block fer = OmbreBlocs.TREILLIS;
        int c0 = bx + 2, c1 = bx + ILOT - 3, d0 = bz + 2, d1 = bz + ILOT - 3;     // emprise des pieds
        // 1er étage : quatre pieds de 3 × 3 qui se resserrent jusqu'à y = 20
        for (int y = 0; y < 20; y++) {
            int e = y * 6 / 20;
            for (int a = 0; a < 3; a++) for (int c = 0; c < 3; c++) {
                set(c0 + e + a, y, d0 + e + c, fer); set(c1 - e - a, y, d0 + e + c, fer);
                set(c0 + e + a, y, d1 - e - c, fer); set(c1 - e - a, y, d1 - e - c, fer);
            }
        }
        // 1re plate-forme (y = 20) : un carré de 16 avec un trou au milieu, garde-corps
        int p0x = c0 + 3, p1x = c1 - 3, p0z = d0 + 3, p1z = d1 - 3;
        boite(p0x, 20, p0z, p1x, 20, p1z, Blocks.SPRUCE_PLANKS);
        boite(p0x + 5, 20, p0z + 5, p1x - 5, 20, p1z - 5, Blocks.AIR);
        for (int x = p0x; x <= p1x; x++) { set(x, 21, p0z, Blocks.IRON_BARS); set(x, 21, p1z, Blocks.IRON_BARS); }
        for (int z = p0z; z <= p1z; z++) { set(p0x, 21, z, Blocks.IRON_BARS); set(p1x, 21, z, Blocks.IRON_BARS); }
        // 2e étage : les pieds se rejoignent jusqu'à y = 40
        int q0x = p0x + 4, q1x = p1x - 4, q0z = p0z + 4, q1z = p1z - 4;
        for (int y = 21; y < 40; y++) {
            int e = (y - 21) * 3 / 19;
            for (int a = 0; a < 2; a++) for (int c = 0; c < 2; c++) {
                set(q0x + e + a, y, q0z + e + c, fer); set(q1x - e - a, y, q0z + e + c, fer);
                set(q0x + e + a, y, q1z - e - c, fer); set(q1x - e - a, y, q1z - e - c, fer);
            }
        }
        int r0x = q0x + 2, r1x = q1x - 2, r0z = q0z + 2, r1z = q1z - 2;
        boite(r0x, 40, r0z, r1x, 40, r1z, Blocks.SPRUCE_PLANKS);
        for (int x = r0x; x <= r1x; x++) { set(x, 41, r0z, Blocks.IRON_BARS); set(x, 41, r1z, Blocks.IRON_BARS); }
        for (int z = r0z; z <= r1z; z++) { set(r0x, 41, z, Blocks.IRON_BARS); set(r1x, 41, z, Blocks.IRON_BARS); }
        set(r0x, 41, r1z, Blocks.AIR);
        // La flèche, jusqu'au perchoir du sommet
        int sx = (r0x + r1x) / 2, sz = (r0z + r1z) / 2;
        for (int y = 41; y < 66; y++) {
            boite(sx, y, sz, sx + (y < 56 ? 1 : 0), y, sz + (y < 56 ? 1 : 0), fer);
        }
        boite(sx - 1, 66, sz - 1, sx + 1, 66, sz + 1, Blocks.SPRUCE_PLANKS);
        set(sx, 67, sz, OmbreBlocs.PERCHOIR);
        set(sx + 1, 67, sz + 1, OmbreBlocs.DRAPEAU);
        // Des meules au pied de chaque face : on saute de la 1re plate-forme
        boite(bx + 11, 0, bz + 4, bx + 14, 1, bz + 6, OmbreBlocs.MEULE);
        boite(bx + 11, 0, bz + ILOT - 7, bx + 14, 1, bz + ILOT - 5, OmbreBlocs.MEULE);
        boite(bx + 4, 0, bz + 11, bx + 6, 1, bz + 14, OmbreBlocs.MEULE);
        boite(bx + ILOT - 7, 0, bz + 11, bx + ILOT - 5, 1, bz + 14, OmbreBlocs.MEULE);
        // Des bancs dans le square
        set(bx + 1, 0, bz + 8, vers(OmbreBlocs.BANC, Direction.WEST));
        set(bx + ILOT - 2, 0, bz + 17, vers(b("banc_dark_oak"), Direction.EAST));
        tourX = r0x; tourZ = r1z; deuxiemeEtage = 40;
        plateforme1 = new int[]{p0x, p0z, p1x, p1z};
    }

    private int deuxiemeEtage;
    private int[] plateforme1;

    /** La tyrolienne de la 2e plate-forme de la tour jusqu'au sommet du clocher. */
    private void tyrolienne() {
        BlockPos a = new BlockPos(ox + tourX + 1, oy + deuxiemeEtage + 1, oz + tourZ - 1);
        BlockPos c = new BlockPos(ox + clocherX, oy + clocherH, oz + clocherZ);
        w.setBlockState(a, OmbreBlocs.POTEAU.getDefaultState(), Block.NOTIFY_ALL);
        w.setBlockState(c, OmbreBlocs.POTEAU.getDefaultState(), Block.NOTIFY_ALL);
        PoteauTyrolienne.relier(w, a, c);
    }

    // ───────────────────────── poutres, linge, mobilier des rues ─────────────────────────

    /** Des poutres d'un toit à l'autre au-dessus des rues, et du linge tendu plus bas. */
    private void poutresEtLinge() {
        for (int k = 1; k < N; k++) {
            int rue = k * PAS;                                     // une rue nord-sud : x de rue à rue + 7
            for (int j = 0; j < N; j++) {
                int z = RUE + j * PAS + 4 + r.nextInt(ILOT - 8);
                int hg = hauteur[rue - 1][z], hd = hauteur[rue + RUE][z];
                if (hg > 0 && hd > 0 && r.nextInt(3) > 0) {
                    int y = Math.min(hg, hd) - 1;
                    for (int x = rue; x < rue + RUE; x++) set(x, y, z, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
                }
                int z2 = RUE + j * PAS + 2 + r.nextInt(ILOT - 4);
                if (hauteur[rue - 1][z2] > 9 && hauteur[rue + RUE][z2] > 9 && r.nextBoolean())
                    for (int x = rue; x < rue + RUE; x++) set(x, 9, z2, vers(OmbreBlocs.CORDE_LINGE, Direction.NORTH));
            }
            int rueZ = k * PAS;                                    // une rue est-ouest
            for (int i = 0; i < N; i++) {
                int x = RUE + i * PAS + 4 + r.nextInt(ILOT - 8);
                int hn = hauteur[x][rueZ - 1], hs = hauteur[x][rueZ + RUE];
                if (hn > 0 && hs > 0 && r.nextInt(3) > 0) {
                    int y = Math.min(hn, hs) - 1;
                    for (int z = rueZ; z < rueZ + RUE; z++) set(x, y, z, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.Z));
                }
            }
        }
    }

    /** Réverbères, colonnes d'affiches, fiacres, charrettes de foin et bancs. */
    private void mobilierDeRue() {
        for (int k = 0; k <= N; k++) {
            int rue = k * PAS;
            for (int t = 3; t < TAILLE - 3; t += 9) {
                if (t % PAS < RUE) continue;                       // pas au milieu d'un carrefour
                lampadaire(rue + 1, t + (k % 2) * 4);
                lampadaire(t + (k % 2) * 4, rue + 1);
                if (r.nextInt(5) == 0 && vide(rue + 4, 0, t + 2)) set(rue + 4, 0, t + 2, vers(OmbreBlocs.FIACRE, Direction.NORTH));
                if (r.nextInt(6) == 0 && vide(t + 2, 0, rue + 3)) set(t + 2, 0, rue + 3, vers(OmbreBlocs.FIACRE, Direction.EAST));
                if (r.nextInt(4) == 0 && vide(rue + RUE - 2, 0, t + 5)) set(rue + RUE - 2, 0, t + 5, vers(OmbreBlocs.CHARRETTE, Direction.NORTH));
                if (r.nextInt(5) == 0 && vide(t + 5, 0, rue + RUE - 2)) set(t + 5, 0, rue + RUE - 2, vers(b(hasard(new String[]{"banc", "banc_spruce", "banc_dark_oak"})), Direction.SOUTH));
            }
        }
        for (int i = 0; i <= N; i++) for (int j = 0; j <= N; j++)
            if (r.nextInt(3) == 0) set(i * PAS + 1, 0, j * PAS + 1, OmbreBlocs.COLONNE);
    }

    private void lampadaire(int x, int z) {
        if (x >= TAILLE || z >= TAILLE || !vide(x, 0, z) || !vide(x, 3, z)) return;
        boite(x, 0, z, x, 2, z, OmbreBlocs.LAMPADAIRE);
        set(x, 3, z, OmbreBlocs.TETE_LAMPADAIRE);
    }

    // ───────────────────────── la foule, les gardes, la cible ─────────────────────────

    private void gens() {
        Scoreboard sb = w.getScoreboard();
        Team rouges = equipe(sb, "ombre_gardes", Formatting.RED), ors = equipe(sb, "ombre_cibles", Formatting.GOLD);
        // La foule : des groupes sur les trottoirs, et sur la place
        for (int g = 0; g < 10; g++) {
            int k = r.nextInt(N + 1), t = RUE + r.nextInt(TAILLE - 2 * RUE);
            boolean ns = r.nextBoolean();
            int x = ns ? k * PAS + 1 + r.nextInt(6) : t, z = ns ? t : k * PAS + 1 + r.nextInt(6);
            for (int i = 0; i < 3 + r.nextInt(2); i++) passant(x + r.nextInt(3) - 1, z + r.nextInt(3) - 1);
        }
        int mx = RUE + PAS + 13, mz = RUE + 2 * PAS + 15;
        for (int i = 0; i < 14; i++) passant(mx - 7 + r.nextInt(14), mz - 5 + r.nextInt(10));
        // Gardiens de la paix dans les rues, par deux
        for (int g = 0; g < 5; g++) {
            int k = 1 + r.nextInt(N - 1), t = RUE + 4 + r.nextInt(TAILLE - 2 * RUE - 8);
            boolean ns = r.nextBoolean();
            int x = ns ? k * PAS + 4 : t, z = ns ? t : k * PAS + 4;
            garde(Entites.GARDIEN, x, 0, z, 10, "Gardien de la paix", rouges, false);
            garde(Entites.GARDIEN, x + 1, 0, z + 1, 10, "Gardien de la paix", rouges, false);
        }
        // Gendarmes sur les toits
        int poses = 0;
        for (int essai = 0; essai < 200 && poses < 6; essai++) {
            int x = r.nextInt(TAILLE), z = r.nextInt(TAILLE), h = hauteur[x][z];
            if (h > 0 && vide(x, h + 2, z) && vide(x, h + 3, z) && !vide(x, h + 1, z)) {
                garde(Entites.GENDARME, x, h + 2, z, 3, "Gendarme", rouges, false);
                poses++;
            }
        }
        garde(Entites.GENDARME, clocherX, clocherH + 1, clocherZ - 1, 1, "Gendarme du clocher", rouges, false);
        // La cible et sa garde, sur la 1re plate-forme de la tour
        int[] p = plateforme1;
        garde(Entites.OFFICIER, p[0] + 1, 21, p[1] + 1, 2, "Comte de Valmont", ors, true);
        garde(Entites.GENDARME, p[2] - 1, 21, p[1] + 1, 2, "Gendarme de la tour", rouges, false);
        garde(Entites.GARDIEN, p[0] + 1, 21, p[3] - 1, 2, "Gardien de la tour", rouges, false);
    }

    private void passant(int x, int z) {
        Passant p = Entites.PASSANT.create(w);
        if (p == null || x < 0 || z < 0 || x >= TAILLE || z >= TAILLE || !vide(x, 0, z)) return;
        BlockPos pos = new BlockPos(ox + x, oy, oz + z);
        p.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360, 0);
        p.initialize(w, w.getLocalDifficulty(pos), SpawnReason.COMMAND, null);
        p.setPersistent();
        p.setPositionTarget(pos, 14);
        w.spawnEntity(p);
    }

    private static Team equipe(Scoreboard sb, String nom, Formatting c) {
        Team t = sb.getTeam(nom);
        if (t == null) t = sb.addTeam(nom);
        t.setColor(c);
        return t;
    }

    private <T extends PathAwareEntity> void garde(EntityType<T> type, int x, int y, int z, int rayon, String nom, Team equipe, boolean cible) {
        T g = type.create(w);
        if (g == null) return;
        BlockPos pos = new BlockPos(ox + x, oy + y, oz + z);
        g.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360, 0);
        g.initialize(w, w.getLocalDifficulty(pos), SpawnReason.COMMAND, null);
        g.setPersistent();
        g.setCustomName(Text.literal(nom));
        g.setPositionTarget(pos, rayon);
        g.addCommandTag(Gardes.GARDE);
        if (cible) g.addCommandTag(Gardes.CIBLE);
        w.spawnEntityAndPassengers(g);
        w.getScoreboard().addScoreHolderToTeam(g.getNameForScoreboard(), equipe);
    }
}
