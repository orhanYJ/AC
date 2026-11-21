package fr.fenua.ombre.client.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Un corps à terre (gardé une minute pour être fouillé) ne reste pas teinté de rouge : seule la seconde de la mort l'est. */
@Mixin(LivingEntityRenderer.class)
public abstract class CorpsRougeMixin {
    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void ombre$corps(LivingEntity e, float blanc, CallbackInfoReturnable<Integer> cir) {
        if (e.deathTime > 20 && e.hurtTime <= 0) cir.setReturnValue(OverlayTexture.DEFAULT_UV);
    }
}
