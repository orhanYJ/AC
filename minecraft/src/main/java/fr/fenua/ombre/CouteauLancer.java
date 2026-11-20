package fr.fenua.ombre;

import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Couteaux de lancer : clic droit, un couteau part droit devant, vite et sans bruit.
 * Il tue un garde d'un coup (24 points de dégâts au moins), mais on ne le ramasse pas.
 */
public class CouteauLancer extends Item {
    public CouteauLancer(Settings s) { super(s); }

    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity joueur, Hand main) {
        ItemStack pile = joueur.getStackInHand(main);
        if (!w.isClient()) {
            ArrowEntity c = new ArrowEntity(w, joueur, new ItemStack(Items.ARROW), null);
            c.setVelocity(joueur, joueur.getPitch(), joueur.getYaw(), 0f, 3.2f, 0.4f);
            c.setDamage(8.0);
            c.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
            w.spawnEntity(c);
            w.playSound(null, joueur.getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.PLAYERS, 0.4f, 1.8f);
            joueur.getItemCooldownManager().set(this, 8);
            pile.decrementUnlessCreative(1, joueur);
        }
        joueur.swingHand(main);
        return TypedActionResult.success(pile, w.isClient());
    }
}
