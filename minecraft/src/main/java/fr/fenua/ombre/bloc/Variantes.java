package fr.fenua.ombre.bloc;

import fr.fenua.ombre.Ombre;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Les variantes, fabriquées en série : couleurs, bois, pierres, enseignes de boutique, affiches…
 * Chaque variante appartient à la même famille (classe) que l'original, donc garde son effet
 * (l'auvent amortit, l'enseigne se saisit, la cabine cache…). Leurs modèles et textures sont
 * produits par outils/generer-ressources.py.
 */
public final class Variantes {
    private Variantes() {}

    public static final String[] COULEURS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    public static final String[] RAYURES = {"rouge_blanc", "vert_blanc", "bleu_blanc", "jaune_blanc", "noir_blanc", "rouge_jaune", "bleu_jaune", "vert_jaune"};
    public static final String[] BOUTIQUES = {"boulangerie", "cafe", "pharmacie", "horloger", "librairie", "boucherie",
            "chapelier", "barbier", "cordonnier", "fleuriste", "tailleur", "marchand_vin"};
    public static final String[] TEINTES_VOLETS = {"vert", "bleu", "rouge", "gris", "blanc", "brun", "jaune", "noir"};
    public static final String[] BOIS = {"spruce", "birch", "dark_oak", "acacia", "cherry", "mangrove"};
    public static final String[] MARCHANDISES = {"fruits", "poisson", "fleurs", "pain", "etoffes", "legumes"};
    public static final String[] PIERRES = {"sandstone", "quartz", "calcite", "bricks", "deepslate", "andesite"};
    public static final String[] FERS = {"vert_de_gris", "dore", "blanc"};
    public static final String[] CHEMINEES = {"pierre", "terre_cuite", "ardoise"};
    public static final String[] AFFICHES = {"theatre", "cirque", "exposition", "bal", "chemin_de_fer", "ballon",
            "recherche", "chocolat", "opera", "savon", "velocipede", "proclamation"};
    public static final String[] FANIONS = {"rouge", "bleu", "vert", "jaune", "blanc", "noir", "violet", "orange"};
    public static final String[] REVERBERES = {"vert", "dore"};
    /** Blocs de construction parisiens, avec leurs propres textures. */
    public static final String[] CONSTRUCTION = {"pierre_de_paris", "pierre_de_paris_lisse", "pierre_de_paris_sculptee",
            "zinc", "ardoise", "pave_parisien", "trottoir", "parquet", "carrelage", "papier_peint_rouge", "papier_peint_vert",
            "papier_peint_bleu", "papier_peint_jaune", "papier_peint_raye", "papier_peint_fleuri", "marbre_blanc",
            "marbre_noir", "brique_parisienne", "crepi", "devanture", "devanture_rouge", "boiserie"};
    /** Ceux-là ont aussi leurs escaliers et leurs dalles (toits, corniches, trottoirs). */
    public static final String[] AVEC_ESCALIERS = {"zinc", "ardoise", "pierre_de_paris", "pave_parisien"};

    /** Tous les blocs du décor parisien (pour l'onglet « Paris » et la vitrine). */
    public static final List<Block> DECOR = new ArrayList<>();
    /** Les variantes de blocs de parkour (pour l'onglet « Parkour »). */
    public static final List<Block> PARKOUR = new ArrayList<>();
    /** Les blocs dont les trous doivent rester transparents. */
    public static final List<Block> AJOURES = new ArrayList<>();

    public static void enregistrer() {
        for (String c : COULEURS) {
            parkour("auvent_" + c, new Mobilier.Auvent(laine()));
            parkour("cabine_" + c, new Cachettes.Cabine(OmbreBlocs.bois()));
        }
        for (String r : RAYURES) parkour("auvent_raye_" + r, new Mobilier.Auvent(laine()));
        for (String b : BOUTIQUES) parkour("enseigne_" + b, new Mobilier.Enseigne(OmbreBlocs.bois().noCollision()));
        for (String b : BOIS) {
            parkour("poutre_" + b, new Mobilier.Poutre(OmbreBlocs.bois()));
            decor("banc_" + b, new Decor.Banc(OmbreBlocs.bois()).formes(true, OmbreBlocs.BANC_CONTOUR, null));
        }
        for (String p : PIERRES) parkour("corniche_" + p, new Mobilier.Corniche(OmbreBlocs.pierre()));
        for (String f : FERS) {
            Block b = new Decor.Balcon(OmbreBlocs.metal()).formes(true, OmbreBlocs.BALCON_CONTOUR, null);
            parkour("balcon_" + f, b);
            AJOURES.add(b);
        }
        for (String r : REVERBERES) {
            Block b = new Decor.Reverbere(OmbreBlocs.metal().noCollision().luminance(s -> 15))
                    .formes(true, OmbreBlocs.REVERBERE_CONTOUR, Decor.VIDE);
            parkour("reverbere_" + r, b);
            AJOURES.add(b);
        }
        for (String t : TEINTES_VOLETS)
            decor("volets_" + t, new Decor.Oriente(OmbreBlocs.bois().noCollision()).formes(true, OmbreBlocs.VOLETS_CONTOUR, Decor.VIDE));
        for (String m : MARCHANDISES) {
            Block b = new Decor.Etal(OmbreBlocs.bois()).formes(false, OmbreBlocs.ETAL_CONTOUR, null);
            decor("etal_" + m, b);
            AJOURES.add(b);
        }
        for (String c : CHEMINEES) decor("cheminee_" + c, new Decor.Cheminee(OmbreBlocs.pierre()).formes(OmbreBlocs.CHEMINEE_CONTOUR, null));
        for (String a : AFFICHES)
            decor("affiche_" + a, new Decor.Oriente(AbstractBlock.Settings.create().strength(0.2f).sounds(BlockSoundGroup.WOOL)
                    .nonOpaque().noCollision()).formes(true, Decor.b(1, 1, 0, 15, 15, 1), Decor.VIDE));
        for (String f : FANIONS) decor("fanion_" + f, new Decor.Simple(OmbreBlocs.bois()).formes(Decor.b(7, 0, 7, 9, 16, 9), null));

        for (String c : CONSTRUCTION) {
            Block b = new Block(AbstractBlock.Settings.copy(c.startsWith("papier") || c.startsWith("parquet") || c.startsWith("devanture")
                    || c.equals("boiserie") ? Blocks.OAK_PLANKS : Blocks.STONE));
            decor(c, b);
        }
        for (String c : AVEC_ESCALIERS) {
            Block base = Registries.BLOCK.get(id(c));
            decor(c + "_escalier", new StairsBlock(base.getDefaultState(), AbstractBlock.Settings.copy(base)));
            decor(c + "_dalle", new SlabBlock(AbstractBlock.Settings.copy(base)));
        }
    }

    private static AbstractBlock.Settings laine() {
        return AbstractBlock.Settings.create().strength(0.8f).sounds(BlockSoundGroup.WOOL).nonOpaque();
    }

    private static Identifier id(String n) { return Identifier.of(Ombre.MOD_ID, n); }

    private static void enregistrer(String nom, Block b) {
        Registry.register(Registries.BLOCK, id(nom), b);
        Registry.register(Registries.ITEM, id(nom), new BlockItem(b, new Item.Settings()));
    }

    private static void parkour(String nom, Block b) { enregistrer(nom, b); PARKOUR.add(b); }
    private static void decor(String nom, Block b) { enregistrer(nom, b); DECOR.add(b); }
}
