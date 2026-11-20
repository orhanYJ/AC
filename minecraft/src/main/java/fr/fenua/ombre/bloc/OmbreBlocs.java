package fr.fenua.ombre.bloc;

import fr.fenua.ombre.Ombre;
import fr.fenua.ombre.OmbreItems;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Les blocs de parkour, et l'onglet « L'Ombre » du mode créatif.
 *  - Poteau de tyrolienne : on relie deux poteaux avec une corde, puis clic droit pour glisser.
 *  - Monte-charge : posé au pied d'un mur, clic droit, et on est hissé jusqu'au toit.
 *  - Perche : une barre horizontale ; on la saisit au vol, on se balance, on est projeté.
 *  - Perchoir : un point de vue ; s'y tenir révèle les gardes du coin.
 */
public final class OmbreBlocs {
    private OmbreBlocs() {}

    public static final PoteauTyrolienne POTEAU = new PoteauTyrolienne(AbstractBlock.Settings.create()
            .strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque());
    public static final MonteCharge MONTE_CHARGE = new MonteCharge(AbstractBlock.Settings.create()
            .strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque());
    public static final Perche PERCHE = new Perche(AbstractBlock.Settings.create()
            .strength(1.0f).sounds(BlockSoundGroup.WOOD).nonOpaque().noCollision());
    public static final Perchoir PERCHOIR = new Perchoir(AbstractBlock.Settings.create()
            .strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque().luminance(s -> 4));

    // Le mobilier de parkour
    public static final Mobilier.Poutre POUTRE = new Mobilier.Poutre(AbstractBlock.Settings.create()
            .strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque());
    public static final Mobilier.Corniche CORNICHE = new Mobilier.Corniche(AbstractBlock.Settings.create()
            .strength(2f).sounds(BlockSoundGroup.STONE).nonOpaque());
    public static final Mobilier.Enseigne ENSEIGNE = new Mobilier.Enseigne(AbstractBlock.Settings.create()
            .strength(1f).sounds(BlockSoundGroup.WOOD).nonOpaque().noCollision());
    public static final Mobilier.Auvent AUVENT = new Mobilier.Auvent(AbstractBlock.Settings.create()
            .strength(0.8f).sounds(BlockSoundGroup.WOOL).nonOpaque());
    public static final Mobilier.Echafaudage ECHAFAUDAGE = new Mobilier.Echafaudage(AbstractBlock.Settings.create()
            .strength(1f).sounds(BlockSoundGroup.SCAFFOLDING).nonOpaque());
    public static final Mobilier.Lampadaire LAMPADAIRE = new Mobilier.Lampadaire(AbstractBlock.Settings.create()
            .strength(2f).sounds(BlockSoundGroup.METAL).nonOpaque());
    public static final Mobilier.TeteLampadaire TETE_LAMPADAIRE = new Mobilier.TeteLampadaire(AbstractBlock.Settings.create()
            .strength(2f).sounds(BlockSoundGroup.LANTERN).nonOpaque().luminance(s -> 15));

    public static final Meule MEULE = new Meule(AbstractBlock.Settings.create()
            .strength(0.5f).sounds(BlockSoundGroup.GRASS).nonOpaque().noCollision());

    public static final Cachettes.Puits PUITS = new Cachettes.Puits(AbstractBlock.Settings.create()
            .strength(2f).sounds(BlockSoundGroup.STONE).nonOpaque());
    public static final Cachettes.Cabine CABINE = new Cachettes.Cabine(AbstractBlock.Settings.create()
            .strength(1f).sounds(BlockSoundGroup.WOOD).nonOpaque());


    // ───────────── Prague, 1600 ─────────────
    static AbstractBlock.Settings bois() { return AbstractBlock.Settings.create().strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque(); }
    static AbstractBlock.Settings pierre() { return AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.STONE).nonOpaque(); }
    static AbstractBlock.Settings metal() { return AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.METAL).nonOpaque(); }

    // Formes partagées avec les variantes (voir Variantes)
    public static final net.minecraft.util.shape.VoxelShape BANC_CONTOUR = Decor.u(Decor.b(0, 0, 2, 16, 8, 13), Decor.b(0, 8, 2, 16, 16, 4));
    public static final net.minecraft.util.shape.VoxelShape BALCON_CONTOUR = Decor.u(Decor.b(0, 0, 0, 16, 2, 16), Decor.b(0, 2, 15, 16, 14, 16));
    public static final net.minecraft.util.shape.VoxelShape REVERBERE_CONTOUR = Decor.u(Decor.b(7, 13, 0, 9, 15, 12), Decor.b(5, 4, 9, 11, 12, 15));
    public static final net.minecraft.util.shape.VoxelShape VOLETS_CONTOUR = Decor.u(Decor.b(0, 0, 0, 4, 16, 1), Decor.b(12, 0, 0, 16, 16, 1));
    public static final net.minecraft.util.shape.VoxelShape ETAL_CONTOUR = Decor.u(Decor.b(0, 0, 0, 16, 9, 5), Decor.b(0, 15, 0, 16, 16, 16),
            Decor.b(0, 0, 15, 1, 16, 16), Decor.b(15, 0, 15, 16, 16, 16));
    public static final net.minecraft.util.shape.VoxelShape CHEMINEE_CONTOUR = Decor.u(Decor.b(3, 0, 3, 13, 16, 13), Decor.b(2, 14, 2, 14, 16, 14));

    /** Gouttière : un tuyau de zinc le long d'un mur. On y grimpe deux fois plus vite. */
    public static final Decor.Gouttiere GOUTTIERE = (Decor.Gouttiere) new Decor.Gouttiere(metal()).formes(true,
            Decor.b(5, 0, 0, 11, 16, 5), null);
    /** Balcon en fer forgé : un plancher et une grille, devant une façade. */
    public static final Decor.Balcon BALCON = (Decor.Balcon) new Decor.Balcon(metal()).formes(true,
            BALCON_CONTOUR, null);
    /** Cheminée de toit, avec ses pots et sa fumée. */
    public static final Decor.Cheminee CHEMINEE = (Decor.Cheminee) new Decor.Cheminee(pierre()).formes(
            CHEMINEE_CONTOUR, null);
    /** Lucarne : une fenêtre de mansarde, sur laquelle on peut se hisser. */
    public static final Decor.Oriente LUCARNE = new Decor.Oriente(pierre()).formes(false,
            Decor.b(1, 0, 3, 15, 16, 16), Decor.b(2, 0, 4, 14, 14, 16));
    /** Réverbère à potence, accroché à un mur : la potence se saisit au clic droit, comme une perche. */
    public static final Decor.Reverbere REVERBERE = (Decor.Reverbere) new Decor.Reverbere(metal().noCollision().luminance(s -> 15)).formes(true,
            REVERBERE_CONTOUR, Decor.VIDE);
    /** Banc public : clic droit pour s'y asseoir, caché aux yeux des gardes. */
    public static final Decor.Banc BANC = (Decor.Banc) new Decor.Banc(bois()).formes(true,
            BANC_CONTOUR, null);
    /** Charrette de foin : saut de la foi, cachette (accroupi). */
    public static final Decor.Charrette CHARRETTE = (Decor.Charrette) new Decor.Charrette(
            AbstractBlock.Settings.create().strength(0.8f).sounds(BlockSoundGroup.GRASS).nonOpaque().noCollision())
            .formes(false, Decor.b(0, 0, 0, 16, 14, 16), Decor.VIDE);
    /** Étal de marché : on peut tomber sur la toile sans se faire mal. */
    public static final Decor.Etal ETAL = (Decor.Etal) new Decor.Etal(bois()).formes(false,
            ETAL_CONTOUR, null);
    /** Gargouille : un point de vue de pierre. S'y tenir révèle les gardes. */
    public static final Decor.Oriente GARGOUILLE = new Decor.Oriente(pierre()).formes(true,
            Decor.u(Decor.b(0, 8, 0, 16, 16, 10), Decor.b(4, 8, 8, 12, 16, 16)), null);
    /** Corde à linge, avec son linge qui sèche. */
    public static final Decor.Oriente CORDE_LINGE = new Decor.Oriente(
            AbstractBlock.Settings.create().strength(0.3f).sounds(BlockSoundGroup.WOOL).nonOpaque().noCollision()).formes(false,
            Decor.b(0, 5, 7, 16, 15, 9), Decor.VIDE);
    /** Barricade de pavés, de roues et de planches : on s'y abrite, on la franchit. */
    public static final Decor.Oriente BARRICADE = new Decor.Oriente(pierre()).formes(false,
            Decor.b(0, 0, 2, 16, 12, 14), Decor.b(0, 0, 2, 16, 10, 14));
    /** Colonne d'affiches (colonne Morris), deux blocs de haut. */
    public static final Decor.Simple COLONNE = new Decor.Simple(pierre()).formes(Decor.b(2, 0, 2, 14, 16, 14), Decor.b(2, 0, 2, 14, 24, 14));
    /** Horloge murale. */
    public static final Decor.Oriente HORLOGE = new Decor.Oriente(metal().noCollision()).formes(true, Decor.b(0, 0, 0, 16, 16, 2), Decor.VIDE);
    /** Affiche collée au mur. */
    public static final Decor.Oriente AFFICHE = new Decor.Oriente(
            AbstractBlock.Settings.create().strength(0.2f).sounds(BlockSoundGroup.WOOL).nonOpaque().noCollision()).formes(true,
            Decor.b(1, 1, 0, 15, 15, 1), Decor.VIDE);
    /** Charpente : un treillis de poutres (clochers, chantiers), ajouré ; il s'escalade. */
    public static final Decor.Simple CHARPENTE = new Decor.Simple(bois()).formes(Decor.b(0, 0, 0, 16, 16, 16), null);
    /** Volets, de part et d'autre d'une fenêtre. */
    public static final Decor.Oriente VOLETS = new Decor.Oriente(bois().noCollision()).formes(true,
            VOLETS_CONTOUR, Decor.VIDE);
    /** Fontaine de pierre. */
    public static final Decor.Simple FONTAINE = new Decor.Simple(pierre()).formes(Decor.b(0, 0, 0, 16, 14, 16),
            Decor.u(Decor.b(0, 0, 0, 16, 6, 2), Decor.b(0, 0, 14, 16, 6, 16), Decor.b(0, 0, 0, 2, 6, 16), Decor.b(14, 0, 0, 16, 6, 16), Decor.b(6, 0, 6, 10, 14, 10)));
    /** Drapeau tricolore sur sa hampe. */
    public static final Decor.Simple DRAPEAU = new Decor.Simple(bois()).formes(Decor.b(7, 0, 7, 9, 16, 9), null);
    /** Carrosse : on monte sur son toit. */
    public static final Decor.Oriente CARROSSE = new Decor.Oriente(bois()).formes(false, Decor.b(0, 0, 1, 16, 15, 15), null);

    public static final Item CORDE = new Item(new Item.Settings().maxCount(16));

    /** Statue de pierre sur son socle (le pont), deux blocs de haut. */
    public static final Decor.Simple STATUE = new Decor.Simple(pierre()).formes(Decor.b(3, 0, 3, 13, 16, 13), Decor.b(3, 0, 3, 13, 24, 13));
    /** Sphère armillaire : l'instrument des astronomes (Tycho Brahe en avait de grandes). */
    public static final Decor.Simple SPHERE = new Decor.Simple(metal()).formes(Decor.b(2, 0, 2, 14, 16, 14), null);
    /** Alambic d'alchimiste. */
    public static final Decor.Simple ALAMBIC = new Decor.Simple(metal()).formes(Decor.b(2, 0, 2, 14, 15, 14), null);
    /** Étagère de fioles et de flacons. */
    public static final Decor.Oriente ETAGERE = new Decor.Oriente(bois()).formes(true, Decor.b(0, 0, 0, 16, 16, 6), null);
    /** Tonneau de bière couché, avec son robinet. */
    public static final Decor.Oriente TONNEAU = new Decor.Oriente(bois()).formes(false, Decor.b(1, 0, 1, 15, 14, 15), null);
    /** Pupitre d'astronome, avec un grand livre ouvert. */
    public static final Decor.Oriente PUPITRE = new Decor.Oriente(bois()).formes(false, Decor.b(2, 0, 2, 14, 15, 14), null);

    /** Tous les blocs de la Prague de 1600, dans l'ordre de l'onglet. */
    public static final Block[] PARIS = {GOUTTIERE, BALCON, CHEMINEE, LUCARNE, REVERBERE, BANC, CHARRETTE, ETAL, GARGOUILLE,
            CORDE_LINGE, BARRICADE, COLONNE, HORLOGE, AFFICHE, CHARPENTE, VOLETS, FONTAINE, DRAPEAU, CARROSSE,
            STATUE, SPHERE, ALAMBIC, ETAGERE, TONNEAU, PUPITRE};

    public static BlockEntityType<PoteauTyrolienne.Entite> POTEAU_ENTITE;
    public static BlockEntityType<MonteCharge.Entite> MONTE_CHARGE_ENTITE;

    public static void enregistrer() {
        bloc("poteau_tyrolienne", POTEAU);
        bloc("monte_charge", MONTE_CHARGE);
        bloc("perche", PERCHE);
        bloc("perchoir", PERCHOIR);
        bloc("poutre", POUTRE);
        bloc("corniche", CORNICHE);
        bloc("enseigne", ENSEIGNE);
        bloc("auvent", AUVENT);
        bloc("echafaudage", ECHAFAUDAGE);
        bloc("lampadaire", LAMPADAIRE);
        bloc("tete_lampadaire", TETE_LAMPADAIRE);
        bloc("meule", MEULE);
        bloc("puits", PUITS);
        bloc("cabine", CABINE);
        bloc("gouttiere", GOUTTIERE);
        bloc("balcon", BALCON);
        bloc("cheminee", CHEMINEE);
        bloc("lucarne", LUCARNE);
        bloc("reverbere", REVERBERE);
        bloc("banc", BANC);
        bloc("charrette", CHARRETTE);
        bloc("etal", ETAL);
        bloc("gargouille", GARGOUILLE);
        bloc("corde_linge", CORDE_LINGE);
        bloc("barricade", BARRICADE);
        bloc("colonne", COLONNE);
        bloc("horloge", HORLOGE);
        bloc("affiche", AFFICHE);
        bloc("charpente", CHARPENTE);
        bloc("volets", VOLETS);
        bloc("fontaine", FONTAINE);
        bloc("drapeau", DRAPEAU);
        bloc("carrosse", CARROSSE);
        bloc("statue", STATUE);
        bloc("sphere_armillaire", SPHERE);
        bloc("alambic", ALAMBIC);
        bloc("etagere_fioles", ETAGERE);
        bloc("tonneau_biere", TONNEAU);
        bloc("pupitre", PUPITRE);

        Registry.register(Registries.ITEM, id("corde_tyrolienne"), CORDE);

        POTEAU_ENTITE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("poteau_tyrolienne"),
                BlockEntityType.Builder.create(PoteauTyrolienne.Entite::new, POTEAU).build(null));
        MONTE_CHARGE_ENTITE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("monte_charge"),
                BlockEntityType.Builder.create(MonteCharge.Entite::new, MONTE_CHARGE).build(null));

        Variantes.enregistrer();

        // Trois onglets dans le mode créatif
        Registry.register(Registries.ITEM_GROUP, id("onglet"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(OmbreItems.LAME))
                .displayName(Text.translatable("itemGroup.ombre"))
                .entries((ctx, e) -> {
                    e.add(OmbreItems.LAME);
                    e.add(OmbreItems.BOMBE_FUMEE);
                    e.add(OmbreItems.COUTEAUX);
                    for (Block b : new Block[]{POTEAU, MONTE_CHARGE, PERCHE, PERCHOIR, MEULE, PUITS, CABINE, POUTRE, CORNICHE,
                            ENSEIGNE, AUVENT, ECHAFAUDAGE, LAMPADAIRE, TETE_LAMPADAIRE, GOUTTIERE, BALCON, REVERBERE, BANC,
                            CHARRETTE, GARGOUILLE, CHARPENTE}) e.add(b);
                    e.add(CORDE);
                    for (Block b : Variantes.PARKOUR) e.add(b);
                })
                .build());
        Registry.register(Registries.ITEM_GROUP, id("onglet_prague"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(LUCARNE))
                .displayName(Text.translatable("itemGroup.ombre.prague"))
                .entries((ctx, e) -> {
                    for (Block b : PARIS) e.add(b);
                    for (Block b : Variantes.DECOR) e.add(b);
                })
                .build());
        Registry.register(Registries.ITEM_GROUP, id("onglet_objets"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(fr.fenua.ombre.objet.Objets.PAIN_SEIGLE))
                .displayName(Text.translatable("itemGroup.ombre.objets"))
                .entries((ctx, e) -> {
                    for (Item i : fr.fenua.ombre.objet.Objets.TOUS) e.add(i);
                    for (Item i : fr.fenua.ombre.entite.Entites.OEUFS) e.add(i);
                })
                .build());
    }

    private static Identifier id(String nom) { return Identifier.of(Ombre.MOD_ID, nom); }

    private static void bloc(String nom, Block b) {
        Registry.register(Registries.BLOCK, id(nom), b);
        Registry.register(Registries.ITEM, id(nom), new BlockItem(b, new Item.Settings()));
    }
}
