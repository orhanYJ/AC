package fr.fenua.ombre;

/** Le texte affiché par /ombre aide. */
public final class Aide {
    private Aide() {}

    public static final String TEXTE = String.join("\n",
            "§6§lL'Ombre — Infiltration",
            "§eEscalade§r : pousse contre un mur et garde §lEspace§r enfoncé. §lMaj§r contre le mur = rester accroché.",
            "§eCourse libre§r : en sprintant, tu montes les blocs d'un mètre sans sauter, et tes sauts portent plus loin.",
            "§eRebords§r : en tombant le long d'un mur, tu t'accroches au bord. Espace = se hisser, Q/D = se déplacer, Maj = lâcher.",
            "§eCourse sur mur§r : en sprint, saute le long d'un mur (Z enfoncé). Espace = rebondir vers le mur d'en face.",
            "§eSaut arrière§r : contre un mur ou suspendu, S + Espace.",
            "§eLame de nacre§r : frappe une créature qui ne t'a pas repéré = assassinat. Depuis une chute = assassinat aérien.",
            "   Une lame dans chaque main = double assassinat.",
            "§eSaut de la foi§r : tombe dans une botte de foin, aucun dégât. §eRoulade§r : accroupi à l'atterrissage, chute de 9 blocs sans mal.",
            "§eCachettes§r : accroupi dans les hautes herbes ou sous des feuilles, on ne te voit plus qu'à 2 blocs.",
            "   Dans le noir, accroupi ou dans le dos d'un garde, on te repère de beaucoup moins loin.",
            "§eBombe de fumée§r (clic droit) : les gardes autour perdent ta trace et sont aveuglés.",
            "§eInstinct§r (touche §lV§r) : révèle les gardes à travers les murs. §eSiffler§r (touche §lB§r) : attire le garde le plus proche.",
            "§ePoint de vue§r : monte tout en haut de la grande tour pour synchroniser et révéler le fortin.",
            "§eArène§r : §l/ombre arene§r construit un fortin à 30 blocs devant toi (de préférence en monde plat).");
}
