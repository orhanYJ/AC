package fr.fenua.ombre.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Les poses du parkour : on plie bras et jambes du modèle du joueur selon ce qu'il fait.
 * (Visible en vue extérieure : touche F5.)
 * Les angles sont en radians ; un bras « pitch = -3 » est levé tout droit au-dessus de la tête.
 */
public final class Animations {
    private Animations() {}

    public static void appliquer(BipedEntityModel<?> m, float age) {
        Parcours.Pose pose = Parcours.pose();
        if (pose == Parcours.Pose.AUCUNE) return;
        float c = Parcours.phase;
        float s = MathHelper.sin(c);
        zero(m.rightArm); zero(m.leftArm); zero(m.rightLeg); zero(m.leftLeg);
        switch (pose) {
            case MUR -> {                       // mains qui cherchent les prises, jambes qui poussent
                m.rightArm.pitch = -2.6f + 0.5f * s;
                m.leftArm.pitch = -2.6f - 0.5f * s;
                m.rightArm.roll = 0.2f;
                m.leftArm.roll = -0.2f;
                m.rightLeg.pitch = -0.5f - 0.5f * s;
                m.leftLeg.pitch = -0.5f + 0.5f * s;
            }
            case REBORD -> {                    // pendu par les mains, jambes qui se balancent
                m.rightArm.pitch = -3.0f + 0.2f * s;
                m.leftArm.pitch = -3.0f - 0.2f * s;
                m.rightArm.roll = 0.3f;
                m.leftArm.roll = -0.3f;
                float b = MathHelper.sin(age * 0.12f) * 0.15f;
                m.rightLeg.pitch = b + 0.1f;
                m.leftLeg.pitch = b - 0.1f;
            }
            case HISSE -> {                     // on pousse sur les mains, un genou remonte
                m.rightArm.pitch = -1.9f;
                m.leftArm.pitch = -1.9f;
                m.rightLeg.pitch = -1.4f;
                m.leftLeg.pitch = -0.2f;
            }
            case MUR_COURU -> {                 // course, le bras côté mur tendu vers lui
                float r = MathHelper.sin(c * 1.6f);
                m.rightLeg.pitch = 1.1f * r;
                m.leftLeg.pitch = -1.1f * r;
                if (Parcours.coteMur > 0) { m.rightArm.roll = 1.3f; m.leftArm.pitch = -0.9f * r; }
                else { m.leftArm.roll = -1.3f; m.rightArm.pitch = 0.9f * r; }
            }
            case TYRO -> {                      // deux mains sur la poulie, jambes ramenées
                m.rightArm.pitch = -2.95f;
                m.leftArm.pitch = -2.95f;
                m.rightArm.roll = 0.1f;
                m.leftArm.roll = -0.1f;
                m.rightLeg.pitch = -0.7f;
                m.leftLeg.pitch = -0.5f;
            }
            case LIFT -> {                      // une main sur la corde
                m.rightArm.pitch = -3.0f;
                m.leftArm.pitch = -0.4f;
                m.leftArm.roll = -0.2f;
                m.rightLeg.pitch = -0.1f;
            }
            case PERCHE -> {                    // mains sur la barre, jambes portées par le balancement
                float t = (float) Parcours.balancement();
                m.rightArm.pitch = -3.0f;
                m.leftArm.pitch = -3.0f;
                m.rightLeg.pitch = -t * 0.9f - 0.1f;
                m.leftLeg.pitch = -t * 0.9f + 0.1f;
            }
            case BOND -> {                      // bras en arrière, jambes en ciseaux
                m.rightArm.pitch = 0.9f;
                m.leftArm.pitch = 0.9f;
                m.rightArm.roll = 0.3f;
                m.leftArm.roll = -0.3f;
                m.rightLeg.pitch = -1.0f;
                m.leftLeg.pitch = 0.6f;
            }
            case FRANCHIR -> {                  // une main sur l'obstacle, les jambes passent de côté
                m.rightArm.pitch = -1.2f;
                m.leftArm.pitch = -0.6f;
                m.leftArm.roll = -0.5f;
                m.rightLeg.pitch = -1.3f;
                m.leftLeg.pitch = -1.0f;
                m.rightLeg.roll = 0.4f;
                m.leftLeg.roll = 0.4f;
            }
            case PLONGEON -> {                  // le saut de la foi, bras en croix
                m.rightArm.roll = 1.45f;
                m.leftArm.roll = -1.45f;
                m.rightArm.pitch = -0.2f;
                m.leftArm.pitch = -0.2f;
                m.rightLeg.pitch = 0.15f;
                m.leftLeg.pitch = 0.15f;
            }
            case GRAPPIN -> {                   // tiré par la corde, le lanceur tendu au-dessus de la tête
                m.rightArm.pitch = -2.9f;
                m.leftArm.pitch = -2.4f;
                m.rightLeg.pitch = -0.4f;
                m.leftLeg.pitch = 0.2f;
            }
            case EXEC_SOL, EXEC_AERIEN, EXEC_CACHETTE, EXEC_REBORD -> ExecClient.poseJoueur(m);
            default -> {}
        }
    }

    private static void zero(ModelPart p) {
        p.pitch = 0; p.yaw = 0; p.roll = 0;
    }
}
