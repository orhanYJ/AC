package fr.fenua.ombre.client;

import fr.fenua.ombre.Ombre;
import fr.fenua.ombre.entite.Passant;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/** Les personnages du mod ont un corps de joueur et leurs propres habits (textures dans textures/entity). */
public class RenduHumain<T extends MobEntity> extends BipedEntityRenderer<T, PlayerEntityModel<T>> {
    private final Function<T, Identifier> texture;

    public RenduHumain(EntityRendererFactory.Context ctx, Function<T, Identifier> texture) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.texture = texture;
    }

    @Override public Identifier getTexture(T e) { return texture.apply(e); }

    public static Identifier tex(String nom) { return Identifier.of(Ombre.MOD_ID, "textures/entity/" + nom + ".png"); }

    private static final Identifier[] PASSANTS = new Identifier[Passant.VARIANTES];
    static { for (int i = 0; i < PASSANTS.length; i++) PASSANTS[i] = tex("passant_" + i); }

    public static Identifier passant(Passant p) { return PASSANTS[p.variante()]; }
}
