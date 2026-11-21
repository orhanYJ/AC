package fr.fenua.ombre.client.mixin;

import fr.fenua.ombre.client.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Après que Minecraft a posé bras et jambes, on applique la pose de parkour du joueur. */
@Mixin(BipedEntityModel.class)
public abstract class PosesMixin<T extends LivingEntity> {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void ombre$poses(T entite, float f, float g, float age, float i, float j, CallbackInfo ci) {
        if (entite == MinecraftClient.getInstance().player) Animations.appliquer((BipedEntityModel<?>) (Object) this, age);
        else if (fr.fenua.ombre.client.ExecClient.victime(entite)) fr.fenua.ombre.client.ExecClient.poseVictime((BipedEntityModel<?>) (Object) this);
    }
}
