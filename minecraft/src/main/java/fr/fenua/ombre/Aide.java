package fr.fenua.ombre;

/** Le texte affiché par /ombre aide. */
public final class Aide {
    private Aide() {}

    public static final String TEXTE = String.join("\n",
            "§6§lL'Ombre — Prague 1600",
            "§6Touche H§r : l'écran avec toutes les commandes détaillées.",
            "§6§l/ombre rappel§r : le cours de rappel, tout ce qui a changé depuis l'initiation (19 leçons).",
            "§6§l/ombre initiation§r : commence par là ! La Ruelle d'Or t'apprend tout, pas à pas (30 leçons). §7/ombre initiation <n>§r : reprendre à l'étape n.",
            "§eEscalade§r : Espace contre un mur. Z/S monter-descendre, Q/D côté, Espace bondir, S+Espace saut arrière, Maj lâcher.",
            "§eClic droit maintenu§r : saisir les perches, enseignes, réverbères et câbles de tyrolienne.",
            "§eCourse libre§r : en sprintant, tu montes les blocs d'un mètre sans sauter, et tes sauts portent plus loin.",
            "§eRebords§r : en tombant le long d'un mur, on saisit le bord. Z/Espace se hisser, Q/D glisser, Q/D+Espace sauter, Maj lâcher.",
            "§eObjets§r : tyrolienne (corde entre deux poteaux), monte-charge, perches, enseignes, corniches, auvents… (onglet « L'Ombre »).",
            "§eCourse sur mur§r : en sprint, saute le long d'un mur (Z enfoncé). Espace = rebondir vers le mur d'en face.",
            "§eFoule, bancs, déguisement§r : marche parmi les passants, assieds-toi sur un banc, ou enfile une tenue de hallebardier.",
            "§eObjets§r : arbalète à grappin, thalers, pétards, fléchettes (soporifique, empoisonnée), couteaux, fumée.",
            "§eLame de nacre§r : frappe une créature qui ne t'a pas repéré = assassinat. Depuis une chute = assassinat aérien.",
            "   Une lame dans chaque main = double assassinat.",
            "§eSaut de la foi§r : tombe dans une botte de foin, aucun dégât. §eRoulade§r : accroupi à l'atterrissage, chute de 9 blocs sans mal.",
            "§eCachettes§r : accroupi dans les hautes herbes ou sous des feuilles, on ne te voit plus qu'à 2 blocs.",
            "   Dans le noir, accroupi ou dans le dos d'un garde, on te repère de beaucoup moins loin.",
            "§eBombe de fumée§r (clic droit) : les gardes autour perdent ta trace et sont aveuglés.",
            "§eInstinct§r (touche §lV§r) : révèle les gardes à travers les murs. §eSiffler§r (touche §lB§r) : attire le garde le plus proche.",
            "§ePoint de vue§r : tiens-toi sur une gargouille ou un perchoir pour synchroniser : gardes révélés, voyage rapide (touche §lJ§r).",
            "§eRoue des outils§r : maintiens §lG§r, montre un outil, relâche. §eCarte§r : touche §lJ§r.",
            "§eDétection§r : « ? » puis « ! » au-dessus des gardes, flèches autour du viseur. §cZones interdites§r autour des étendards rouges.",
            "§eNotoriété§r : monte à chaque alerte ; arrache les §lavis de recherche§r (clic droit) pour la faire baisser.",
            "§eArène§r : §l/ombre arene§r construit un fortin à 30 blocs devant toi (de préférence en monde plat).",
            "§eParcours§r : §l/ombre parcours§r construit une grande carte d'entraînement (200 blocs, sans gardes) et t'y emmène.",
            "§ePrague§r : §l/ombre prague§r construit la Vieille-Ville de 1600 et t'y emmène. Clic droit sur Tycho Brahe ou Kepler : la mission.",
            "§l/ombre kit§r : l'équipement. §l/ombre vitrine§r : tous les blocs.");
}
