package fr.fenua.ombre.initiation;

import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.ac.Defis;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static fr.fenua.ombre.initiation.Rappel.*;

/**
 * Le Jardin royal du château de Prague (aménagé au XVIᵉ siècle, avec le Belvédère de la reine Anne) :
 * ici, une allée de cours ceintes de murs et de haies taillées, une par nouveauté du cours de rappel.
 */
public final class JardinRoyal extends RuelleDOr {
    private JardinRoyal(ServerWorld w, BlockPos o) { super(w, o, Rappel.COURS); }

    public static int construire(ServerCommandSource src) {
        ServerWorld w = src.getWorld();
        BlockPos p = BlockPos.ofFloored(src.getPosition());
        BlockPos o = p.add(-2, 0, -7);
        new JardinRoyal(w, o).batir();
        Rappel.COURS.retenir(w, o);
        w.setTimeOfDay(1000);
        if (src.getEntity() instanceof ServerPlayerEntity j) Rappel.COURS.demarrer(j, o);
        src.sendFeedback(() -> Text.literal("§6Le Jardin royal est prêt§r : " + NOMS.length + " cours, " + Rappel.COURS.nombre()
                + " leçons — tout ce qui a changé. Suis le panneau en haut à gauche et le losange doré.\n"
                + "§7Pour reprendre à une leçon : /ombre rappel <numéro>."), true);
        return 1;
    }

    /** Pas de maisons ici : un mur de pierre coiffé d'une haie taillée, des niches et des statues. */
    @Override
    protected void facades(int zm, Direction cote) {
        BlockState haie = Blocks.SPRUCE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int x = -1; x <= longueur + 1; x++) {
            boite(x, 0, zm, x, 4, zm, b("pierre_de_taille"));
            set(x, 5, zm, b("pierre_sculptee"));
            boite(x, 6, zm, x, 7, zm, haie);
            boite(x, 8, zm, x, 27, zm, Blocks.BARRIER);
            if (Math.floorMod(x, 9) == 4) {
                int zi = zm == -1 ? 0 : 14;
                set(x, 0, zi, b("statue"));
            }
        }
    }

    private void sol() {
        for (int x = 0; x <= longueur; x++) for (int z = 0; z <= 14; z++)
            set(x, -1, z, z >= 6 && z <= 8 ? Blocks.GRAVEL.getDefaultState() : (x + z) % 5 == 0 ? Blocks.MOSS_BLOCK.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
    }

    @Override
    protected void lesCours() {
        sol();
        bx = debuts[ENTREE]; entree();
        bx = debuts[POUTRE]; poutre();
        bx = debuts[BORD]; bord();
        bx = debuts[ANGLES]; angles();
        bx = debuts[FACADE]; facade();
        bx = debuts[FRANCHIR]; franchir();
        bx = debuts[BONDS]; bonds();
        bx = debuts[DESCENTE]; descente();
        bx = debuts[BUISSONS]; buissons();
        bx = debuts[CINEMA]; cinema();
        bx = debuts[AIGLE]; aigle();
        bx = debuts[FOLIE]; folie();
        bx = debuts[PLUMES]; plumes();
        bx = debuts[PRAGUE]; prague();
    }

    private void entree() {
        c(6, 0, 3, OmbreBlocs.FONTAINE);
        c(3, 0, 12, vers(b("cabine_blue"), Direction.NORTH));
        c(8, 0, 12, vers(b("cabine_red"), Direction.NORTH));
        panneau(3, 0, 5, "JARDIN ROYAL", "Cours de", "rappel", "§fTouche H : aide");
    }

    /** Un palier, une poutre à 5 au-dessus d'un bassin, un palier d'arrivée. */
    private void poutre() {
        cb(1, 0, 0, 5, 3, 14, Blocks.SMOOTH_STONE);
        cb(0, 0, 0, 0, 0, 14, Blocks.SMOOTH_STONE);
        eau(6, 0, 12, 14);
        cb(9, 5, 1, 9, 5, 13, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.Z));
        cb(9, -3, 0, 9, 4, 0, Blocks.STRIPPED_SPRUCE_LOG); cb(9, -3, 14, 9, 4, 14, Blocks.STRIPPED_SPRUCE_LOG);
        cb(14, 0, 0, 17, 0, 14, Blocks.SMOOTH_STONE);
    }

    /** Un mur de 8, un mannequin sur son sommet. */
    private void bord() {
        cb(8, 0, 0, 10, 7, 14, b("pierre_de_taille"));
        cb(8, 3, 0, 10, 3, 14, b("pierre_sculptee"));
        meule(12, 5, 14, 9);
    }

    /** Un pilier de 3 × 3, haut de 8, au milieu de l'eau. */
    private void angles() {
        eau(3, 1, 13, 13);
        cb(7, -3, 6, 9, 7, 8, b("brique_ancienne"));
        cb(7, 3, 6, 9, 3, 8, b("pierre_sculptee"));
        cb(14, 0, 1, 15, 0, 13, Blocks.SMOOTH_STONE);
    }

    /** Une façade à fenêtres le long du mur nord, avec un trou de trois blocs au milieu. */
    private void facade() {
        for (int x = 1; x <= 31; x++) {
            if (x >= 15 && x <= 17) continue;
            cb(x, 0, 0, x, 8, 1, b(x < 15 ? "crepi_jaune" : "crepi_rose"));
            if (x % 2 == 0) for (int y : new int[]{2, 5}) { c(x, y, 1, Blocks.GLASS_PANE); c(x, y + 1, 1, Blocks.GLASS_PANE); }   // des fenêtres, comme à Prague
            c(x, 8, 1, b("pierre_sculptee"));
        }
        meule(14, 4, 18, 6);
    }

    /** Quatre rangées d'obstacles hauts d'un bloc et demi : barrières, murets, un étal, une charrette. */
    private void franchir() {
        for (int z = 0; z <= 14; z++) {
            c(6, 0, z, Blocks.OAK_FENCE);
            c(12, 0, z, Blocks.COBBLESTONE_WALL);
            c(18, 0, z, z % 2 == 0 ? Blocks.SPRUCE_FENCE.getDefaultState() : Blocks.SPRUCE_FENCE.getDefaultState());
            c(24, 0, z, Blocks.STONE_BRICK_WALL);
        }
        for (int z = 6; z <= 8; z++) c(18, 0, z, vers(b("etal_fruits"), Direction.WEST));
    }

    /** Quatre toits de 5 au-dessus de l'eau, écartés de 2, 3 puis 4 blocs. */
    private void bonds() {
        cb(1, 0, 0, 1, 0, 14, Blocks.SMOOTH_STONE); cb(2, 0, 0, 2, 1, 14, Blocks.SMOOTH_STONE);
        cb(3, 0, 0, 3, 2, 14, Blocks.SMOOTH_STONE); cb(4, 0, 0, 4, 3, 14, Blocks.SMOOTH_STONE);
        eau(5, 0, 33, 14);
        int[][] toits = {{5, 9}, {12, 15}, {19, 22}, {27, 33}};
        for (int[] t : toits) {
            cb(t[0], -3, 3, t[1], 4, 11, b("crepi_ocre"));
            cb(t[0], 4, 3, t[1], 4, 11, b("tuiles"));
        }
    }

    /** Une tour de 9 et son échelle ; une meule au pied, côté est. */
    private void descente() {
        cb(4, 0, 5, 8, 8, 9, b("pierre_noircie"));
        for (int y = 0; y <= 8; y++) c(3, y, 7, Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST));
        meule(10, 5, 12, 9);
    }

    /** Une plante de deux blocs, posée d'un coup (sans que la moitié du bas ne se casse en attendant celle du haut). */
    private void plante(int x, int z, Block g) {
        int f = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        w.setBlockState(o.add(bx + x, 0, z), g.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), f);
        w.setBlockState(o.add(bx + x, 1, z), g.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), f);
    }

    /** Des massifs de lilas et de rosiers (deux blocs de haut) ; un buisson marqué d'une lanterne. */
    private void buissons() {
        Block[] grands = {Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LARGE_FERN};
        for (int x = 2; x <= 22; x++) for (int z = 1; z <= 13; z++) {
            if (z >= 6 && z <= 8 && x > 10) continue;
            if (x == 8 && z == 3) continue;
            if ((x * 7 + z * 3) % 4 == 0 || (x >= 6 && x <= 10 && z <= 5)) plante(x, z, grands[Math.floorMod(x + z, grands.length)]);
        }
        plante(8, 3, Blocks.LILAC);
        cb(7, 0, 2, 7, 1, 2, Blocks.SPRUCE_FENCE); c(7, 2, 2, Blocks.LANTERN);
    }

    /** Une tour de 18 avec gouttière et gargouille ; une meule au pied. */
    private void cinema() {
        cb(3, 0, 6, 5, 17, 8, b("pierre_noircie"));
        for (int y = 5; y < 18; y += 6) cb(3, y, 6, 5, y, 8, b("pierre_sculptee"));
        for (int y = 0; y <= 17; y++) c(2, y, 7, vers(OmbreBlocs.GOUTTIERE, Direction.EAST));
        c(6, 17, 7, vers(OmbreBlocs.GARGOUILLE, Direction.WEST));
        meule(8, 5, 11, 9);
        Entity m = EntityType.MARKER.create(w);
        if (m != null) {
            m.refreshPositionAndAngles(o.getX() + bx + 6.5, o.getY() + 18, o.getZ() + 7.5, 0, 0);
            m.setCustomName(Text.literal("Belvédère du Jardin royal"));
            m.addCommandTag(Gardes.LIEU);
            w.spawnEntity(m);
        }
    }

    /** Au fond, des murs qui cachent trois mannequins à qui regarde depuis le sol. */
    private void aigle() {
        cb(20, 0, 0, 20, 4, 14, b("pierre_de_taille"));
        cb(20, 0, 6, 20, 2, 8, Blocks.AIR);
        cb(22, 0, 4, 28, 3, 4, b("brique_ancienne"));
        cb(22, 0, 10, 28, 3, 10, b("brique_ancienne"));
        cb(32, 0, 0, 35, 0, 14, Blocks.SMOOTH_STONE);
    }

    /** Une fosse de trois blocs, deux gardes armés au fond ; on tire d'en haut. */
    private void folie() {
        cb(14, -4, 2, 22, -4, 12, b("pave"));
        cb(15, -3, 3, 21, -1, 11, Blocks.AIR);
        for (int x = 14; x <= 22; x++) for (int z = 2; z <= 12; z++)
            if (x == 14 || x == 22 || z == 2 || z == 12) cb(x, -3, z, x, -1, z, b("pierre_de_taille"));
        for (int x = 15; x <= 21; x += 3) c(x, -3, 3, Blocks.LANTERN);
        panneau(12, 0, 3, "FLÉCHETTE", "DE FOLIE", "Tire sur l'un", "des deux");
    }

    /** Trois plumes sur des hauteurs : un pilier, une poutre, un toit. */
    private void plumes() {
        cb(5, 0, 2, 6, 5, 3, b("pierre_de_taille"));
        cb(12, 0, 10, 12, 5, 10, Blocks.STRIPPED_SPRUCE_LOG); cb(18, 0, 10, 18, 5, 10, Blocks.STRIPPED_SPRUCE_LOG);
        cb(13, 6, 10, 17, 6, 10, OmbreBlocs.POUTRE.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
        cb(13, 0, 9, 17, 0, 9, Blocks.HAY_BLOCK);
        cb(22, 0, 1, 26, 4, 5, b("crepi_vert")); cb(22, 5, 1, 26, 5, 5, b("tuiles"));
        double[][] q = {{5.5, 6, 2.5}, {15.5, 7, 10.5}, {24.5, 6, 3.5}};
        for (double[] p : q) Defis.poser(w, o.getX() + bx + p[0], o.getY() + p[1], o.getZ() + p[2], Defis.PLUME, null);
        // (elles sont marquées « école » : elles ne comptent pas parmi les quinze de Prague)
        for (Entity e : w.getEntitiesByType(EntityType.MARKER, new net.minecraft.util.math.Box(o.add(bx, -1, 0).toCenterPos(), o.add(bx + 30, 12, 15).toCenterPos()),
                e -> e.getCommandTags().contains(Defis.PLUME))) e.addCommandTag(Defis.ECOLE);
        c(28, 0, 2, OmbreBlocs.DRAPEAU);
    }

    private void prague() {
        c(7, 0, 7, OmbreBlocs.FONTAINE);
        c(7, 0, 2, OmbreBlocs.DRAPEAU);
        c(7, 0, 12, OmbreBlocs.DRAPEAU);
        panneau(11, 0, 7, "RAPPEL", "TERMINÉ", "Tape", "/ombre prague");
    }
}
