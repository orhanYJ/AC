package fr.fenua.ombre.objet;

import fr.fenua.ombre.Ombre;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Les objets de Prague, en 1600 : à manger, à lancer, à tirer. */
public final class Objets {
    private Objets() {}

    /** Un objet qu'on lance d'un clic droit (thalers, pétard, fléchettes). */
    public static class ALancer extends Item {
        private final float vitesse;
        private final int recharge;

        public ALancer(Settings s, float vitesse, int recharge) { super(s); this.vitesse = vitesse; this.recharge = recharge; }

        @Override
        public TypedActionResult<ItemStack> use(World w, PlayerEntity joueur, Hand main) {
            ItemStack pile = joueur.getStackInHand(main);
            w.playSound(null, joueur.getX(), joueur.getY(), joueur.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS,
                    0.5f, 0.6f + w.random.nextFloat() * 0.3f);
            if (!w.isClient()) {
                Projectile p = new Projectile(PROJECTILE, joueur, w);
                p.setItem(pile.copyWithCount(1));
                p.setVelocity(joueur, joueur.getPitch(), joueur.getYaw(), 0f, vitesse, 0.6f);
                w.spawnEntity(p);
            }
            joueur.getItemCooldownManager().set(this, recharge);
            pile.decrementUnlessCreative(1, joueur);
            return TypedActionResult.success(pile, w.isClient());
        }
    }

    private static Item.Settings manger(int faim, float sat) {
        return new Item.Settings().food(new FoodComponent.Builder().nutrition(faim).saturationModifier(sat).build());
    }

    public static final Item PAIN_SEIGLE = new Item(manger(6, 0.6f));
    public static final Item PAIN_EPICES = new Item(manger(4, 0.5f));
    public static final Item FROMAGE = new Item(manger(5, 0.7f));
    public static final Item VIN = new Item(new Item.Settings().maxCount(16).food(new FoodComponent.Builder().nutrition(2).saturationModifier(0.2f)
            .alwaysEdible().statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0), 0.6f).build()));
    /** Une chope de bière de Bohême : de l'élan (vitesse), et un peu de courage (force). */
    public static final Item BIERE = new Item(new Item.Settings().maxCount(16).food(new FoodComponent.Builder().nutrition(2).saturationModifier(0.2f)
            .alwaysEdible().statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 0), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 0), 1f).build()));

    /** Thalers, la monnaie d'argent de Bohême (le mot a donné « dollar ») : jetés au sol, la foule accourt. */
    public static final Item THALERS = new ALancer(new Item.Settings().maxCount(64), 0.9f, 10);
    public static final Item PETARD = new ALancer(new Item.Settings().maxCount(16), 1.0f, 20);
    public static final Item FLECHETTE_SOPORIFIQUE = new ALancer(new Item.Settings().maxCount(16), 2.6f, 15);
    public static final Item FLECHETTE_EMPOISONNEE = new ALancer(new Item.Settings().maxCount(16), 2.6f, 15);
    /** Fléchette de folie : le garde touché attaque ses camarades pendant 15 s. */
    public static final Item FLECHETTE_FOLIE = new ALancer(new Item.Settings().maxCount(16), 2.6f, 15);

    /**
     * Tenue de hallebardier : clic droit pour l'enfiler. Déguisé, les gardes ne te remarquent qu'à 2 blocs,
     * tant que tu ne cours pas, ne grimpes pas et ne frappes personne (voir Discretion et Gardes).
     */
    public static class Tenue extends Item {
        public Tenue(Settings s) { super(s); }
        @Override
        public TypedActionResult<ItemStack> use(World w, PlayerEntity joueur, Hand main) {
            ItemStack pile = joueur.getStackInHand(main);
            if (!w.isClient()) {
                joueur.addStatusEffect(new StatusEffectInstance(Effets.DEGUISE, 20 * 120, 0, false, false, true));
                w.playSound(null, joueur.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 1f, 1f);
                joueur.sendMessage(net.minecraft.text.Text.literal("§6Déguisé en hallebardier§r — ne cours pas, ne grimpe pas, ne frappe pas."), true);
                pile.decrementUnlessCreative(1, joueur);
            }
            return TypedActionResult.success(pile, w.isClient());
        }
    }

    public static final Item TENUE = new Tenue(new Item.Settings().maxCount(4));

    /**
     * Arbalète à grappin : clic droit vers le bord d'un toit (40 blocs au plus). Le crochet s'y accroche,
     * on est tiré jusqu'en haut et on se rétablit. Le mouvement est calculé chez le joueur (voir Parcours).
     */
    public static class Grappin extends Item {
        public Grappin(Settings s) { super(s); }
        @Override
        public TypedActionResult<ItemStack> use(World w, PlayerEntity joueur, Hand main) {
            if (w.isClient() && fr.fenua.ombre.bloc.CoteClient.ICI[0] != null) fr.fenua.ombre.bloc.CoteClient.ICI[0].grappin();
            if (!w.isClient()) w.playSound(null, joueur.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.3f);
            joueur.getItemCooldownManager().set(this, 30);
            return TypedActionResult.success(joueur.getStackInHand(main), w.isClient());
        }
    }

    public static final Item GRAPPIN = new Grappin(new Item.Settings().maxCount(1));

    public static EntityType<Projectile> PROJECTILE;

    /** Dans l'ordre de l'onglet « Objets ». */
    public static final List<Item> TOUS = new ArrayList<>();

    public static void enregistrer() {
        Effets.enregistrer();
        objet("pain_seigle", PAIN_SEIGLE);
        objet("pain_epices", PAIN_EPICES);
        objet("fromage", FROMAGE);
        objet("vin", VIN);
        objet("biere", BIERE);
        objet("thalers", THALERS);
        objet("petard", PETARD);
        objet("flechette_soporifique", FLECHETTE_SOPORIFIQUE);
        objet("flechette_empoisonnee", FLECHETTE_EMPOISONNEE);
        objet("flechette_folie", FLECHETTE_FOLIE);
        objet("tenue_hallebardier", TENUE);
        objet("lance_grappin", GRAPPIN);
        PROJECTILE = Registry.register(Registries.ENTITY_TYPE, Identifier.of(Ombre.MOD_ID, "projectile"),
                EntityType.Builder.<Projectile>create(Projectile::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f)
                        .maxTrackingRange(4).trackingTickInterval(10).build("projectile"));
    }

    private static void objet(String nom, Item i) {
        Registry.register(Registries.ITEM, Identifier.of(Ombre.MOD_ID, nom), i);
        TOUS.add(i);
    }
}
