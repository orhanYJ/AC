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

    /** La victime d'un assassinat : dessinée à sa place dans la chorégraphie, tournée et basculée comme il faut. */
    @Override
    protected void setupTransforms(T e, net.minecraft.client.util.math.MatrixStack m, float progres, float lacet, float td, float echelle) {
        if (!ExecClient.victime(e)) { super.setupTransforms(e, m, progres, lacet, td, echelle); return; }
        double k = ExecClient.k(td);
        net.minecraft.util.math.Vec3d ici = e.getLerpedPos(td), la = ExecClient.posVictime(k, ici);
        m.translate(la.x - ici.x, la.y - ici.y, la.z - ici.z);
        super.setupTransforms(e, m, progres, ExecClient.lacetVictime(k), td, echelle);
        m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-ExecClient.bascule(k)));
    }

    /** Pas de chute sur le côté à la mort : la chorégraphie a déjà couché la victime. */
    @Override
    protected float getLyingAngle(T e) { return ExecClient.victime(e) ? 0 : super.getLyingAngle(e); }

    public static Identifier tex(String nom) { return Identifier.of(Ombre.MOD_ID, "textures/entity/" + nom + ".png"); }

    private static final Identifier[] PASSANTS = new Identifier[Passant.VARIANTES];
    static { for (int i = 0; i < PASSANTS.length; i++) PASSANTS[i] = tex("passant_" + i); }

    public static Identifier passant(Passant p) { return PASSANTS[p.variante()]; }

    private static final String[] FIGURES = {"tycho_brahe", "kepler", "rodolphe_ii", "rabbi_loew"};

    public static Identifier figure(fr.fenua.ombre.entite.Figure f) { return tex(FIGURES[f.qui()]); }
}
