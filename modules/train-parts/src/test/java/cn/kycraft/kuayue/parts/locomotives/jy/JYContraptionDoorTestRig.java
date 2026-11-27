package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellCompanionBlockEntity;
import cn.kycraft.kuayue.parts.core.multiblock.MultiCellRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real Create routing with recorded host ports; creates no client or server world. */
final class JYContraptionDoorTestRig {
    static final BlockPos ANCHOR = new BlockPos(8, 4, -3);
    final RecordingLevel level;
    final CapturedContraption contraption;
    final CapturedEntity entity;

    JYContraptionDoorTestRig(boolean clientSide) throws Exception {
        level = allocate(RecordingLevel.class);
        var side = Level.class.getDeclaredField("isClientSide");
        side.setAccessible(true);
        side.setBoolean(level, clientSide);
        level.worldBlocks = new HashMap<>();
        level.worldEntities = new HashMap<>();
        level.sounds = new ArrayList<>();
        level.events = new ArrayList<>();
        contraption = new CapturedContraption();
        contraption.localView = allocate(CapturedWorld.class);
        contraption.localView.captured = contraption;
        contraption.localView.realWorld = level;
        entity = new CapturedEntity(level, contraption);
    }

    void add(BlockPos pos, BlockState state, CompoundTag nbt) {
        contraption.getBlocks().put(pos, new StructureBlockInfo(pos, state, nbt));
        var behaviour = MovingInteractionBehaviour.REGISTRY.get(state.getBlock());
        if (behaviour != null) {
            contraption.getInteractors().put(pos, behaviour);
        }
    }

    void addDoor(BlockState state) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("test_payload", "keep_this");
        add(ANCHOR, state, nbt);
    }

    BlockPos addCompanion(BlockPos parent, BlockPos relativeCell) {
        BlockPos cell = parent.offset(relativeCell);
        BlockState state = MultiCellRegistry.MULTI_CELL_COMPANION.getEntry().defaultBlockState();
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("parent_x", -relativeCell.getX());
        nbt.putInt("parent_y", -relativeCell.getY());
        nbt.putInt("parent_z", -relativeCell.getZ());
        add(cell, state, nbt);
        var companion = new MultiCellCompanionBlockEntity(cell, state);
        companion.setParentOffset(parent.subtract(cell));
        contraption.presentBlockEntities.put(cell, companion);
        level.worldEntities.put(cell, companion);
        return cell;
    }

    VoxelShape collisionAt(BlockPos pos) {
        return contraption.localView.getBlockState(pos).getCollisionShape(
                contraption.localView,
                pos,
                CollisionContext.empty()
        );
    }

    boolean click(BlockPos pos) {
        return entity.handlePlayerInteraction(null, pos, Direction.NORTH, InteractionHand.MAIN_HAND);
    }

    BlockState doorState() {
        return contraption.getBlocks().get(ANCHOR).state();
    }

    Vec3 world(Vec3 local) {
        return entity.toGlobalVector(local, 1f);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        var field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    record Sound(Player excludedPlayer, int id, BlockPos worldPos) {}

    static final class RecordingLevel extends ClientLevel {
        Map<BlockPos, BlockState> worldBlocks;
        Map<BlockPos, BlockEntity> worldEntities;
        List<Sound> sounds;
        List<Holder<GameEvent>> events;
        int worldWrites;
        int worldEntityLookups;
        boolean allowWorldWrites;

        private RecordingLevel() {
            super(null, null, null, null, 0, 0, null, null, false, 0);
        }

        @Override public BlockState getBlockState(BlockPos pos) {
            return worldBlocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }
        @Override public BlockEntity getBlockEntity(BlockPos pos) {
            worldEntityLookups++;
            return worldEntities.get(pos);
        }
        @Override public int getHeight() { return 384; }
        @Override public int getMinBuildHeight() { return -64; }
        @Override public long getGameTime() { return 1; }
        @Override public void levelEvent(Player player, int id, BlockPos pos, int data) {
            sounds.add(new Sound(player, id, pos));
        }
        @Override public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) {
            events.add(event);
        }
        @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
            worldWrites++;
            if (!allowWorldWrites) {
                throw new AssertionError("An assembled interaction wrote to the static world: " + pos);
            }
            worldBlocks.put(pos, state);
            return true;
        }
    }

    static final class CapturedContraption extends Contraption {
        final Set<BlockPos> hidden = new HashSet<>();
        CapturedWorld localView;
        int colliderInvalidations;

        @Override public boolean assemble(Level level, BlockPos pos) { return false; }
        @Override public boolean canBeStabilized(Direction facing, BlockPos pos) { return false; }
        @Override public ContraptionType getType() { return null; }
        @Override public void invalidateColliders() { colliderInvalidations++; }
        @Override public boolean isHiddenInPortal(BlockPos pos) { return hidden.contains(pos); }
        @Override public ContraptionWorld getContraptionWorld() { return localView; }
    }

    static final class CapturedWorld extends ContraptionWorld {
        CapturedContraption captured;
        RecordingLevel realWorld;

        private CapturedWorld() { super(null, null); }
        @Override public BlockState getBlockState(BlockPos pos) {
            var info = captured.getBlocks().get(pos);
            return info == null ? Blocks.AIR.defaultBlockState() : info.state();
        }
        // Matches WrappedLevel's real-world BE lookup so the production adapter must replace it.
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return realWorld.getBlockEntity(pos); }
        @Override public int getHeight() { return 384; }
        @Override public int getMinBuildHeight() { return -64; }
    }

    static final class CapturedEntity extends AbstractContraptionEntity {
        private final Level testLevel;
        private final Vec3 worldOffset = new Vec3(100, 20, -50);
        int blockChanges;

        CapturedEntity(Level level, CapturedContraption captured) {
            super(EntityType.MARKER, null);
            testLevel = level;
            contraption = captured;
            captured.entity = this;
        }

        @Override public Level level() { return testLevel; }
        @Override public void setBlock(BlockPos pos, StructureBlockInfo info) {
            blockChanges++;
            contraption.getBlocks().put(pos, info);
        }
        @Override public Vec3 toGlobalVector(Vec3 local, float partialTick) { return local.add(worldOffset); }
        @Override public Vec3 toLocalVector(Vec3 global, float partialTick) { return global.subtract(worldOffset); }
        @Override public void applyLocalTransforms(PoseStack matrices, float partialTick) {}
        @Override public Vec3 applyRotation(Vec3 local, float partialTick) { return local; }
        @Override public Vec3 reverseRotation(Vec3 local, float partialTick) { return local; }
        @Override public ContraptionRotationState getRotationState() { return new ContraptionRotationState(); }
        @Override protected void tickContraption() {}
        @Override protected StructureTransform makeStructureTransform() { return null; }
        @Override protected float getStalledAngle() { return 0; }
        @Override protected void handleStallInformation(double x, double y, double z, float angle) {}
    }
}
