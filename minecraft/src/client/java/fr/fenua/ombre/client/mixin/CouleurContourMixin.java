package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Assassin;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** … et ce contour est rouge, quelle que soit son équipe. */
@Mixin(Entity.class)
public abstract class CouleurContourMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void ombre$rouge(CallbackInfoReturnable<Integer> cir) {
        if (Assassin.estCible((Entity) (Object) this)) cir.setReturnValue(Assassin.ROUGE);
    }
}
