package fr.fenua.ombre.entite;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Les personnages réels de la Prague de 1600. Ils ne combattent pas et ne peuvent pas être blessés :
 * on ne fait que les croiser et leur parler (clic droit). Leurs répliques mêlent l'histoire vraie
 * et les indices de la mission ; les cibles, elles, sont des personnages inventés (la Confrérie du Corbeau).
 */
public class Figure extends PathAwareEntity {
    public static final int TYCHO = 0, KEPLER = 1, RODOLPHE = 2, LOEW = 3;
    public static final String[] NOMS = {"Tycho Brahe", "Johannes Kepler", "Rodolphe II", "Rabbi Judah Loew"};

    /** Les répliques, dans l'ordre (un clic droit = la suivante). */
    private static final String[][] PAROLES = {
        {   // Tycho Brahe (1546-1601), astronome de l'empereur depuis 1599
            "Tycho Brahe, mathématicien de l'empereur. Oui, ce nez est en métal : je l'ai perdu en duel, étudiant, pour une querelle de mathématiques.",
            "J'ai mesuré la place des étoiles et des planètes plus précisément que quiconque avant moi, et sans lunette : elle n'existe pas encore.",
            "Une confrérie, le Corbeau, veut voler mes registres d'observations pour les vendre. Son grand maître, Ottokar von Rabenstein, se cache au sommet de la tour du pont.",
            "Mon jeune assistant, Kepler, est près de l'horloge. Il croit que les planètes tournent autour du Soleil. Nous en débattons chaque soir.",
        },
        {   // Johannes Kepler (1571-1630), arrivé à Prague en 1600
            "Johannes Kepler. J'arrive de Graz pour travailler avec maître Tycho… s'il veut bien me montrer ses chiffres.",
            "L'orbite de Mars me résiste. Et si ce n'était pas un cercle ? Il me faudra des années pour le savoir.",
            "Le second de la Confrérie, l'alchimiste Melchior Kragl, a installé son laboratoire en haut d'un clocher de Týn.",
            "Depuis les gargouilles de Týn, on voit toute la place : un bon point de vue avant d'agir.",
        },
        {   // Rodolphe II (1552-1612), empereur, installé à Prague
            "Je suis Rodolphe, empereur. Ma cour accueille les savants, les peintres et les alchimistes de toute l'Europe.",
            "Mon cabinet de curiosités renferme des automates, des pierres étranges, une corne de licorne… du moins me l'a-t-on vendue ainsi.",
            "La Confrérie du Corbeau trompe ma cour avec de faux élixirs. Délivre-moi d'Ottokar von Rabenstein, sur la tour du pont, et de son alchimiste.",
        },
        {   // Judah Loew ben Bezalel, le « Maharal » (vers 1520-1609)
            "Judah Loew ben Bezalel. On m'appelle le Maharal. J'enseigne, j'écris, et l'empereur m'a fait l'honneur de me recevoir.",
            "Une légende raconte que j'ai façonné un homme d'argile pour protéger les nôtres. Les légendes ont parfois des bras solides.",
            "Le Golem ne te fera aucun mal. Les gardes, eux, feraient bien de l'éviter.",
        },
    };

    private static final TrackedData<Integer> QUI = DataTracker.registerData(Figure.class, TrackedDataHandlerRegistry.INTEGER);
    private final Map<UUID, Integer> suite = new HashMap<>();

    public Figure(EntityType<? extends PathAwareEntity> t, World w) {
        super(t, w);
        setInvulnerable(true);
        setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder attributs() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.4));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8));
        goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder b) {
        super.initDataTracker(b);
        b.add(QUI, 0);
    }

    public int qui() { return dataTracker.get(QUI); }

    public void devenir(int qui) {
        dataTracker.set(QUI, Math.floorMod(qui, NOMS.length));
        setCustomName(Text.literal(NOMS[qui()]));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) { super.writeCustomDataToNbt(nbt); nbt.putInt("Qui", qui()); }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) { super.readCustomDataFromNbt(nbt); devenir(nbt.getInt("Qui")); }

    @Override public boolean isInvulnerableTo(DamageSource source) { return !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL); }
    @Override public boolean canImmediatelyDespawn(double distance) { return false; }
    @Override public boolean isPushable() { return false; }

    /** Clic droit : la réplique suivante. */
    @Override
    protected ActionResult interactMob(PlayerEntity joueur, Hand main) {
        if (!getWorld().isClient() && main == Hand.MAIN_HAND) {
            String[] p = PAROLES[qui()];
            int i = suite.getOrDefault(joueur.getUuid(), 0);
            joueur.sendMessage(Text.literal("§6" + NOMS[qui()] + " §7: §f" + p[i % p.length]), false);
            suite.put(joueur.getUuid(), i + 1);
            if (joueur instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                if (qui() == TYCHO) fr.fenua.ombre.ac.Mission.marquer(sp, "ombre_m_tycho");
                if (qui() == KEPLER) fr.fenua.ombre.ac.Mission.marquer(sp, "ombre_m_kepler");
                fr.fenua.ombre.ac.Quetes.evenement(sp, "parle", this);
            }
            getLookControl().lookAt(joueur);
        }
        return ActionResult.success(getWorld().isClient());
    }
}
