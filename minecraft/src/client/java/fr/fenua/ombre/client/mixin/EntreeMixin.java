package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Cinema;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pendant la vue de l'aigle, les touches ne font plus bouger le corps (il attend en bas). */
@Mixin(KeyboardInput.class)
public abstract class EntreeMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ombre$fige(boolean lent, float facteur, CallbackInfo ci) {
        if (!Cinema.corpsFige()) return;
        KeyboardInput in = (KeyboardInput) (Object) this;
        in.movementForward = 0; in.movementSideways = 0;
        in.pressingForward = in.pressingBack = in.pressingLeft = in.pressingRight = false;
        in.jumping = false; in.sneaking = false;
    }
}
