package fr.fenua.ombre.mixin;

import fr.fenua.ombre.Discretion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Un « mixin » modifie une méthode de Minecraft lui-même.
 * Ici, une méthode de LivingEntity (toute créature vivante, joueur compris).
 * (L'escalade, elle, est gérée par le client : voir client/Parcours.)
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @org.spongepowered.asm.mixin.Shadow public int deathTime;

    /**
     * Le corps d'un garde reste à terre une minute (on peut le fouiller, comme dans Assassin's Creed),
     * au lieu de disparaître en fumée au bout d'une seconde.
     */
    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    private void ombre$corps(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LivingEntity moi = (LivingEntity) (Object) this;
        if (!Discretion.estGardeClient(moi) && !Discretion.estGarde(moi)) return;
        if (deathTime >= 19 && deathTime < fr.fenua.ombre.ac.Profil.CORPS) {
            deathTime++;
            // Fouillé : il s'efface bientôt
            if (moi.getCommandTags().contains(fr.fenua.ombre.ac.Profil.FOUILLE) && deathTime < fr.fenua.ombre.ac.Profil.CORPS - 200)
                deathTime = fr.fenua.ombre.ac.Profil.CORPS - 200;
            ci.cancel();
        }
    }

    /** Discrétion : réduit la distance à laquelle les créatures repèrent le joueur. */
    @Inject(method = "getAttackDistanceScalingFactor", at = @At("RETURN"), cancellable = true)
    private void ombre$discretion(@Nullable Entity observateur, CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof PlayerEntity p) {
            cir.setReturnValue(cir.getReturnValue() * Discretion.facteur(p, observateur));
        }
    }
}
