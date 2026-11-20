package fr.fenua.ombre.mixin;

import fr.fenua.ombre.ac.Detection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Un garde ne prend pas le joueur pour cible d'un coup : c'est la détection progressive qui décide (voir ac/Detection). */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void ombre$soupcon(@Nullable LivingEntity cible, CallbackInfo ci) {
        if (Detection.bloquer((MobEntity) (Object) this, cible)) ci.cancel();
    }
}
