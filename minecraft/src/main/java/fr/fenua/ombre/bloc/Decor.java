package fr.fenua.ombre.bloc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Le mobilier de la Prague de 1600. Chaque bloc est une forme (une liste de boîtes en pixels, de 0 à 16),
 * dessinée « face au nord » pour les blocs orientés, et tournée selon leur sens.
 *  - MURAL : on le pose sur un mur, il s'y accroche (gouttière, balcon, lanterne, horloge…).
 *  - LIBRE : il se tourne vers celui qui le pose (banc, étal, charrette…).
 */
public final class Decor {
    private Decor() {}

    /** Pas de collision : on passe au travers. */
    public static final VoxelShape VIDE = VoxelShapes.empty();

    /** Une boîte en pixels (0 à 16), comme dans les modèles. */
    public static VoxelShape b(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
    }

    public static VoxelShape u(VoxelShape... s) {
        VoxelShape r = VoxelShapes.empty();
        for (VoxelShape x : s) r = VoxelShapes.union(r, x);
        return r;
    }

    /** Un bloc décoratif sans orientation. */
    public static class Simple extends Block {
        protected VoxelShape contour = VoxelShapes.fullCube(), collision = null;

        public Simple(Settings s) { super(s); }

        public Simple formes(VoxelShape contour, VoxelShape collision) {
            this.contour = contour;
            this.collision = collision;
            return this;
        }

        @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return contour; }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return collision == null ? contour : collision;
        }
    }

    /** Un bloc décoratif orienté (propriété « facing »). */
    public static class Oriente extends Block {
        protected VoxelShape contour = VoxelShapes.fullCube(), collision = null;
        protected boolean mural;

        public Oriente(Settings s) {
            super(s);
            setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        }

        public Oriente formes(boolean mural, VoxelShape contour, VoxelShape collision) {
            this.mural = mural;
            this.contour = contour;
            this.collision = collision;
            return this;
        }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(Properties.HORIZONTAL_FACING); }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            Direction d;
            if (mural) {
                Direction cote = ctx.getSide();
                d = cote.getAxis().isHorizontal() ? cote.getOpposite() : ctx.getHorizontalPlayerFacing();
            } else {
                d = ctx.getHorizontalPlayerFacing();
            }
            return getDefaultState().with(Properties.HORIZONTAL_FACING, d);
        }

        @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return Formes.tourner(contour, s.get(Properties.HORIZONTAL_FACING));
        }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) {
            return collision == null ? getOutlineShape(s, w, p, c) : Formes.tourner(collision, s.get(Properties.HORIZONTAL_FACING));
        }
    }

    /** Familles de blocs : toutes leurs variantes (couleurs, bois…) partagent le même effet. */
    public static class Etal extends Oriente { public Etal(Settings s) { super(s); } }
    public static class Reverbere extends Oriente { public Reverbere(Settings s) { super(s); } }
    public static class Gouttiere extends Oriente { public Gouttiere(Settings s) { super(s); } }
    public static class Balcon extends Oriente { public Balcon(Settings s) { super(s); } }

    /** Cheminée : de la fumée sort des pots. */
    public static class Cheminee extends Simple {
        public Cheminee(Settings s) { super(s); }

        @Override
        public void randomDisplayTick(BlockState s, World w, BlockPos p, Random r) {
            if (!w.getBlockState(p.up()).isAir() || r.nextInt(3) != 0) return;
            w.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX() + 0.35 + r.nextDouble() * 0.3, p.getY() + 1.3,
                    p.getZ() + 0.35 + r.nextDouble() * 0.3, 0, 0.03, 0);
        }
    }

    /** Charrette de foin : on y plonge sans mal (pas de collision), on s'y cache accroupi. */
    public static class Charrette extends Oriente {
        public Charrette(Settings s) { super(s); }

        @Override
        protected void onEntityCollision(BlockState s, World w, BlockPos p, Entity e) {
            e.slowMovement(s, new Vec3d(0.8, 0.5, 0.8));
        }
    }

    /**
     * Banc : clic droit pour s'asseoir. Assis, on se fond dans le décor : les gardes ne te voient pas
     * (voir Discretion). Maj pour se relever. Le siège est un support d'armure invisible.
     */
    public static class Banc extends Oriente {
        public static final String SIEGE = "ombre_siege";

        public Banc(Settings s) { super(s); }

        @Override
        protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity joueur, BlockHitResult hit) {
            if (w.isClient() || joueur.hasVehicle()) return ActionResult.SUCCESS;
            ArmorStandEntity siege = EntityType.ARMOR_STAND.create(w);
            if (siege == null) return ActionResult.PASS;
            // Le dos du banc est du côté « facing » : on s'assoit en regardant de l'autre côté
            Direction dos = s.get(Properties.HORIZONTAL_FACING);
            siege.refreshPositionAndAngles(pos.getX() + 0.5 - dos.getOffsetX() * 0.1, pos.getY() - 0.2,
                    pos.getZ() + 0.5 - dos.getOffsetZ() * 0.1, dos.getOpposite().asRotation(), 0);
            siege.setInvisible(true);
            siege.setNoGravity(true);
            siege.setInvulnerable(true);
            siege.setSilent(true);
            ((fr.fenua.ombre.mixin.SupportArmureAccesseur) siege).ombre$setMarker(true);
            siege.addCommandTag(SIEGE);
            w.spawnEntity(siege);
            joueur.startRiding(siege, true);
            return ActionResult.SUCCESS;
        }
    }
}
