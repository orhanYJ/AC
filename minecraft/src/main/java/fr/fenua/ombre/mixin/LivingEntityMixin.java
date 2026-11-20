package fr.fenua.ombre.mixin;

import fr.fenua.ombre.Discretion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Un « mixin » modifie une méthode de Minecraft lui-même.
 * Ici, deux méthodes de LivingEntity (toute créature vivante, joueur compris).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected boolean jumping;

    /**
     * Escalade : un joueur qui pousse contre un mur en tenant Espace grimpe
     * comme sur une échelle ; en tenant Maj, il reste accroché.
     */
    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void ombre$escalade(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PlayerEntity p)) return;
        if (p.isSpectator() || p.getAbilities().flying || p.isFallFlying() || p.isTouchingWater() || p.hasVehicle()) return;
        if (p.horizontalCollision && (this.jumping || p.isSneaking())) cir.setReturnValue(true);
    }

    /** Discrétion : réduit la distance à laquelle les créatures repèrent le joueur. */
    @Inject(method = "getAttackDistanceScalingFactor", at = @At("RETURN"), cancellable = true)
    private void ombre$discretion(@Nullable Entity observateur, CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof PlayerEntity p) {
            cir.setReturnValue(cir.getReturnValue() * Discretion.facteur(p, observateur));
        }
    }
}
