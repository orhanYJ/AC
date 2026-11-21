package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Cinema;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Le saut de la foi : tout le corps bascule, tête la première, comme un plongeur. */
@Mixin(PlayerEntityRenderer.class)
public abstract class CorpsMixin {
    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V", at = @At("TAIL"))
    private void ombre$plongeon(AbstractClientPlayerEntity joueur, MatrixStack m, float age, float corps, float td, float echelle, CallbackInfo ci) {
        if (joueur != MinecraftClient.getInstance().player) return;
        float angle = Cinema.bascule(td);
        if (angle == 0) return;
        m.translate(0, 0.9, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        m.translate(0, -0.9, 0);
    }
}
