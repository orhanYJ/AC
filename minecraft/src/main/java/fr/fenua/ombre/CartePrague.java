package fr.fenua.ombre;

import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.bloc.PoteauTyrolienne;
import fr.fenua.ombre.entite.Entites;
import fr.fenua.ombre.entite.Figure;
import fr.fenua.ombre.entite.Passant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.IronGolemEntity;
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
 * /ombre prague : la Vieille-Ville de Prague en 1600, au nord-est du joueur (monde plat conseillé).
 *
 *  x (est) : 0 → 131 la ville (4 × 4 îlots de 24, rues de 7), 131 → 153 la Vltava, 153 → 166 l'autre rive.
 *  z (sud) : 0 → 131.
 *  - Îlots de maisons bourgeoises : façades pastel ou en sgraffite, pignon sur rue, toit de tuiles rouges,
 *    lanternes à potence, enseignes, cours (puits, linge, gouttières).
 *  - La place de la Vieille-Ville (îlots 1,1 et 2,1) : marché, fontaine, et la tour de l'horloge
 *    astronomique (gargouilles = points de vue), où se tiennent Tycho Brahe et Johannes Kepler.
 *  - L'église de Týn (îlot 3,1) : nef et deux tours à flèches ; l'alchimiste de la Confrérie dans la tour nord.
 *  - La Vltava et le pont Charles (statues), entre la tour du pont (le grand maître de la Confrérie,
 *    au sommet) et la tour de l'autre rive, où attend l'empereur Rodolphe II.
 *  - Au nord, le rabbin Loew et son Golem. Une foule, des hallebardiers, des arbalétriers sur les toits.
 */
public final class CartePrague {
    private static final int ILOT = 24, RUE = 7, PAS = ILOT + RUE, N = 5, VILLE = N * PAS + RUE;   // 162
    private static final int FLEUVE = 24, RIVE = 34, LARGEUR = VILLE + FLEUVE + RIVE, PROFONDEUR = VILLE;
    private static final int HAUT_MAX = 30;

    private final ServerWorld w;
    private final int ox, oy, oz;
    private final Random r = new Random(1600);
    private final int[][] hauteur = new int[LARGEUR][PROFONDEUR];

    private CartePrague(ServerWorld w, int ox, int oy, int oz) { this.w = w; this.ox = ox; this.oy = oy; this.oz = oz; }

    public static int construire(ServerCommandSource src) {
        BlockPos p = BlockPos.ofFloored(src.getPosition());
        CartePrague c = new CartePrague(src.getWorld(), p.getX() - 3, p.getY(), p.getZ() - (PROFONDEUR - 3));
        c.batir();
        if (src.getEntity() instanceof ServerPlayerEntity j) {
            BlockPos d = new BlockPos(c.ox + 3, c.oy, c.oz + PROFONDEUR - 3);
            j.teleport(src.getWorld(), d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 180f, 0f);
            j.setSpawnPoint(src.getWorld().getRegistryKey(), d, 180f, true, false);
            Kit.donner(j);
            // La mission recommence : journal vierge, points de vue à refaire, notoriété effacée
            for (String t : new java.util.ArrayList<>(j.getCommandTags()))
                if (t.startsWith("ombre_m_") || t.startsWith(Gardes.VUE_SYNCHRO)) j.removeCommandTag(t);
            j.addCommandTag(fr.fenua.ombre.ac.Mission.ACTIVE);
            fr.fenua.ombre.ac.Notoriete.ajouter(j, -100, null);
        }
        src.sendFeedback(() -> Text.literal("§6Prague, 1600.§r La Vieille-Ville s'élève au nord-est, la Vltava coule à l'est.\n"
                + "§eMission§r : parle à §6Tycho Brahe§r et à §6Johannes Kepler§r, au pied de l'horloge astronomique (clic droit).\n"
                + "§eCibles§r (dorées avec l'Instinct) : Ottokar von Rabenstein au sommet de la tour du pont, et l'alchimiste Melchior Kragl dans la tour nord de Týn.\n"
                + "§cZones interdites§r : autour des étendards rouges de la garde, on te repère deux fois plus vite.\n"
                + "§7Fonds-toi dans la foule, grimpe par les gouttières, synchronise aux gargouilles, plonge dans la Vltava. Touche H : toutes les commandes."), true);
        return 1;
    }

    // ───────────────────────── outils ─────────────────────────

    private static Block b(String nom) { return Registries.BLOCK.get(Identifier.of(Ombre.MOD_ID, nom)); }
    private void set(int x, int y, int z, BlockState s) { w.setBlockState(new BlockPos(ox + x, oy + y, oz + z), s, Block.NOTIFY_LISTENERS); }
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
    private static BlockState marche(Block bl, Direction d) { return bl.getDefaultState().with(StairsBlock.FACING, d); }
    private <T> T hasard(T[] t) { return t[r.nextInt(t.length)]; }

    // ───────────────────────── l'ensemble ─────────────────────────

    private void batir() {
        // Une Prague rebâtie remplace l'ancienne : on retire ses habitants (sauf les joueurs)
        net.minecraft.util.math.Box zone = new net.minecraft.util.math.Box(ox - 2, oy - 4, oz - 2, ox + LARGEUR + 2, oy + 70, oz + PROFONDEUR + 2);
        for (Entity e : w.getEntitiesByClass(Entity.class, zone, e -> !(e instanceof net.minecraft.entity.player.PlayerEntity))) e.discard();
        aplanir();
        rues();
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) {
                int bx = RUE + i * PAS, bz = RUE + j * PAS;
                if ((j == 1 || j == 2) && (i == 1 || i == 2)) continue;   // la grande place (2 × 2 îlots)
                if (i == 3 && j == 1) tyn(bx, bz);
                else if (i == 3 && j == 3) chantier(bx, bz);
                else if (i == 0 && j == 4) jardin(bx, bz);
                else ilot(bx, bz);
            }
        place();
        fleuve();
        poutresEtLinge();
        mobilierDeRue();
        tyroliennes();
        gens();
        zones();
    }

    // ───────────────────────── zones interdites et noms des lieux ─────────────────────────

    /** Les étendards de la garde (zones interdites) et les repères qui nomment les points de vue. */
    private void zones() {
        // La tour du pont, repaire du grand maître : un étendard au sommet
        set(tourPontA0 + 6, tourPontH, tourPontC0 + 7, vers(OmbreBlocs.ETENDARD, Direction.WEST));
        // Týn : un étendard au pied de la tour nord, où se cache l'alchimiste
        set(tynX - 2, 0, tynZ - 7, vers(OmbreBlocs.ETENDARD, Direction.WEST));
        // Des avis de recherche : au pied de la tour du pont, et sous l'horloge
        set(tourPontA0 - 1, 1, tourPontC0 + 1, vers(b("affiche_recherche"), Direction.EAST));   // sur les piliers de la porte, côté rue
        set(tourPontA0 - 1, 1, tourPontC0 + 7, vers(b("affiche_recherche"), Direction.EAST));
        set(orlojTx1 + 1, 1, orlojZ + 1, vers(b("affiche_recherche"), Direction.WEST));
        // Les repères nommés : les points de vue proches prennent leur nom
        BlockPos coin = new BlockPos(ox, oy - 4, oz), fin = new BlockPos(ox + LARGEUR, oy + 60, oz + PROFONDEUR);
        for (Entity e : w.getEntitiesByType(EntityType.MARKER, new net.minecraft.util.math.Box(coin.toCenterPos(), fin.toCenterPos()),
                e -> e.getCommandTags().contains(Gardes.LIEU))) e.discard();
        lieu("Horloge astronomique", orlojX, orlojH, orlojZ);
        lieu("Église de Týn", tynX, tynH + 6, tynZ);
        lieu("Tour du pont Charles", tourPontX, tourPontH, tourPontZ);
        if (grueH > 0) lieu("La grue du chantier", grueX, grueH, grueZ);
        if (jardinX > 0) lieu("Le jardin du couvent", jardinX, 4, jardinZ);
    }

    private void lieu(String nom, int x, int y, int z) {
        Entity m = EntityType.MARKER.create(w);
        if (m == null) return;
        m.refreshPositionAndAngles(ox + x + 0.5, oy + y, oz + z + 0.5, 0, 0);
        m.setCustomName(Text.literal(nom));
        m.addCommandTag(Gardes.LIEU);
        w.spawnEntity(m);
    }

    private void aplanir() {
        for (int x = 0; x < LARGEUR; x++)
            for (int z = 0; z < PROFONDEUR; z++) {
                for (int y = 0; y <= HAUT_MAX; y++) if (!vide(x, y, z)) set(x, y, z, Blocks.AIR);
                for (int y = -3; y <= -2; y++) set(x, y, z, Blocks.DIRT);
            }
    }

    private void rues() {
        BlockState pave = b("pave").getDefaultState(), dalles = b("dalles_de_pierre").getDefaultState();
        for (int x = 0; x < VILLE; x++)
            for (int z = 0; z < PROFONDEUR; z++) {
                boolean rue = x % PAS < RUE || z % PAS < RUE;
                set(x, -1, z, rue ? ((x + z) % 9 == 0 ? dalles : pave) : pave);
            }
    }

    // ───────────────────────── les maisons bourgeoises ─────────────────────────

    private void ilot(int bx, int bz) {
        rangee(bx, bz, ILOT, Direction.NORTH);
        rangee(bx, bz + ILOT - 8, ILOT, Direction.SOUTH);
        rangee(bx, bz + 8, ILOT - 16, Direction.WEST);
        rangee(bx + ILOT - 8, bz + 8, ILOT - 16, Direction.EAST);
        cour(bx + 8, bz + 8);
    }

    private void rangee(int x0, int z0, int longueur, Direction facade) {
        boolean ns = facade.getAxis() == Direction.Axis.Z;
        int u = 0;
        while (u < longueur) {
            int l = Math.min(longueur - u, 5 + r.nextInt(3));
            if (longueur - u - l < 5 && longueur - u - l > 0) l = longueur - u;
            int etages = 3 + r.nextInt(2);
            if (ns) maison(x0 + u, z0, x0 + u + l - 1, z0 + 7, facade, etages);
            else maison(x0, z0 + u, x0 + 7, z0 + u + l - 1, facade, etages);
            u += l;
        }
    }

    private static final String[] FACADES = {"crepi_jaune", "crepi_rose", "crepi_bleu", "crepi_vert", "crepi_ocre", "crepi", "sgraffite", "pierre_de_taille", "crepi_jaune", "crepi_rose"};
    private static final String[] VOLETS = {"volets", "volets_vert", "volets_rouge", "volets_brun", "volets_blanc", "volets_jaune"};
    private static final String[] AUVENTS = {"auvent", "auvent_raye_rouge_blanc", "auvent_raye_vert_blanc", "auvent_green", "auvent_brown",
            "auvent_raye_rouge_jaune", "auvent_raye_bleu_jaune", "auvent_yellow"};
    private static final String[] ENSEIGNES = {"enseigne_boulangerie", "enseigne_brasseur", "enseigne_apothicaire", "enseigne_horloger", "enseigne_imprimeur",
            "enseigne_boucherie", "enseigne_chapelier", "enseigne_barbier", "enseigne_cordonnier", "enseigne_alchimiste", "enseigne_tailleur", "enseigne_marchand_vin"};
    private static final String[] AFFICHES = {"affiche_proclamation", "affiche_recherche", "affiche_comediens", "affiche_foire", "affiche_almanach",
            "affiche_comete", "affiche_remede", "affiche_carte_du_ciel", "affiche_taverne", "affiche_joute", "affiche_bal_masque", "affiche_gazette"};

    /** Une maison étroite, sa façade côté « facade », pignon sur rue, toit de tuiles en bâtière. */
    private void maison(int x0, int z0, int x1, int z1, Direction facade, int etages) {
        int h = 4 + etages * 3;
        Block mur = b(hasard(FACADES));
        boite(x0, 0, z0, x1, h - 1, z1, mur);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) hauteur[x][z] = h;
        boolean ns = facade.getAxis() == Direction.Axis.Z;
        int debut = ns ? x0 : z0, fin = ns ? x1 : z1;
        int bord = switch (facade) { case NORTH -> z0; case SOUTH -> z1; case WEST -> x0; default -> x1; };
        int dx = facade.getOffsetX(), dz = facade.getOffsetZ();
        Direction versMur = facade.getOpposite();
        Block volets = b(hasard(VOLETS)), auvent = b(hasard(AUVENTS)), enseigne = b(hasard(ENSEIGNES));
        boolean boutique = r.nextInt(3) > 0;
        int porte = debut + 1 + r.nextInt(Math.max(1, fin - debut - 1)), posEnseigne = debut + 1 + r.nextInt(Math.max(1, fin - debut - 1));
        for (int u = debut; u <= fin; u++) {
            int x = ns ? u : bord, z = ns ? bord : u, xo = x + dx, zo = z + dz;
            // Rez-de-chaussée : pierre, porte, vitrines ou arcades
            for (int y = 0; y <= 3; y++) set(x, y, z, b(y == 3 ? "pierre_sculptee" : "pierre_de_taille"));
            if (u == porte) { set(x, 0, z, Blocks.AIR); set(x, 1, z, Blocks.AIR); }
            else if (boutique && (u - debut) % 2 == 1) { set(x, 1, z, Blocks.GLASS_PANE); set(x, 2, z, Blocks.GLASS_PANE); }
            if (u == debut) {                                             // une gouttière à chaque mitoyen
                for (int y = 0; y < h; y++) set(xo, y, zo, vers(OmbreBlocs.GOUTTIERE, versMur));
                continue;
            }
            if (boutique && u == posEnseigne) set(xo, 3, zo, vers(enseigne, versMur));
            else if (boutique && u != porte && r.nextInt(3) > 0) set(xo, 3, zo, vers(auvent, versMur));
            else if (r.nextInt(4) == 0) set(xo, 2, zo, vers(OmbreBlocs.REVERBERE, versMur));
            else if (r.nextInt(5) == 0) set(xo, 1, zo, vers(b(r.nextInt(3) == 0 ? "affiche_recherche" : hasard(AFFICHES)), versMur));
            for (int k = 0; k < etages; k++) {
                int yb = 4 + k * 3;
                boolean fenetre = (u - debut) % 2 == 1;
                if (fenetre) { set(x, yb + 1, z, Blocks.GLASS_PANE); set(x, yb + 2, z, Blocks.GLASS_PANE); }
                if (k == 0 && r.nextInt(3) == 0) set(xo, yb, zo, vers(OmbreBlocs.BALCON, versMur));
                else if (fenetre && r.nextBoolean()) { set(xo, yb + 1, zo, vers(volets, versMur)); set(xo, yb + 2, zo, vers(volets, versMur)); }
                else if (k == etages - 1 && !fenetre) set(xo, yb + 1, zo, vers(OmbreBlocs.CORNICHE, versMur));
            }
        }
        toit(x0, z0, x1, z1, h, facade, mur);
    }

    /** Toit à deux pentes, faîte perpendiculaire à la rue ; le pignon de façade monte en triangle. */
    private void toit(int x0, int z0, int x1, int z1, int h, Direction facade, Block mur) {
        Block tuile = b("tuiles"), marcheTuile = b("tuiles_escalier");
        boolean ns = facade.getAxis() == Direction.Axis.Z;
        int debut = ns ? x0 : z0, fin = ns ? x1 : z1, l = fin - debut + 1;
        int prof0 = ns ? z0 : x0, prof1 = ns ? z1 : x1;
        int pignon = switch (facade) { case NORTH -> z0; case SOUTH -> z1; case WEST -> x0; default -> x1; };
        Direction monteVersFin = ns ? Direction.EAST : Direction.SOUTH, monteVersDebut = monteVersFin.getOpposite();
        int haut = 0;
        for (int k = 0; k <= (l - 1) / 2; k++) {
            int ua = debut + k, ub = fin - k, y = h + k;
            haut = y;
            for (int p = prof0; p <= prof1; p++) {
                boolean front = p == pignon;
                int xa = ns ? ua : p, za = ns ? p : ua, xb = ns ? ub : p, zb = ns ? p : ub;
                if (ua == ub) { set(xa, y, za, front ? mur : tuile); continue; }
                if (front) {
                    set(xa, y, za, mur); set(xb, y, zb, mur);
                    for (int u = ua + 1; u < ub; u++) set(ns ? u : p, y, ns ? p : u, mur);
                } else {
                    set(xa, y, za, marche(marcheTuile, monteVersFin));
                    set(xb, y, zb, marche(marcheTuile, monteVersDebut));
                    for (int u = ua + 1; u < ub; u++) set(ns ? u : p, y, ns ? p : u, tuile);
                }
            }
        }
        // Le sommet du pignon, une fenêtre de grenier, une cheminée
        int milieu = (debut + fin) / 2;
        set(ns ? milieu : pignon, haut + 1, ns ? pignon : milieu, b("pierre_sculptee"));
        set(ns ? milieu : pignon, h + 1, ns ? pignon : milieu, Blocks.GLASS_PANE);
        int cp = (prof0 + prof1) / 2 + 2;
        set(ns ? debut + 1 : cp, h + 2, ns ? cp : debut + 1, b("brique_ancienne"));
        set(ns ? debut + 1 : cp, h + 3, ns ? cp : debut + 1, r.nextBoolean() ? OmbreBlocs.CHEMINEE : b("cheminee_terre_cuite"));
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) hauteur[x][z] = h;
    }

    private void cour(int cx, int cz) {
        set(cx + 3, 0, cz + 3, OmbreBlocs.PUITS);
        arbre(cx + 6, cz + 6);
        for (int x = cx; x <= cx + 7; x++) set(x, 7, cz + 1, vers(OmbreBlocs.CORDE_LINGE, Direction.NORTH));
        for (int y = 0; y < 13; y++) set(cx, y, cz + 5, vers(OmbreBlocs.GOUTTIERE, Direction.WEST));
        if (r.nextBoolean()) set(cx + 1, 0, cz + 7, vers(OmbreBlocs.TONNEAU, Direction.NORTH));
        if (r.nextBoolean()) boite(cx + 5, 0, cz + 1, cx + 6, 1, cz + 2, OmbreBlocs.MEULE);
    }

    private void arbre(int x, int z) {
        boite(x, 0, z, x, 4, z, Blocks.OAK_LOG);
        BlockState f = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int a = -2; a <= 2; a++) for (int c = -2; c <= 2; c++) for (int y = 3; y <= 6; y++) {
            if (Math.abs(a) + Math.abs(c) > (y == 6 ? 1 : 3) || (a == 0 && c == 0 && y <= 4)) continue;
            if (vide(x + a, y, z + c)) set(x + a, y, z + c, f);
        }
    }

    // ───────────────────────── la place et l'horloge astronomique ─────────────────────────

    private static final String[] ETALS = {"etal", "etal_fruits", "etal_poisson", "etal_fleurs", "etal_pain", "etal_etoffes", "etal_legumes"};
    private int orlojX, orlojZ, orlojH, orlojTx1, tychoX, tychoZ;

    private void place() {
        int x0 = RUE + PAS, x1 = RUE + 2 * PAS + ILOT - 1, z0 = RUE + PAS, z1 = RUE + 2 * PAS + ILOT - 1;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) set(x, -1, z, b((x * 7 + z * 3) % 11 == 0 ? "dalles_de_pierre" : "pave"));
        // La tour de l'horloge, à l'ouest de la place (l'hôtel de ville)
        int tx0 = x0, tx1 = x0 + 6, tz0 = z0 + 6, tz1 = z0 + 12, h = 32;
        boite(tx0, 0, tz0, tx1, h - 1, tz1, b("pierre_noircie"));
        for (int y = 6; y < h; y += 7) boite(tx0, y, tz0, tx1, y, tz1, b("pierre_sculptee"));
        boite(tx0 - 3, 0, tz0 - 2, tx0 - 1, 9, tz1 + 2, b("crepi_ocre"));                    // un bout d'hôtel de ville
        // Le cadran (3 × 3), côté place
        for (int a = 0; a < 3; a++) for (int c = 0; c < 3; c++) set(tx1 + 1, 10 + c, tz0 + 2 + a, vers(OmbreBlocs.HORLOGE, Direction.WEST));
        set(tx1 + 1, 14, tz0 + 3, vers(b("statue"), Direction.WEST));
        // Galerie et flèche
        for (int x = tx0 - 1; x <= tx1 + 1; x++) for (int z = tz0 - 1; z <= tz1 + 1; z++)
            if (x == tx0 - 1 || x == tx1 + 1 || z == tz0 - 1 || z == tz1 + 1) set(x, h - 1, z, Blocks.STONE_BRICK_SLAB.getDefaultState()
                    .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.TOP));
        Block ardoise = b("ardoise"), marcheArdoise = b("ardoise_escalier");
        for (int k = 0; k < 4; k++) {
            int y = h + k, a0 = tx0 + k, a1 = tx1 - k, c0 = tz0 + k, c1 = tz1 - k;
            if (a0 > a1) break;
            boite(a0, y, c0, a1, y, c1, ardoise);
            for (int x = a0; x <= a1; x++) { set(x, y, c0, marche(marcheArdoise, Direction.SOUTH)); set(x, y, c1, marche(marcheArdoise, Direction.NORTH)); }
            for (int z = c0; z <= c1; z++) { set(a0, y, z, marche(marcheArdoise, Direction.EAST)); set(a1, y, z, marche(marcheArdoise, Direction.WEST)); }
        }
        set((tx0 + tx1) / 2, h + 4, (tz0 + tz1) / 2, OmbreBlocs.DRAPEAU);
        // Gargouilles aux quatre angles de la galerie : points de vue
        set(tx1 + 2, h - 2, tz0, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        set(tx1 + 2, h - 2, tz1, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        set(tx0 - 2, h - 2, tz0, vers(OmbreBlocs.GARGOUILLE, Direction.EAST));
        for (int y = 0; y < h - 1; y++) set(tx1 + 1, y, tz1, vers(OmbreBlocs.GOUTTIERE, Direction.WEST));
        orlojX = (tx0 + tx1) / 2; orlojZ = tz0 - 1; orlojH = h; orlojTx1 = tx1;
        // Les astronomes et leurs instruments, au pied de l'horloge
        set(tx1 + 3, 0, tz0 - 2, OmbreBlocs.SPHERE);
        set(tx1 + 3, 0, tz1 + 7, vers(OmbreBlocs.PUPITRE, Direction.EAST));
        tychoX = tx1 + 4; tychoZ = tz0 + 4;
        marche(x0, x1, z0, z1);
        boite(tx1 + 2, 0, tz1 + 1, tx1 + 4, 1, tz1 + 3, OmbreBlocs.MEULE);                    // juste sous la gargouille sud
    }


    // ───────────────────────── le marché de la place ─────────────────────────

    /** Les marchands, derrière leurs étals (on les fait venir avec la foule). */
    private final java.util.List<int[]> marchands = new java.util.ArrayList<>();

    /** Place un bloc dans le repère d'une construction tournée vers « face » : a vers la droite, p vers l'arrière. */
    private void rel(int x, int z, Direction face, int a, int y, int p, BlockState st) {
        Direction droite = face.rotateYClockwise(), arriere = face.getOpposite();
        set(x + droite.getOffsetX() * a + arriere.getOffsetX() * p, y, z + droite.getOffsetZ() * a + arriere.getOffsetZ() * p, st);
    }
    private boolean videRel(int x, int z, Direction face, int a0, int a1, int p0, int p1, int y1) {
        Direction droite = face.rotateYClockwise(), arriere = face.getOpposite();
        for (int a = a0; a <= a1; a++) for (int p = p0; p <= p1; p++) for (int y = 0; y <= y1; y++)
            if (!vide(x + droite.getOffsetX() * a + arriere.getOffsetX() * p, y, z + droite.getOffsetZ() * a + arriere.getOffsetZ() * p)) return false;
        return true;
    }

    private static final String[][] MARCHANDISES = {
            // marchandise, toile 1, toile 2, ce qui trône sur le comptoir
            {"fruits", "orange_wool", "white_wool", "melon"}, {"poisson", "blue_wool", "white_wool", "water_cauldron"},
            {"fleurs", "pink_wool", "white_wool", "potted_red_tulip"}, {"pain", "yellow_wool", "brown_wool", "cake"},
            {"etoffes", "purple_wool", "yellow_wool", "red_wool"}, {"legumes", "lime_wool", "white_wool", "pumpkin"}};

    /**
     * Un étal de marché, tourné vers « face » (là où se tiennent les clients) : trois de large, deux de profondeur.
     * Devant, le comptoir chargé de marchandises ; derrière, les tonneaux et la place du marchand ;
     * quatre poteaux et une toile rayée aux couleurs du métier, avec un auvent qui déborde vers les clients.
     * (x, z) : le coin avant gauche.
     */
    private void etal(int x, int z, Direction face, int sorte) {
        String[] m = MARCHANDISES[Math.floorMod(sorte, MARCHANDISES.length)];
        if (!videRel(x, z, face, 0, 2, 0, 1, 3)) return;
        BlockState comptoir = vers(b("etal_" + m[0]), face), poteau = Blocks.SPRUCE_FENCE.getDefaultState();
        BlockState t1 = Registries.BLOCK.get(Identifier.ofVanilla(m[1])).getDefaultState(), t2 = Registries.BLOCK.get(Identifier.ofVanilla(m[2])).getDefaultState();
        for (int a = 0; a <= 2; a++) rel(x, z, face, a, 0, 0, comptoir);
        rel(x, z, face, 0, 0, 1, Blocks.BARREL.getDefaultState());
        rel(x, z, face, 2, 0, 1, m[0].equals("poisson") ? Blocks.WATER_CAULDRON.getDefaultState() : Blocks.BARREL.getDefaultState());
        for (int y = 1; y <= 2; y++) for (int[] c : new int[][]{{0, 0}, {2, 0}, {0, 1}, {2, 1}}) rel(x, z, face, c[0], y, c[1], poteau);
        for (int a = 0; a <= 2; a++) for (int p = 0; p <= 1; p++) rel(x, z, face, a, 3, p, a % 2 == 0 ? t1 : t2);
        for (int a = 0; a <= 2; a++) rel(x, z, face, a, 3, -1, vers(b("auvent_raye_" + (a % 2 == 0 ? "rouge_blanc" : "rouge_jaune")), face.getOpposite()));
        rel(x, z, face, 1, 1, 0, Registries.BLOCK.get(Identifier.ofVanilla(m[3])).getDefaultState());
        Direction droite = face.rotateYClockwise(), arriere = face.getOpposite();
        marchands.add(new int[]{x + droite.getOffsetX() + arriere.getOffsetX(), z + droite.getOffsetZ() + arriere.getOffsetZ()});
    }

    /** Une tente de marché : cinq de large, quatre de profondeur, toit de toile en bâtière, tables et tonneaux dessous. */
    private void tente(int x, int z, BlockState toile1, BlockState toile2) {
        BlockState poteau = Blocks.SPRUCE_FENCE.getDefaultState();
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 3}, {4, 3}, {2, 0}, {2, 3}}) boite(x + c[0], 0, z + c[1], x + c[0], 2, z + c[1], poteau);
        for (int a = 0; a <= 4; a++) {
            BlockState t = a % 2 == 0 ? toile1 : toile2;
            set(x + a, 3, z, t); set(x + a, 3, z + 3, t);
            set(x + a, 4, z + 1, t); set(x + a, 4, z + 2, t);
        }
        set(x + 1, 0, z + 1, vers(b("etal_etoffes"), Direction.SOUTH));
        set(x + 3, 0, z + 1, vers(b("etal_pain"), Direction.SOUTH));
        set(x + 1, 0, z + 2, Blocks.BARREL);
        set(x + 3, 0, z + 2, vers(OmbreBlocs.TONNEAU, Direction.EAST));
        marchands.add(new int[]{x + 2, z + 2});
    }

    /** La grande fontaine : un bassin de sept, la colonne et sa statue au milieu. */
    private void fontaineBassin(int cx, int cz) {
        for (int a = -3; a <= 3; a++) for (int c = -3; c <= 3; c++) {
            if (Math.abs(a) == 3 && Math.abs(c) == 3) continue;
            boolean bord = Math.abs(a) == 3 || Math.abs(c) == 3 || (Math.abs(a) == 2 && Math.abs(c) == 3) || (Math.abs(a) == 3 && Math.abs(c) == 2);
            set(cx + a, -2, cz + c, Blocks.STONE_BRICKS);
            set(cx + a, -1, cz + c, bord ? b("pierre_sculptee").getDefaultState() : Blocks.WATER.getDefaultState());
            set(cx + a, 0, cz + c, bord ? b("pierre_de_taille_dalle").getDefaultState() : Blocks.WATER.getDefaultState());
        }
        for (int y = 0; y <= 3; y++) set(cx, y, cz, b(y == 3 ? "pierre_sculptee" : "pierre_lisse"));
        set(cx, 4, cz, b("statue"));
        for (Direction d : Direction.Type.HORIZONTAL) set(cx + d.getOffsetX(), 1, cz + d.getOffsetZ(), OmbreBlocs.FONTAINE);
    }

    /** Les tréteaux des comédiens : une estrade, un rideau rouge, l'affiche. */
    private void scene(int x, int z) {
        boite(x, 0, z, x + 6, 0, z + 3, Blocks.SPRUCE_PLANKS);
        for (int a = 0; a <= 6; a++) {
            boite(x + a, 1, z + 3, x + a, 4, z + 3, a == 0 || a == 6 ? Blocks.STRIPPED_SPRUCE_LOG.getDefaultState() : Blocks.RED_WOOL.getDefaultState());
            set(x + a, 5, z + 3, Blocks.SPRUCE_SLAB);
        }
        set(x + 3, 2, z + 2, vers(b("affiche_comediens"), Direction.SOUTH));
        set(x + 1, 1, z, Blocks.LANTERN); set(x + 5, 1, z, Blocks.LANTERN);
        for (int a : new int[]{1, 3, 5}) set(x + a, 0, z - 2, vers(b("banc_spruce"), Direction.SOUTH));
    }

    /**
     * La grande place : la fontaine au milieu, douze étals tournés vers elle, deux tentes,
     * les tréteaux des comédiens, des bancs, des charrettes, des arbres, des lanternes.
     */
    private void marche(int x0, int x1, int z0, int z1) {
        int cx = (x0 + x1) / 2 + 2, cz = (z0 + z1) / 2;
        fontaineBassin(cx, cz);
        int sorte = 0;
        for (int k = 0; k < 4; k++) {
            int sx = cx - 14 + k * 8;
            etal(sx + 2, cz - 13, Direction.SOUTH, sorte++);     // au nord, tournés vers le sud (coin avant gauche à l'est)
            etal(sx, cz + 13, Direction.NORTH, sorte++);
        }
        etal(cx - 18, cz - 4, Direction.EAST, sorte++);
        etal(cx - 18, cz + 6, Direction.EAST, sorte++);
        etal(cx + 18, cz + 4, Direction.WEST, sorte++);
        etal(cx + 18, cz - 6, Direction.WEST, sorte++);
        tente(x0 + 12, z0 + 2, Blocks.WHITE_WOOL.getDefaultState(), Blocks.RED_WOOL.getDefaultState());
        tente(x1 - 14, z0 + 2, Blocks.WHITE_WOOL.getDefaultState(), Blocks.BLUE_WOOL.getDefaultState());
        scene(x1 - 12, z1 - 7);
        // Bancs autour de la fontaine, un puits, des charrettes, des tonneaux
        for (Direction d : Direction.Type.HORIZONTAL) set(cx + d.getOffsetX() * 6, 0, cz + d.getOffsetZ() * 6, vers(OmbreBlocs.BANC, d));
        set(cx - 9, 0, cz + 7, OmbreBlocs.PUITS);
        set(cx + 9, 0, cz - 8, vers(OmbreBlocs.CHARRETTE, Direction.NORTH));
        set(x0 + 14, 0, z1 - 4, vers(OmbreBlocs.CHARRETTE, Direction.EAST));
        set(x1 - 4, 0, cz, vers(OmbreBlocs.CARROSSE, Direction.NORTH));
        for (int[] t : new int[][]{{cx - 6, cz - 9}, {cx + 7, cz + 9}, {x0 + 20, cz}}) {
            set(t[0], 0, t[1], Blocks.BARREL); set(t[0] + 1, 0, t[1], Blocks.BARREL); set(t[0], 1, t[1], Blocks.BARREL);
        }
        for (int[] p : new int[][]{{x1 - 2, z0 + 2}, {x1 - 2, z1 - 2}, {x0 + 10, z1 - 2}, {x0 + 22, z0 + 2}}) arbre(p[0], p[1]);
        for (int[] l : new int[][]{{cx - 10, cz}, {cx + 10, cz}, {cx, cz - 10}, {cx, cz + 10}}) {
            boite(l[0], 0, l[1], l[0], 2, l[1], OmbreBlocs.LAMPADAIRE);
            set(l[0], 3, l[1], OmbreBlocs.TETE_LAMPADAIRE);
        }
        set(x0 + 24, 0, z1 - 1, vers(b("cabine_red"), Direction.NORTH));
        set(x1 - 6, 0, z0 + 1, OmbreBlocs.COLONNE);
        set(cx + 3, 0, cz + 20, vers(OmbreBlocs.TONNEAU, Direction.WEST));
    }

    // ───────────────────────── le chantier et sa grue ─────────────────────────

    private int grueX, grueZ, grueH;

    /**
     * Un îlot en chantier : une grande maison à moitié bâtie, ses échafaudages, des tas de pierres et de bois,
     * et une grue à roue (les maçons de 1600 en avaient : des hommes marchaient dans la roue pour hisser les pierres).
     * Du haut de sa flèche, un point de vue.
     */
    private void chantier(int bx, int bz) {
        boite(bx, -1, bz, bx + ILOT - 1, -1, bz + ILOT - 1, Blocks.COARSE_DIRT);
        for (int x = bx; x < bx + ILOT; x++) for (int z = bz; z < bz + ILOT; z++) if ((x * 5 + z * 3) % 7 == 0) set(x, -1, z, Blocks.GRAVEL);
        rangee(bx, bz, 9, Direction.NORTH);                        // des maisons déjà finies
        // Les murs en cours : leur sommet est inégal ; des trous de fenêtres attendent leurs vitres
        int a0 = bx + 11, a1 = bx + 22, c0 = bz + 3, c1 = bz + 13;
        Block mur = b("pierre_de_taille");
        for (int x = a0; x <= a1; x++) for (int z = c0; z <= c1; z++) {
            if (x != a0 && x != a1 && z != c0 && z != c1) continue;
            int h = 6 + (int) (4 * Math.abs(Math.sin(x * 0.7 + z * 0.4))) + (x == a0 || x == a1 ? 2 : 0);
            for (int y = 0; y < h; y++) set(x, y, z, (y % 4 == 3) && (x + z) % 3 == 0 ? Blocks.AIR.getDefaultState() : mur.getDefaultState());
        }
        for (int x = a0; x <= a1; x++) for (int z = c0; z <= c1; z++) hauteur[x][z] = 7;
        // Un plancher posé à mi-hauteur, des poutres en attente
        boite(a0 + 1, 4, c0 + 1, a1 - 1, 4, c0 + 5, Blocks.SPRUCE_PLANKS);
        for (int z = c0 + 7; z < c1; z += 2) boite(a0 + 1, 4, z, a1 - 1, 4, z, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
        // Les échafaudages, au sud et à l'ouest
        for (int x = a0 - 1; x <= a1 + 1; x += 2) boite(x, 0, c1 + 1, x, 8, c1 + 1, OmbreBlocs.ECHAFAUDAGE);
        for (int z = c0; z <= c1; z += 2) boite(a0 - 1, 0, z, a0 - 1, 8, z, OmbreBlocs.ECHAFAUDAGE);
        for (int y : new int[]{3, 6}) {
            boite(a0 - 1, y, c1 + 2, a1 + 1, y, c1 + 2, Blocks.SPRUCE_SLAB);
            boite(a0 - 2, y, c0, a0 - 2, y, c1 + 2, Blocks.SPRUCE_SLAB);
        }
        // Les tas : pierres, bois, sable ; une charrette chargée
        boite(bx + 3, 0, bz + 15, bx + 5, 1, bz + 16, Blocks.COBBLESTONE);
        boite(bx + 3, 0, bz + 19, bx + 6, 0, bz + 20, Blocks.OAK_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
        set(bx + 4, 1, bz + 19, Blocks.OAK_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
        boite(bx + 18, 0, bz + 18, bx + 19, 0, bz + 19, Blocks.SAND);
        set(bx + 18, 1, bz + 18, Blocks.SAND);
        set(bx + 14, 0, bz + 20, vers(OmbreBlocs.CHARRETTE, Direction.EAST));
        boite(bx + 1, 0, bz + 22, bx + 2, 1, bz + 23, OmbreBlocs.MEULE);
        // La grue : un mât de 18, une flèche qui s'avance au-dessus du chantier, le câble et sa charge, la roue au pied
        int gx = bx + 8, gz = bz + 17, h = 18;
        for (int y = 0; y < h; y++) {
            set(gx, y, gz, Blocks.STRIPPED_SPRUCE_LOG);
            if (y % 4 == 0) { set(gx - 1, y, gz, Blocks.SPRUCE_FENCE); set(gx + 1, y, gz, Blocks.SPRUCE_FENCE); }
        }
        for (int x = gx - 2; x <= gx + 9; x++) set(x, h, gz, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
        set(gx - 2, h + 1, gz, Blocks.COBBLESTONE);                    // le contrepoids
        set(gx + 9, h + 1, gz, OmbreBlocs.PERCHOIR);                   // au bout de la flèche : un point de vue
        for (int y = h - 1; y >= 8; y--) set(gx + 8, y, gz, Blocks.CHAIN);
        boite(gx + 7, 7, gz - 1, gx + 9, 7, gz + 1, Blocks.SPRUCE_PLANKS);
        set(gx + 8, 8, gz - 1, Blocks.COBBLESTONE); set(gx + 7, 8, gz + 1, Blocks.COBBLESTONE);
        // La roue : un cercle de planches debout, contre le mât
        for (int t = 0; t < 24; t++) {
            double ang = t * Math.PI * 2 / 24;
            int yy = 3 + (int) Math.round(Math.sin(ang) * 3), zz = gz + 1 + (int) Math.round(Math.cos(ang) * 3);
            if (yy >= 0) set(gx - 2, yy, zz, Blocks.SPRUCE_PLANKS);
        }
        set(gx - 2, 3, gz + 1, Blocks.STRIPPED_SPRUCE_LOG);
        grueX = gx + 9; grueZ = gz; grueH = h + 2;
    }

    // ───────────────────────── le jardin du couvent ─────────────────────────

    /** Un jardin clos de couvent : allées en croix, massifs, haies, arbres fruitiers, un kiosque, et des herbes hautes où se cacher. */
    private void jardin(int bx, int bz) {
        int b1x = bx + ILOT - 1, b1z = bz + ILOT - 1, mx = bx + ILOT / 2, mz = bz + ILOT / 2;
        for (int x = bx; x <= b1x; x++) for (int z = bz; z <= b1z; z++) {
            boolean allee = Math.abs(x - mx) <= 1 || Math.abs(z - mz) <= 1;
            set(x, -1, z, allee ? Blocks.GRAVEL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            if (x == bx || x == b1x || z == bz || z == b1z) {
                boolean porte = Math.abs(x - mx) <= 1 || Math.abs(z - mz) <= 1;
                if (!porte) { set(x, 0, z, Blocks.STONE_BRICK_WALL); if ((x + z) % 4 == 0) set(x, 1, z, Blocks.STONE_BRICK_WALL); }
            }
        }
        BlockState haie = Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        Block[] fleurs = {Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.LILY_OF_THE_VALLEY};
        for (int[] q : new int[][]{{bx + 2, bz + 2}, {mx + 3, bz + 2}, {bx + 2, mz + 3}, {mx + 3, mz + 3}}) {
            int qx1 = q[0] + ILOT / 2 - 6, qz1 = q[1] + ILOT / 2 - 6;
            for (int x = q[0]; x <= qx1; x++) for (int z = q[1]; z <= qz1; z++) {
                boolean bord = x == q[0] || x == qx1 || z == q[1] || z == qz1;
                if (bord) { set(x, 0, z, haie); continue; }
                if ((x + z) % 3 != 0) set(x, 0, z, fleurs[Math.floorMod(x * 3 + z, fleurs.length)]);
            }
        }
        // Le carré sud-est : des herbes hautes (une cachette), un pommier
        for (int x = mx + 4; x < b1x - 1; x++) for (int z = mz + 4; z < b1z - 1; z++) {
            set(x, 0, z, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
            set(x, 1, z, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        }
        for (int[] t : new int[][]{{bx + 5, bz + 5}, {b1x - 5, bz + 5}, {bx + 5, b1z - 5}}) arbre(t[0], t[1]);
        // Le kiosque, au croisement des allées
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) boite(mx + c[0], 0, mz + c[1], mx + c[0], 2, mz + c[1], Blocks.DARK_OAK_FENCE);
        boite(mx - 2, 3, mz - 2, mx + 2, 3, mz + 2, Blocks.DARK_OAK_SLAB);
        boite(mx - 1, 4, mz - 1, mx + 1, 4, mz + 1, b("ardoise_dalle"));
        set(mx, 5, mz, Blocks.LANTERN);
        set(mx, 0, mz, OmbreBlocs.FONTAINE);
        set(mx - 3, 0, mz - 3, vers(b("banc_dark_oak"), Direction.SOUTH));
        set(mx + 3, 0, mz + 3, vers(b("banc_dark_oak"), Direction.NORTH));
        for (int x = bx; x <= b1x; x++) for (int z = bz; z <= b1z; z++) hauteur[x][z] = 1;
        jardinX = mx; jardinZ = mz;
    }

    private int jardinX, jardinZ;

    // ───────────────────────── sur la Vltava ─────────────────────────

    /** Une barque amarrée, pointe vers le nord. */
    private void barque(int x, int z) {
        for (int k = 0; k < 7; k++) {
            int l = k == 0 || k == 6 ? 0 : 1;
            for (int a = -l; a <= l; a++) set(x + a, -1, z + k, (a == 0 && k > 0 && k < 6) ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.SPRUCE_SLAB.getDefaultState());
            if (l == 1) { set(x - 1, 0, z + k, Blocks.SPRUCE_TRAPDOOR.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST).with(Properties.OPEN, true));
                          set(x + 1, 0, z + k, Blocks.SPRUCE_TRAPDOOR.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST).with(Properties.OPEN, true)); }
        }
        set(x, 0, z + 3, Blocks.BARREL);
    }

    /**
     * Le moulin à eau de la rive (la Vieille-Ville en avait toute une rangée au bord de la Vltava) :
     * une maison de bois, et la grande roue qui trempe dans le courant.
     */
    private void moulin(int f0, int z0) {
        boite(f0 - 7, 0, z0, f0 - 2, 6, z0 + 8, Blocks.SPRUCE_PLANKS);
        boite(f0 - 6, 1, z0 + 1, f0 - 3, 5, z0 + 7, Blocks.AIR);
        for (int y = 0; y <= 6; y += 3) boite(f0 - 7, y, z0, f0 - 2, y, z0 + 8, Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.Z));
        set(f0 - 7, 1, z0 + 4, Blocks.AIR); set(f0 - 7, 2, z0 + 4, Blocks.AIR);
        for (int k = 0; k <= 3; k++) boite(f0 - 7 + k, 7 + k, z0, f0 - 2 - k, 7 + k, z0 + 8, b("ardoise"));
        set(f0 - 5, 1, z0 + 2, Blocks.GRINDSTONE); set(f0 - 4, 1, z0 + 6, Blocks.BARREL); set(f0 - 4, 2, z0 + 6, Blocks.BARREL);
        for (int x = f0 - 7; x <= f0 - 2; x++) for (int z = z0; z <= z0 + 8; z++) hauteur[x][z] = 7;
        // La roue, dans le plan nord-sud, son axe entre dans le moulin
        int cz = z0 + 4, cy = 0;
        for (int t = 0; t < 40; t++) {
            double a = t * Math.PI * 2 / 40;
            int y = cy + (int) Math.round(Math.sin(a) * 4), z = cz + (int) Math.round(Math.cos(a) * 4);
            set(f0 + 1, y, z, Blocks.STRIPPED_SPRUCE_LOG); set(f0 + 2, y, z, Blocks.SPRUCE_PLANKS);
        }
        for (int k = -3; k <= 3; k++) { set(f0 + 1, cy + k, cz, Blocks.SPRUCE_FENCE); set(f0 + 1, cy, cz + k, Blocks.SPRUCE_FENCE); }
        set(f0, cy, cz, Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
        set(f0 - 1, cy, cz, Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
    }

    // ───────────────────────── l'église de Týn ─────────────────────────

    private int tynX, tynZ, tynH, laboX, laboZ, laboY;

    private void tyn(int bx, int bz) {
        boite(bx, -1, bz, bx + ILOT - 1, -1, bz + ILOT - 1, b("pave"));
        Block pierre = b("pierre_noircie"), ardoise = b("ardoise"), marcheArdoise = b("ardoise_escalier");
        // La nef : 12 de large (z), de x = bx+6 à bx+22, 16 de haut, toit très pentu
        int n0 = bx + 6, n1 = bx + 22, z0 = bz + 6, z1 = bz + 17, hn = 16;
        boite(n0, 0, z0, n1, hn - 1, z1, pierre);
        for (int x = n0 + 2; x < n1; x += 3) for (int y = 3; y < 12; y++) { set(x, y, z0, Blocks.GLASS_PANE); set(x, y, z1, Blocks.GLASS_PANE); }
        for (int k = 0; k <= 5; k++) {
            int y = hn + k * 2;
            for (int x = n0; x <= n1; x++) {
                for (int dy = 0; dy < 2; dy++) {
                    set(x, y + dy, z0 + k, marche(marcheArdoise, Direction.SOUTH));
                    set(x, y + dy, z1 - k, marche(marcheArdoise, Direction.NORTH));
                }
                for (int z = z0 + k + 1; z < z1 - k; z++) { set(x, y, z, ardoise); set(x, y + 1, z, ardoise); }
            }
        }
        // Les deux tours, côté place (ouest), et leurs flèches
        int ht = 30;
        for (int[] t : new int[][]{{bx + 1, bz + 3}, {bx + 1, bz + 15}}) {
            int a0 = t[0], c0 = t[1], a1 = a0 + 5, c1 = c0 + 5;
            boite(a0, 0, c0, a1, ht - 1, c1, pierre);
            for (int y = 8; y < ht; y += 7) boite(a0, y, c0, a1, y, c1, b("pierre_sculptee"));
            for (int k = 0; k < 9; k++) {
                int y = ht + k * 2, e0 = a0 + k / 2, e1 = a1 - k / 2, f0 = c0 + k / 2, f1 = c1 - k / 2;
                if (e0 > e1 || f0 > f1) break;
                for (int dy = 0; dy < 2; dy++) boite(e0, y + dy, f0, e1, y + dy, f1, ardoise);
            }
            int sx = (a0 + a1) / 2, sz = (c0 + c1) / 2;
            set(sx, ht + 12, sz, OmbreBlocs.CHARPENTE);
            set(sx, ht + 13, sz, OmbreBlocs.PERCHOIR);
            for (int[] coin : new int[][]{{a0, c0}, {a1, c0}, {a0, c1}, {a1, c1}}) {   // les clochetons
                boite(coin[0], ht, coin[1], coin[0], ht + 4, coin[1], ardoise);
                set(coin[0], ht + 5, coin[1], OmbreBlocs.CHARPENTE);
            }
            set(a0 - 1, ht - 2, c0 + 1, vers(OmbreBlocs.GARGOUILLE, Direction.EAST));
            set(a0 - 1, ht - 2, c1 - 1, vers(OmbreBlocs.GARGOUILLE, Direction.EAST));
            for (int y = 0; y < ht; y++) set(a0 - 1, y, (c0 + c1) / 2, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
            tynX = a0; tynZ = (c0 + c1) / 2; tynH = ht;
        }
        // Le laboratoire de l'alchimiste, dans la tour nord : une salle creuse, une fenêtre sur la place
        int a0 = bx + 1, c0 = bz + 3;
        laboX = a0 + 2; laboZ = c0 + 2; laboY = 22;
        boite(a0 + 1, laboY, c0 + 1, a0 + 4, laboY + 3, c0 + 4, Blocks.AIR);
        boite(a0, laboY + 1, c0 + 2, a0, laboY + 2, c0 + 3, Blocks.AIR);
        set(a0 + 4, laboY, c0 + 1, OmbreBlocs.ALAMBIC);
        set(a0 + 4, laboY, c0 + 4, vers(OmbreBlocs.ETAGERE, Direction.EAST));
        set(a0 + 1, laboY, c0 + 4, vers(OmbreBlocs.PUPITRE, Direction.WEST));
        set(a0 + 2, laboY + 3, c0 + 2, OmbreBlocs.TETE_LAMPADAIRE);
        // Le saut de la foi depuis les gargouilles
        boite(bx - 4, 0, bz + 9, bx - 2, 1, bz + 12, OmbreBlocs.MEULE);
        for (int x = n0; x <= n1; x++) for (int z = z0; z <= z1; z++) hauteur[x][z] = hn;
    }

    // ───────────────────────── la Vltava et le pont Charles ─────────────────────────

    private int tourPontX, tourPontZ, tourPontH, tourPontA0, tourPontC0, rodolpheX, rodolpheZ, rivePontX;

    private void fleuve() {
        int f0 = VILLE, f1 = VILLE + FLEUVE - 1;
        for (int x = f0; x <= f1; x++) for (int z = 0; z < PROFONDEUR; z++) {
            boite(x, -3, z, x, -1, z, Blocks.WATER);
        }
        for (int z = 0; z < PROFONDEUR; z++) {                      // les quais
            set(f0 - 1, -1, z, Blocks.STONE_BRICKS); set(f1 + 1, -1, z, Blocks.STONE_BRICKS);
            if (z % 2 == 0) { set(f0 - 1, 0, z, Blocks.STONE_BRICK_WALL); set(f1 + 1, 0, z, Blocks.STONE_BRICK_WALL); }
        }
        // Sur l'eau : le moulin, des radeaux (un passage de parkour d'une rive à l'autre), des barques, un ponton
        moulin(f0, 10);
        for (int k = 0; k < 5; k++) boite(f0 + 2 + k * 4, -1, 30 + (k % 2) * 2, f0 + 3 + k * 4, -1, 32 + (k % 2) * 2, Blocks.SPRUCE_PLANKS);
        for (int z : new int[]{80, 96, 112}) barque(f0 + 3 + r.nextInt(3), z);
        for (int z = 88; z <= 92; z++) boite(f0, -1, z, f0 + 3, -1, z, Blocks.SPRUCE_PLANKS);
        for (int z : new int[]{88, 92}) set(f0 + 3, 0, z, Blocks.SPRUCE_FENCE);
        // L'autre rive (Malá Strana) : herbe, deux rangées de maisons le long d'une rue, un verger
        for (int x = f1 + 2; x < LARGEUR; x++) for (int z = 0; z < PROFONDEUR; z++) set(x, -1, z, (x + z) % 7 == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
        for (int x = f1 + 12; x <= f1 + 17; x++) for (int z = 0; z < PROFONDEUR; z++) set(x, -1, z, b("pave"));
        for (int z = 6; z < PROFONDEUR - 6; z += 9) {
            if (Math.abs(z - (2 * PAS + 3)) < 8) continue;
            maison(f1 + 4, z, f1 + 11, z + 5 + r.nextInt(2), Direction.EAST, 2 + r.nextInt(2));
            maison(f1 + 18, z, f1 + 25, z + 5 + r.nextInt(2), Direction.WEST, 2 + r.nextInt(2));
        }
        for (int z = 8; z < PROFONDEUR - 6; z += 7) for (int x : new int[]{f1 + 29, f1 + 32}) if (x < LARGEUR - 1) arbre(x, z + (x % 2) * 3);
        // Le pont : dans l'axe de la rue entre les rangées 1 et 2
        int zp0 = 2 * PAS, zp1 = zp0 + RUE - 1;
        for (int x = f0 - 1; x <= f1 + 1; x++) {
            boite(x, -1, zp0, x, -1, zp1, b("dalles_de_pierre"));
            set(x, 0, zp0, Blocks.STONE_BRICK_WALL); set(x, 0, zp1, Blocks.STONE_BRICK_WALL);
            if ((x - f0) % 5 == 2) {
                set(x, 1, zp0, b("statue")); set(x, 1, zp1, b("statue"));
                set(x, 0, zp0, Blocks.STONE_BRICKS); set(x, 0, zp1, Blocks.STONE_BRICKS);
            }
            if ((x - f0) % 5 == 0) { set(x, 1, zp0, OmbreBlocs.TETE_LAMPADAIRE); set(x, 1, zp1, OmbreBlocs.TETE_LAMPADAIRE); }
        }
        // La tour du pont (rive de la Vieille-Ville), avec sa porte voûtée
        int a0 = f0 - 8, a1 = f0 - 1, c0 = zp0 - 1, c1 = zp1 + 1, h = 26;
        boite(a0, 0, c0, a1, h - 1, c1, b("pierre_noircie"));
        boite(a0, 0, zp0 + 1, a1, 4, zp1 - 1, Blocks.AIR);
        for (int y = 7; y < h; y += 6) boite(a0, y, c0, a1, y, c1, b("pierre_sculptee"));
        // Le sommet : une plate-forme crénelée, et une petite flèche d'ardoise au milieu
        for (int x = a0; x <= a1; x++) for (int z = c0; z <= c1; z++)
            if ((x == a0 || x == a1 || z == c0 || z == c1) && (x + z) % 2 == 0) set(x, h, z, Blocks.STONE_BRICK_WALL);
        int mx = (a0 + a1) / 2, mz = (c0 + c1) / 2;
        boite(mx - 1, h, mz - 1, mx + 1, h + 2, mz + 1, b("ardoise"));
        set(mx, h + 3, mz, b("ardoise"));
        set(mx, h + 4, mz, OmbreBlocs.DRAPEAU);
        set(a1 + 2, h - 2, c0, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        for (int y = 0; y < h; y++) set(a0 - 1, y, c0 + 2, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
        set(a0, 0, c0 - 1, vers(OmbreBlocs.MONTE_CHARGE, Direction.SOUTH));
        tourPontX = a1 + 1; tourPontZ = (c0 + c1) / 2; tourPontH = h; tourPontA0 = a0; tourPontC0 = c0;
        // La petite tour de l'autre rive
        int b0 = f1 + 2, b1 = f1 + 7;
        boite(b0, 0, c0, b1, 17, c1, b("pierre_noircie"));
        boite(b0, 0, zp0 + 1, b1, 4, zp1 - 1, Blocks.AIR);
        boite(b0, 18, c0, b1, 18, c1, b("ardoise"));
        rivePontX = b0; rodolpheX = b1 + 3; rodolpheZ = zp1 + 3;
        set(b1 + 4, 0, zp1 + 5, vers(OmbreBlocs.CARROSSE, Direction.WEST));
        set(b1 + 2, 0, zp0 - 3, vers(b("banc_birch"), Direction.NORTH));
    }

    // ───────────────────────── liaisons ─────────────────────────

    private void tyroliennes() {
        relier(orlojX, orlojH, orlojZ, false, tynX - 2, tynH - 1, tynZ - 1, true);             // de la galerie de l'horloge vers Týn
        relier(tourPontX - 2, tourPontH, tourPontZ, false, rivePontX + 1, 19, tourPontZ, false); // du sommet de la tour du pont vers l'autre rive
    }

    private void relier(int x1, int y1, int z1, boolean socle1, int x2, int y2, int z2, boolean socle2) {
        BlockPos a = new BlockPos(ox + x1, oy + y1, oz + z1), c = new BlockPos(ox + x2, oy + y2, oz + z2);
        if (socle1) set(x1, y1 - 1, z1, b("pierre_noircie"));
        if (socle2) set(x2, y2 - 1, z2, b("pierre_noircie"));
        w.setBlockState(a, OmbreBlocs.POTEAU.getDefaultState(), Block.NOTIFY_ALL);
        w.setBlockState(c, OmbreBlocs.POTEAU.getDefaultState(), Block.NOTIFY_ALL);
        PoteauTyrolienne.relier(w, a, c);
    }

    private void poutresEtLinge() {
        for (int k = 1; k < N; k++) {
            int rue = k * PAS;
            for (int j = 0; j < N; j++) {
                int z = RUE + j * PAS + 4 + r.nextInt(ILOT - 8);
                int hg = hauteur[rue - 1][z], hd = hauteur[rue + RUE][z];
                if (hg > 0 && hd > 0 && r.nextInt(3) > 0) {
                    int y = Math.min(hg, hd) - 1;
                    for (int x = rue; x < rue + RUE; x++) set(x, y, z, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
                }
                int z2 = RUE + j * PAS + 2 + r.nextInt(ILOT - 4);
                if (hauteur[rue - 1][z2] > 9 && hauteur[rue + RUE][z2] > 9 && r.nextBoolean())
                    for (int x = rue; x < rue + RUE; x++) set(x, 8, z2, vers(OmbreBlocs.CORDE_LINGE, Direction.NORTH));
            }
            for (int i = 0; i < N; i++) {
                int x = RUE + i * PAS + 4 + r.nextInt(ILOT - 8);
                int hn = hauteur[x][k * PAS - 1], hs = hauteur[x][k * PAS + RUE];
                if (hn > 0 && hs > 0 && r.nextInt(3) > 0) {
                    int y = Math.min(hn, hs) - 1;
                    for (int z = k * PAS; z < k * PAS + RUE; z++) set(x, y, z, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.Z));
                }
            }
        }
    }

    private void mobilierDeRue() {
        for (int k = 0; k <= N; k++) {
            int rue = k * PAS;
            for (int t = 3; t < VILLE - 3; t += 8) {
                if (t % PAS < RUE) continue;
                if (r.nextInt(5) == 0 && vide(rue + 3, 0, t + 2)) set(rue + 3, 0, t + 2, vers(OmbreBlocs.CARROSSE, Direction.NORTH));
                if (r.nextInt(4) == 0 && vide(rue + RUE - 2, 0, t + 5)) set(rue + RUE - 2, 0, t + 5, vers(OmbreBlocs.CHARRETTE, Direction.NORTH));
                if (r.nextInt(4) == 0) etal(t + 6, rue + 2, Direction.SOUTH, r.nextInt(MARCHANDISES.length));
                if (r.nextInt(4) == 0 && vide(t + 2, 0, rue + RUE - 2) && vide(t + 2, 3, rue + RUE - 2)) {
                    boite(t + 2, 0, rue + RUE - 2, t + 2, 2, rue + RUE - 2, OmbreBlocs.LAMPADAIRE);
                    set(t + 2, 3, rue + RUE - 2, OmbreBlocs.TETE_LAMPADAIRE);
                }
            }
        }
        for (int i = 0; i <= N; i++) for (int j = 0; j <= N; j++)
            if (r.nextInt(3) == 0 && vide(i * PAS + 1, 0, j * PAS + 1)) set(i * PAS + 1, 0, j * PAS + 1, OmbreBlocs.COLONNE);
    }

    // ───────────────────────── les gens ─────────────────────────

    private void gens() {
        Scoreboard sb = w.getScoreboard();
        Team rouges = equipe(sb, "ombre_gardes", Formatting.RED), ors = equipe(sb, "ombre_cibles", Formatting.GOLD);
        // La foule : des groupes dans les rues, beaucoup sur la place, quelques-uns sur le pont
        for (int g = 0; g < 9; g++) {
            int k = r.nextInt(N + 1), t = RUE + r.nextInt(VILLE - 2 * RUE);
            boolean ns = r.nextBoolean();
            int x = ns ? k * PAS + 2 + r.nextInt(3) : t, z = ns ? t : k * PAS + 2 + r.nextInt(3);
            for (int i = 0; i < 3 + r.nextInt(2); i++) passant(x + r.nextInt(3) - 1, z + r.nextInt(3) - 1);
        }
        int px = RUE + PAS + ILOT + 14, pz = RUE + PAS + ILOT + 3;          // le milieu de la grande place
        for (int i = 0; i < 34; i++) passant(px - 22 + r.nextInt(44), pz - 22 + r.nextInt(44));
        for (int[] m : marchands) passant(m[0], m[1], -1, 1);
        if (grueH > 0) for (int i = 0; i < 4; i++) passant(grueX - 10 + r.nextInt(8), grueZ - 2 + r.nextInt(5), 1, 6);      // les ouvriers
        if (jardinX > 0) for (int i = 0; i < 3; i++) passant(jardinX - 4 + r.nextInt(8), jardinZ - 6 + r.nextInt(3), 6, 8);  // les moines
        for (int i = 0; i < 5; i++) passant(VILLE + 2 + r.nextInt(FLEUVE - 4), 2 * PAS + 1 + r.nextInt(RUE - 2));
        // Les personnages réels
        figure(Figure.TYCHO, tychoX, tychoZ);
        figure(Figure.KEPLER, tychoX + 1, tychoZ + 3);
        figure(Figure.RODOLPHE, rodolpheX, rodolpheZ);
        int lx = RUE + PAS + 4, lz = 3;                       // au nord, dans la rue du quartier juif
        figure(Figure.LOEW, lx, lz);
        IronGolemEntity golem = EntityType.IRON_GOLEM.create(w);
        if (golem != null) {
            golem.refreshPositionAndAngles(ox + lx + 3.5, oy, oz + lz + 0.5, 0, 0);
            golem.setPlayerCreated(true);
            golem.setPersistent();
            golem.setCustomName(Text.literal("Golem"));
            golem.setPositionTarget(new BlockPos(ox + lx + 3, oy, oz + lz), 16);
            w.spawnEntity(golem);
        }
        // La garde : hallebardiers dans les rues, par deux ; arbalétriers sur les toits
        for (int g = 0; g < 7; g++) {
            int k = 1 + r.nextInt(N - 1), t = RUE + 4 + r.nextInt(VILLE - 2 * RUE - 8);
            boolean ns = r.nextBoolean();
            int x = ns ? k * PAS + 3 : t, z = ns ? t : k * PAS + 3;
            garde(Entites.HALLEBARDIER, x, 0, z, 10, "Hallebardier", rouges, false);
            garde(Entites.HALLEBARDIER, x + 1, 0, z + 1, 10, "Hallebardier", rouges, false);
        }
        int poses = 0;
        for (int essai = 0; essai < 300 && poses < 10; essai++) {
            int x = r.nextInt(VILLE), z = r.nextInt(PROFONDEUR), h = hauteur[x][z];
            if (h > 0 && !vide(x, h, z) && vide(x, h + 1, z) && vide(x, h + 2, z)) {
                garde(Entites.ARBALETRIER, x, h + 1, z, 3, "Arbalétrier", rouges, false);
                poses++;
            }
        }
        garde(Entites.HALLEBARDIER, tychoX + 3, 0, tychoZ - 6, 6, "Hallebardier de l'hôtel de ville", rouges, false);
        garde(Entites.ARBALETRIER, orlojX - 4, orlojH, orlojZ + 3, 1, "Arbalétrier de l'horloge", rouges, false);
        // Les cibles (personnages inventés) et leur garde
        garde(Entites.CAPITAINE, tourPontA0 + 1, tourPontH, tourPontC0 + 1, 2, "Ottokar von Rabenstein, grand maître du Corbeau", ors, true)
                .addCommandTag("ombre_cible_ottokar");
        garde(Entites.ARBALETRIER, tourPontA0 + 1, tourPontH, tourPontC0 + 7, 2, "Arbalétrier du Corbeau", rouges, false);
        garde(Entites.HALLEBARDIER, tourPontX - 6, 0, tourPontZ - 6, 4, "Hallebardier du pont", rouges, false);
        garde(Entites.CAPITAINE, laboX, laboY, laboZ, 1, "Melchior Kragl, alchimiste du Corbeau", ors, true)
                .addCommandTag("ombre_cible_kragl");
    }

    private void passant(int x, int z) { passant(x, z, -1, 14); }

    /** Un passant ; « sorte » choisit son habit (-1 : au hasard), « rayon » jusqu'où il flâne. */
    private void passant(int x, int z, int sorte, int rayon) {
        Passant p = Entites.PASSANT.create(w);
        if (p == null || x < 0 || z < 0 || x >= LARGEUR || z >= PROFONDEUR || !vide(x, 0, z) || !vide(x, 1, z)) return;
        BlockPos pos = new BlockPos(ox + x, oy, oz + z);
        p.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360, 0);
        p.initialize(w, w.getLocalDifficulty(pos), SpawnReason.COMMAND, null);
        p.setPersistent();
        if (sorte >= 0) p.setVariante(sorte);
        p.setPositionTarget(pos, rayon);
        w.spawnEntity(p);
    }

    private void figure(int qui, int x, int z) {
        Figure f = Entites.FIGURE.create(w);
        if (f == null) return;
        BlockPos pos = new BlockPos(ox + x, oy, oz + z);
        f.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360, 0);
        f.devenir(qui);
        f.setPersistent();
        f.setPositionTarget(pos, 3);
        w.spawnEntity(f);
    }

    private static Team equipe(Scoreboard sb, String nom, Formatting c) {
        Team t = sb.getTeam(nom);
        if (t == null) t = sb.addTeam(nom);
        t.setColor(c);
        return t;
    }

    private <T extends PathAwareEntity> Entity garde(EntityType<T> type, int x, int y, int z, int rayon, String nom, Team equipe, boolean cible) {
        T g = type.create(w);
        if (g == null) return EntityType.MARKER.create(w);   // (ne devrait pas arriver)
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
        return g;
    }
}
