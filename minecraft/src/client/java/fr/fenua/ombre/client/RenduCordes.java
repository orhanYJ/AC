package fr.fenua.ombre.client;

import fr.fenua.ombre.bloc.CoteClient;
import fr.fenua.ombre.bloc.MonteCharge;
import fr.fenua.ombre.bloc.PoteauTyrolienne;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/** Dessine les cordes : le câble des tyroliennes, et la corde des monte-charges jusqu'au toit. */
public final class RenduCordes {
    private RenduCordes() {}

    /** Un câble de tyrolienne vu récemment : ses deux bouts (points d'accroche) et leurs poteaux. */
    public record Cable(Vec3d a, Vec3d b, BlockPos poteauA, BlockPos poteauB, long vu) {}

    /** Les câbles dessinés ces dernières secondes (on peut les attraper au vol). */
    public static final java.util.Map<BlockPos, Cable> CABLES = new java.util.concurrent.ConcurrentHashMap<>();

    public static class Poteau implements BlockEntityRenderer<PoteauTyrolienne.Entite> {
        @Override
        public void render(PoteauTyrolienne.Entite e, float dt, MatrixStack m, VertexConsumerProvider vcp, int lum, int ov) {
            if (!e.porteCorde || e.cible == null || e.getWorld() == null) return;
            BlockPos d = e.cible.subtract(e.getPos());
            Vec3d a = new Vec3d(0.5, CoteClient.ACCROCHE, 0.5);
            Vec3d b = new Vec3d(d.getX() + 0.5, d.getY() + CoteClient.ACCROCHE, d.getZ() + 0.5);
            dessiner(m, vcp, e.getWorld(), e.getPos(), a, b, true);
            Vec3d o = Vec3d.of(e.getPos());
            CABLES.put(e.getPos().toImmutable(), new Cable(o.add(a), o.add(b), e.getPos().toImmutable(), e.cible, System.currentTimeMillis()));
        }
        @Override public boolean rendersOutsideBoundingBox(PoteauTyrolienne.Entite e) { return true; }
        @Override public int getRenderDistance() { return 160; }
    }

    public static class Lift implements BlockEntityRenderer<MonteCharge.Entite> {
        @Override
        public void render(MonteCharge.Entite e, float dt, MatrixStack m, VertexConsumerProvider vcp, int lum, int ov) {
            World w = e.getWorld();
            if (w == null) return;
            Direction mur = MonteCharge.murReel(w, e.getPos(), e.getCachedState().get(Properties.HORIZONTAL_FACING));
            if (mur == null) return;
            int sommet = MonteCharge.sommet(w, e.getPos(), mur);
            double x = 0.5 + mur.getOffsetX() * 0.38, z = 0.5 + mur.getOffsetZ() * 0.38;
            dessiner(m, vcp, w, e.getPos(), new Vec3d(x, 0.9, z), new Vec3d(x, sommet - e.getPos().getY() + 0.4, z), false);
        }
        @Override public boolean rendersOutsideBoundingBox(MonteCharge.Entite e) { return true; }
    }

    /** Une corde en deux rubans croisés (comme une laisse), éclairée segment par segment. */
    static void dessiner(MatrixStack m, VertexConsumerProvider vcp, World w, BlockPos origine, Vec3d a, Vec3d b, boolean creux) {
        Vec3d d = b.subtract(a);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        Vec3d travers = h > 0.01 ? new Vec3d(-d.z / h, 0, d.x / h).multiply(0.04) : new Vec3d(0.04, 0, 0);
        Vec3d haut = h > 0.01 ? new Vec3d(0, 0.04, 0) : new Vec3d(0, 0, 0.04);
        int n = Math.max(8, (int) (d.length() * 1.5));
        Matrix4f mat = m.peek().getPositionMatrix();
        for (Vec3d o : new Vec3d[]{travers, haut}) {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getLeash());
            for (int i = 0; i <= n; i++) {
                double t = (double) i / n;
                Vec3d pt = creux ? CoteClient.pointCable(a, b, t) : a.lerp(b, t);
                int lum = WorldRenderer.getLightmapCoordinates(w, BlockPos.ofFloored(
                        origine.getX() + pt.x, origine.getY() + pt.y, origine.getZ() + pt.z));
                float k = (i % 2 == 0) ? 0.75f : 1f;
                int r = (int) (150 * k), g = (int) (112 * k), bl = (int) (70 * k);
                vc.vertex(mat, (float) (pt.x - o.x), (float) (pt.y - o.y), (float) (pt.z - o.z)).color(r, g, bl, 255).light(lum);
                vc.vertex(mat, (float) (pt.x + o.x), (float) (pt.y + o.y), (float) (pt.z + o.z)).color(r, g, bl, 255).light(lum);
            }
        }
    }
}
