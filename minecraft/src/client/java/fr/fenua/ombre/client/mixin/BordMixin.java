package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Parcours;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** En course libre au bord d'un grand vide, on s'arrête net (comme accroupi) au lieu de tomber. */
@Mixin(PlayerEntity.class)
public abstract class BordMixin {
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void ombre$bord(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == MinecraftClient.getInstance().player && Parcours.bordProtege()) cir.setReturnValue(true);
    }
}
