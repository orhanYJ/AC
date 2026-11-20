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

    public static final Item CORDE = new Item(new Item.Settings().maxCount(16));

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
        Registry.register(Registries.ITEM, id("corde_tyrolienne"), CORDE);

        POTEAU_ENTITE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("poteau_tyrolienne"),
                BlockEntityType.Builder.create(PoteauTyrolienne.Entite::new, POTEAU).build(null));
        MONTE_CHARGE_ENTITE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("monte_charge"),
                BlockEntityType.Builder.create(MonteCharge.Entite::new, MONTE_CHARGE).build(null));

        Registry.register(Registries.ITEM_GROUP, id("onglet"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(OmbreItems.LAME))
                .displayName(Text.translatable("itemGroup.ombre"))
                .entries((ctx, e) -> {
                    e.add(OmbreItems.LAME);
                    e.add(OmbreItems.BOMBE_FUMEE);
                    e.add(POTEAU);
                    e.add(CORDE);
                    e.add(MONTE_CHARGE);
                    e.add(PERCHE);
                    e.add(PERCHOIR);
                    e.add(POUTRE);
                    e.add(CORNICHE);
                    e.add(ENSEIGNE);
                    e.add(AUVENT);
                    e.add(ECHAFAUDAGE);
                    e.add(LAMPADAIRE);
                    e.add(TETE_LAMPADAIRE);
                })
                .build());
    }

    private static Identifier id(String nom) { return Identifier.of(Ombre.MOD_ID, nom); }

    private static void bloc(String nom, Block b) {
        Registry.register(Registries.BLOCK, id(nom), b);
        Registry.register(Registries.ITEM, id(nom), new BlockItem(b, new Item.Settings()));
    }
}
