package fr.fenua.ombre.initiation;

import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.Ombre;
import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.bloc.PoteauTyrolienne;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;

import java.util.Random;

import static fr.fenua.ombre.initiation.Initiation.*;

/**
 * La Ruelle d'Or du château de Prague (Zlatá ulička), où l'Ombre forme ses novices.
 * La vraie ruelle est une rangée de toutes petites maisons de couleur, bâties à la fin du XVIᵉ siècle
 * contre le rempart du château ; ici, elle s'étire vers l'est en vingt cours, une par leçon.
 *
 * Coordonnées locales : x vers l'est (0 = début de la première cour), z de 0 (mur nord) à 14 (mur sud), y = 0 au sol.
 */
public class RuelleDOr {
    private static final int HAUT = 27;                       // le plafond invisible des murs (barrière)
    private static final String[] FACADES = {"crepi", "crepi_jaune", "crepi_rose", "crepi_bleu", "crepi_vert", "crepi_ocre", "sgraffite"};

    protected final ServerWorld w;
    protected final BlockPos o;
    protected final Random r = new Random(1597);
    protected int bx;                                         // début de la cour en cours de construction
    protected final Cours cours;
    protected final int[] debuts;
    protected final String[] noms;
    protected final int longueur;

    protected RuelleDOr(ServerWorld w, BlockPos o, Cours cours) {
        this.w = w; this.o = o; this.cours = cours;
        this.debuts = cours.debuts; this.noms = cours.noms; this.longueur = cours.longueur;
    }

    public static int construire(ServerCommandSource src) {
        ServerWorld w = src.getWorld();
        BlockPos p = BlockPos.ofFloored(src.getPosition());
        BlockPos o = p.add(-2, 0, -7);
        new RuelleDOr(w, o, Initiation.COURS).batir();
        Initiation.COURS.retenir(w, o);
        w.setTimeOfDay(1000);
        if (src.getEntity() instanceof ServerPlayerEntity j) Initiation.COURS.demarrer(j, o);
        src.sendFeedback(() -> Text.literal("§6La Ruelle d'Or est prête§r : " + NOMS.length + " cours, " + Initiation.nombre()
                + " étapes. Suis le panneau à gauche de l'écran et le losange doré de la boussole.\n"
                + "§7Pour reprendre à une étape : /ombre initiation <numéro>. Une fois fini : /ombre prague."), true);
        return 1;
    }

    // ───────────────────────── outils ─────────────────────────

    protected static Block b(String nom) { return Registries.BLOCK.get(Identifier.of(Ombre.MOD_ID, nom)); }
    protected void set(int x, int y, int z, BlockState s) { w.setBlockState(o.add(x, y, z), s, Block.NOTIFY_LISTENERS); }
    protected void set(int x, int y, int z, Block bl) { set(x, y, z, bl.getDefaultState()); }
    protected boolean vide(int x, int y, int z) { return w.getBlockState(o.add(x, y, z)).isAir(); }
    protected void boite(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }
    protected void boite(int x0, int y0, int z0, int x1, int y1, int z1, Block bl) { boite(x0, y0, z0, x1, y1, z1, bl.getDefaultState()); }
    protected static BlockState vers(Block bl, Direction d) {
        BlockState s = bl.getDefaultState();
        return s.contains(Properties.HORIZONTAL_FACING) ? s.with(Properties.HORIZONTAL_FACING, d) : s;
    }

    // Les mêmes, dans la cour en cours (x relatif à son début)
    protected void c(int x, int y, int z, BlockState s) { set(bx + x, y, z, s); }
    protected void c(int x, int y, int z, Block bl) { set(bx + x, y, z, bl); }
    protected void cb(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) { boite(bx + x0, y0, z0, bx + x1, y1, z1, s); }
    protected void cb(int x0, int y0, int z0, int x1, int y1, int z1, Block bl) { boite(bx + x0, y0, z0, bx + x1, y1, z1, bl); }
    protected void meule(int x0, int z0, int x1, int z1) { cb(x0, 0, z0, x1, 1, z1, OmbreBlocs.MEULE); }
    protected void eau(int x0, int z0, int x1, int z1) {
        cb(x0, -4, z0, x1, -4, z1, Blocks.STONE_BRICKS);
        cb(x0, -3, z0, x1, -1, z1, Blocks.WATER);
    }
    protected void herbes(int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                c(x, -1, z, Blocks.GRASS_BLOCK);
                c(x, 0, z, Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
                c(x, 1, z, Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
            }
    }

    /** Un panneau tourné vers l'ouest (vers le joueur qui arrive), texte doré lumineux. */
    protected void panneau(int x, int y, int z, String... l) {
        BlockPos p = o.add(bx + x, y, z);
        w.setBlockState(p, Blocks.DARK_OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, RotationPropertyHelper.fromDirection(Direction.WEST)), Block.NOTIFY_ALL);
        if (w.getBlockEntity(p) instanceof SignBlockEntity sbe) {
            SignText t = new SignText().withColor(DyeColor.YELLOW).withGlowing(true);
            for (int i = 0; i < 4 && i < l.length; i++) t = t.withMessage(i, Text.literal(l[i]));
            sbe.setText(t, true);
        }
    }

    // ───────────────────────── l'ensemble ─────────────────────────

    protected void batir() {
        // Place nette : on retire les anciens habitants (sauf les joueurs) et tout ce qui dépasse
        Box zone = new Box(o.getX() - 4, o.getY() - 6, o.getZ() - 4, o.getX() + longueur + 4, o.getY() + 32, o.getZ() + 19);
        for (Entity e : w.getEntitiesByClass(Entity.class, zone, e -> !(e instanceof PlayerEntity))) e.discard();
        for (int x = -3; x <= longueur + 3; x++)
            for (int z = -3; z <= 17; z++) {
                for (int y = 0; y <= HAUT + 1; y++) if (!vide(x, y, z)) set(x, y, z, Blocks.AIR);
                set(x, -1, z, b("pave"));
                for (int y = -4; y <= -2; y++) set(x, y, z, Blocks.DIRT);
            }
        facades(-1, Direction.NORTH);
        facades(15, Direction.SOUTH);
        for (int i = 0; i <= noms.length; i++) muraille(i);
        lesCours();
        panneaux();
    }

    /** Les épreuves de chaque cour. */
    protected void lesCours() {
        bx = S[ENTREE]; entree();
        bx = S[ESCALADE]; escalade();
        bx = S[ARRIERE]; arriere();
        bx = S[REBORDS]; rebords();
        bx = S[BONDS]; bonds();
        bx = S[MUR]; murCouru();
        bx = S[PERCHES]; perches();
        bx = S[TYRO]; tyrolienne();
        bx = S[VUE]; pointDeVue();
        bx = S[ARMURERIE]; armurerie();
        bx = S[CACHETTES]; cachettes();
        bx = S[FOULE]; foule();
        bx = S[ASSASSINATS]; courDArmes();
        bx = S[DISTANCE]; armesDeJet();
        bx = S[DEGUISE]; deguisement();
        bx = S[ZONE]; zoneInterdite();
        bx = S[NOTORIETE]; notoriete();
        bx = S[VOL]; vol();
        bx = S[GRAPPIN]; grappin();
        bx = S[FIN]; sortie();
    }

    /** Un panneau et deux lanternes à l'entrée de chaque cour. */
    protected void panneaux() {
        for (int i = 0; i < noms.length; i++) {
            bx = debuts[i];
            String n = noms[i];
            int coupe = n.length() > 15 ? Math.max(1, n.lastIndexOf(' ', 15)) : -1;
            if (vide(bx, 0, 2)) panneau(0, 0, 2, "§l" + (i + 1) + ".", coupe > 0 ? n.substring(0, coupe) : n, coupe > 0 ? n.substring(coupe + 1) : "", "");
            for (int z : new int[]{0, 14})
                if (vide(bx, 0, z) && vide(bx, 1, z) && vide(bx, 2, z)) {
                    c(0, 0, z, OmbreBlocs.LAMPADAIRE); c(0, 1, z, OmbreBlocs.LAMPADAIRE); c(0, 2, z, OmbreBlocs.TETE_LAMPADAIRE);
                }
        }
    }

    /** Une rangée de petites maisons de couleur contre le mur nord (z = -1) ou sud (z = 15). */
    protected void facades(int zm, Direction cote) {
        int zi = zm == -1 ? 0 : 14;                              // la case devant la façade, côté ruelle
        int x = -1;
        while (x <= longueur + 1) {
            int x1 = Math.min(longueur + 1, x + 3 + r.nextInt(3));
            int h = 6 + r.nextInt(3);
            Block mur = b(FACADES[r.nextInt(FACADES.length)]);
            boite(x, 0, zm, x1, h - 1, zm, mur);
            boite(x, 0, zm, x1, 1, zm, b("pierre_de_taille"));
            boite(x, h - 1, zm, x1, h - 1, zm, b("pierre_sculptee"));
            for (int k = x; k <= x1; k++) {
                if ((k - x) % 2 == 1) { set(k, 3, zm, Blocks.GLASS_PANE); set(k, 4, zm, Blocks.GLASS_PANE); }
                set(k, h, zm, b("tuiles_dalle"));
            }
            int milieu = (x + x1) / 2;
            set(milieu, 0, zm, Blocks.SPRUCE_PLANKS);
            set(milieu, 1, zm, Blocks.SPRUCE_PLANKS);
            if (r.nextInt(3) == 0) set(milieu, 2, zi, vers(OmbreBlocs.REVERBERE, cote));
            else if (r.nextInt(3) == 0) set(milieu + 1, 2, zi, vers(b("volets_vert"), cote));
            boite(x, h + 1, zm, x1, HAUT, zm, Blocks.BARRIER);      // on ne sort pas par les toits
            x = x1 + 1;
        }
    }

    /** Le mur qui ferme la cour i à l'ouest, avec sa grille (fermée) ; i = NOMS.length : le mur du bout. */
    protected void muraille(int i) {
        int x = i < noms.length ? debuts[i] - 1 : longueur + 1;
        boite(x, 0, -1, x, 8, 15, b("pierre_de_taille"));
        boite(x, 5, -1, x, 5, 15, b("pierre_sculptee"));
        boite(x, 9, -1, x, HAUT, 15, Blocks.BARRIER);
        if (i == 0 || i == noms.length) return;
        boite(x, 0, 5, x, 4, 9, b("pierre_sculptee"));
        boite(x, 0, 6, x, 3, 8, Blocks.IRON_BARS);
        set(x, 5, 7, b("pierre_noircie"));
    }

    // ───────────────────────── les cours ─────────────────────────

    private void entree() {
        c(6, 0, 3, OmbreBlocs.FONTAINE);
        c(3, 0, 12, vers(OmbreBlocs.BANC, Direction.NORTH));
        c(8, 0, 12, vers(b("banc_spruce"), Direction.NORTH));
        panneau(3, 0, 5, "RUELLE D'OR", "Château de", "Prague, 1600", "§fTouche H : aide");
    }

    /** 2. Un mur de 8 avec une gouttière ; du foin derrière. */
    private void escalade() {
        cb(6, 0, 0, 7, 7, 14, b("pierre_de_taille"));
        cb(6, 3, 0, 7, 3, 14, b("pierre_sculptee"));
        for (int y = 0; y <= 7; y++) c(5, y, 11, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
        meule(9, 5, 11, 9);
    }

    /** 3. Un mur suspendu (on passe dessous), un mur plein en face : on y monte par un saut arrière. */
    private void arriere() {
        cb(3, 5, 0, 4, 9, 14, b("brique_ancienne"));
        cb(8, 0, 0, 9, 8, 14, b("pierre_de_taille"));
        cb(8, 4, 0, 9, 4, 14, b("pierre_sculptee"));
        meule(0, 5, 2, 9);
        meule(11, 5, 13, 9);
    }

    /** 4. Palier, bassin, trois murs de 5, 7 et 9 à saisir au vol. */
    private void rebords() {
        cb(0, 0, 0, 0, 0, 14, Blocks.SMOOTH_STONE);
        cb(1, 0, 0, 1, 1, 14, Blocks.SMOOTH_STONE);
        cb(2, 0, 0, 4, 2, 14, Blocks.SMOOTH_STONE);
        eau(5, 0, 14, 14);
        cb(7, -3, 0, 8, 4, 14, Blocks.STONE_BRICKS);
        cb(11, -3, 0, 12, 6, 14, Blocks.STONE_BRICKS);
        cb(15, 0, 0, 16, 8, 14, Blocks.STONE_BRICKS);
        cb(11, 3, 0, 12, 3, 14, Blocks.MOSSY_STONE_BRICKS);
        meule(18, 5, 20, 9);
    }

    /** 5. Des marches d'un bloc, un palier, puis des piliers au-dessus de l'eau : 2, 3, 4 et 3 blocs d'écart. */
    private void bonds() {
        for (int i = 0; i < 4; i++) cb(i * 2, 0, 0, i * 2 + 1, i, 14, Blocks.SMOOTH_STONE);
        cb(8, 0, 0, 11, 3, 14, Blocks.SMOOTH_STONE);
        eau(12, 0, 32, 14);
        for (int[] p : new int[][]{{14, 16}, {20, 22}, {27, 29}}) cb(p[0], -3, 6, p[1], 3, 8, Blocks.POLISHED_DEEPSLATE);
        cb(33, 0, 0, 36, 3, 14, Blocks.SMOOTH_STONE);
        for (int i = 0; i < 3; i++) cb(37 + i, 0, 0, 37 + i, 2 - i, 14, Blocks.SMOOTH_STONE);
    }

    /** 6. Un grand mur au nord d'un bassin de 10 blocs : on court dessus. */
    private void murCouru() {
        cb(0, 0, 0, 16, 5, 3, Blocks.STONE_BRICKS);
        cb(0, 2, 3, 16, 2, 3, Blocks.MOSSY_STONE_BRICKS);
        eau(4, 4, 13, 14);
    }

    /** 7. Trois perches au-dessus d'un bassin. */
    private void perches() {
        cb(0, 0, 0, 0, 0, 14, Blocks.SMOOTH_STONE);
        cb(1, 0, 0, 1, 1, 14, Blocks.SMOOTH_STONE);
        cb(2, 0, 0, 2, 2, 14, Blocks.SMOOTH_STONE);
        cb(3, 0, 0, 6, 3, 14, Blocks.SMOOTH_STONE);
        eau(7, 0, 25, 14);
        BlockState barre = OmbreBlocs.PERCHE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.Z);
        for (int x : new int[]{10, 15, 20}) {
            cb(x, 5, 3, x, 5, 11, barre);
            cb(x, -3, 2, x, 5, 2, Blocks.SPRUCE_LOG);
            cb(x, -3, 12, x, 5, 12, Blocks.SPRUCE_LOG);
        }
        cb(26, 0, 0, 30, 2, 14, Blocks.SMOOTH_STONE);
        cb(31, 0, 0, 31, 1, 14, Blocks.SMOOTH_STONE);
    }

    /** 8. Une tour de 16 avec son monte-charge ; une tyrolienne jusqu'à une plate-forme. */
    private void tyrolienne() {
        cb(2, 0, 5, 4, 15, 9, b("pierre_noircie"));
        for (int y = 4; y < 16; y += 5) cb(2, y, 5, 4, y, 9, b("pierre_sculptee"));
        c(1, 0, 5, vers(OmbreBlocs.MONTE_CHARGE, Direction.EAST));        // pas sur le point d'arrivée de la cour
        cb(24, 0, 4, 28, 5, 10, Blocks.SPRUCE_PLANKS);
        c(3, 16, 7, OmbreBlocs.POTEAU);
        c(25, 6, 7, OmbreBlocs.POTEAU);
        PoteauTyrolienne.relier(w, o.add(bx + 3, 16, 7), o.add(bx + 25, 6, 7));
        meule(29, 5, 31, 9);
    }

    /** 9. Une tour de 18 avec gouttière et gargouille (point de vue), une meule au pied. */
    private void pointDeVue() {
        cb(3, 0, 6, 5, 17, 8, b("pierre_noircie"));
        for (int y = 5; y < 18; y += 6) cb(3, y, 6, 5, y, 8, b("pierre_sculptee"));
        for (int y = 0; y <= 17; y++) c(2, y, 7, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
        c(6, 17, 7, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        meule(8, 5, 11, 9);
        Entity m = EntityType.MARKER.create(w);
        if (m != null) {
            m.refreshPositionAndAngles(o.getX() + bx + 6.5, o.getY() + 18, o.getZ() + 7.5, 0, 0);
            m.setCustomName(Text.literal("Tour de la Ruelle d'Or"));
            m.addCommandTag(Gardes.LIEU);
            w.spawnEntity(m);
        }
    }

    private void armurerie() {
        c(3, 0, 0, vers(OmbreBlocs.ETAGERE, Direction.NORTH));
        c(5, 0, 0, vers(OmbreBlocs.ETAGERE, Direction.NORTH));
        c(4, 0, 14, vers(OmbreBlocs.TONNEAU, Direction.SOUTH));
        c(7, 0, 14, vers(OmbreBlocs.TONNEAU, Direction.SOUTH));
        c(8, 0, 2, vers(OmbreBlocs.PUPITRE, Direction.WEST));
    }

    /** 11. Hautes herbes, meule, puits, cabine, banc : toutes les cachettes. */
    private void cachettes() {
        herbes(2, 1, 7, 5);
        meule(10, 11, 11, 12);
        c(15, 0, 3, OmbreBlocs.PUITS);
        c(19, 0, 12, vers(b("cabine_red"), Direction.NORTH));
        c(6, 0, 13, vers(OmbreBlocs.BANC, Direction.NORTH));
        c(14, 0, 13, vers(OmbreBlocs.CHARRETTE, Direction.WEST));
    }

    /** 12. Une rue étroite entre deux rangées d'échoppes ; un garde au bout. */
    private void foule() {
        cb(2, 0, 0, 22, 3, 4, b("crepi_ocre"));
        cb(2, 0, 10, 22, 3, 14, b("crepi_rose"));
        cb(2, 4, 0, 22, 4, 4, b("tuiles"));
        cb(2, 4, 10, 22, 4, 14, b("tuiles"));
        String[] auvents = {"auvent_raye_rouge_blanc", "auvent_raye_vert_blanc", "auvent_raye_bleu_jaune", "auvent_yellow"};
        for (int x = 3; x <= 21; x += 3) {
            c(x, 3, 5, vers(b(auvents[(x / 3) % 4]), Direction.NORTH));
            c(x + 1, 3, 9, vers(b(auvents[(x / 3 + 1) % 4]), Direction.SOUTH));
        }
    }

    /** 13. La cour d'armes : un mannequin face au mur, une poutre, un rempart, deux mannequins côte à côte. */
    private void courDArmes() {
        BlockState poutre = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z);
        cb(12, 0, 1, 12, 3, 1, Blocks.STRIPPED_SPRUCE_LOG);
        cb(12, 4, 1, 12, 4, 13, poutre);
        cb(12, 0, 13, 12, 3, 13, Blocks.STRIPPED_SPRUCE_LOG);
        cb(19, 0, 0, 20, 3, 14, b("pierre_de_taille"));
        cb(19, 2, 0, 20, 2, 14, b("pierre_sculptee"));
        c(29, 0, 2, vers(b("tonneau_biere"), Direction.WEST));
    }

    /** 14. Un canal ; de l'autre côté, une tourelle et un mannequin. */
    private void armesDeJet() {
        eau(8, 0, 12, 14);
        cb(18, 0, 2, 20, 4, 4, b("pierre_noircie"));
    }

    /** 15. Deux murets, et un passage entre eux, tenu par deux gardes. */
    private void deguisement() {
        cb(18, 0, 0, 18, 5, 4, Blocks.STONE_BRICKS);
        cb(18, 0, 10, 18, 5, 14, Blocks.STONE_BRICKS);
        c(19, 0, 2, vers(OmbreBlocs.DRAPEAU, Direction.WEST));
    }

    /** 16. Un étendard de la garde, des herbes le long du mur sud, un pupitre à atteindre. */
    private void zoneInterdite() {
        c(16, 0, 1, vers(OmbreBlocs.ETENDARD, Direction.SOUTH));
        herbes(3, 11, 28, 13);
        meule(8, 2, 9, 3);
        c(19, 0, 9, vers(OmbreBlocs.TONNEAU, Direction.NORTH));
        c(20, 0, 9, vers(OmbreBlocs.TONNEAU, Direction.NORTH));
        c(25, 0, 3, vers(OmbreBlocs.PUPITRE, Direction.WEST));
    }

    /** 17. Des avis de recherche sur les murs. */
    private void notoriete() {
        c(4, 1, 0, vers(b("affiche_recherche"), Direction.NORTH));
        c(10, 1, 0, vers(b("affiche_recherche"), Direction.NORTH));
        c(7, 1, 14, vers(b("affiche_recherche"), Direction.SOUTH));
        c(8, 0, 7, OmbreBlocs.COLONNE);
    }

    /** 18. Un étal ; des passants qui le regardent. */
    private void vol() {
        String[] etals = {"etal_fruits", "etal_pain", "etal_fleurs", "etal_legumes", "etal_poisson", "etal_etoffes", "etal_fruits"};
        for (int z = 4; z <= 10; z++) c(11, 0, z, vers(b(etals[z - 4]), Direction.WEST));
    }

    /** 19. Une haute maison de 16 : le lance-grappin. */
    private void grappin() {
        cb(10, 0, 0, 13, 15, 14, b("crepi_jaune"));
        cb(10, 0, 0, 13, 2, 14, b("pierre_de_taille"));
        for (int y = 5; y < 15; y += 4)
            for (int z = 1; z < 14; z += 3) { c(10, y, z, Blocks.GLASS_PANE); c(10, y + 1, z, Blocks.GLASS_PANE); }
        cb(10, 15, 0, 13, 15, 14, b("pierre_sculptee"));
        meule(15, 5, 17, 9);
    }

    private void sortie() {
        c(7, 0, 7, OmbreBlocs.FONTAINE);
        c(7, 0, 2, OmbreBlocs.DRAPEAU);
        c(7, 0, 12, OmbreBlocs.DRAPEAU);
        panneau(11, 0, 7, "INITIATION", "RÉUSSIE", "Tape", "/ombre prague");
    }
}
