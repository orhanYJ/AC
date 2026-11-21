package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Cinema;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pendant un plan de cinéma (synchronisation, saut de la foi, assassinat), c'est Cinema qui place la caméra. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void ombre$cinema(BlockView zone, Entity suivi, boolean troisieme, boolean inverse, float td, CallbackInfo ci) {
        double[] c = Cinema.camera(td);
        if (c == null) return;
        setRotation((float) c[3], (float) c[4]);
        setPos(c[0], c[1], c[2]);
    }
}
