package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Parcours;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quand le clic droit sert à attraper (parkour), l'objet en main n'est pas utilisé (pas de couteau lancé). */
@Mixin(MinecraftClient.class)
public abstract class ClicMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void ombre$prise(CallbackInfo ci) {
        if (Parcours.clicPourParkour((MinecraftClient) (Object) this)) ci.cancel();
    }
}
