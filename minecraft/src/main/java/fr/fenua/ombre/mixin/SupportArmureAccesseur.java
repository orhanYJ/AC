package fr.fenua.ombre.mixin;

import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Donne accès à une méthode privée du support d'armure (le rendre « marqueur » : sans taille ni collision). */
@Mixin(ArmorStandEntity.class)
public interface SupportArmureAccesseur {
    @Invoker("setMarker")
    void ombre$setMarker(boolean marker);
}
