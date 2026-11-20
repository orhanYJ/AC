package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Assassin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Le garde qu'on peut assassiner reçoit un contour (le même que l'effet « surbrillance »). */
@Mixin(MinecraftClient.class)
public abstract class ContourMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void ombre$contour(Entity e, CallbackInfoReturnable<Boolean> cir) {
        if (Assassin.estCible(e)) cir.setReturnValue(true);
    }
}
