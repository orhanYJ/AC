package fr.fenua.ombre.ac;

import fr.fenua.ombre.bloc.Decor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * L'avis de recherche, comme dans Assassin's Creed II : un clic droit l'arrache,
 * et ta notoriété baisse de 30. (Il ne repousse pas : il faut en trouver d'autres.)
 */
public class AvisRecherche extends Decor.Oriente {
    public AvisRecherche(Settings s) { super(s); }

    @Override
    protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity joueur, BlockHitResult hit) {
        if (w.isClient()) return ActionResult.SUCCESS;
        w.removeBlock(pos, false);
        w.playSound(null, pos, SoundEvents.ENTITY_PAINTING_BREAK, SoundCategory.BLOCKS, 1f, 1.3f);
        w.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 0.7f);
        if (w instanceof ServerWorld sw)
            sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, s), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.25, 0.3, 0.25, 0.1);
        if (joueur instanceof ServerPlayerEntity p) {
            if (Notoriete.get(p) > 0) Notoriete.ajouter(p, -30, "avis de recherche arraché");
            else p.sendMessage(net.minecraft.text.Text.literal("§7Avis de recherche arraché — personne ne te cherche encore."), true);
            Mission.marquer(p, "ombre_m_avis");
        }
        return ActionResult.SUCCESS;
    }
}
