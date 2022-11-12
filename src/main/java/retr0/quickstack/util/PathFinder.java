package retr0.quickstack.util;

import com.google.common.collect.Maps;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.chunk.ChunkCache;
import org.jetbrains.annotations.Nullable;
import retr0.quickstack.QuickStack;

import java.util.EnumSet;
import java.util.function.Predicate;

import static net.minecraft.world.RaycastContext.FluidHandling.WATER;
import static net.minecraft.world.RaycastContext.ShapeType.COLLIDER;

public class PathFinder {
    ChunkCache chunkCache;

    public PathFinder(World world, int i, BlockPos from) {
        chunkCache = new ChunkCache(world, from.add(-i, -i, -i), from.add(i, i, i));
    }

    public boolean hasNearUnobstructedView(Vec3d start, Vec3d end, int distance) {
        var tokens = distance;

        //     each chest has: N tokens
        //
        //     if it reduces the distance between the target and a new position, each chest can choose to:
        //         * move up to two blocks on the y-axis (-0 tokens)
        //         * move up to two blocks on the x-z plane (-1 token each)
        //     if the new position is a free space (mostly empty block??)
        //
        //     if it chooses neither move, don't proceed
        //
        //     ray cast from the new position to the target for up to remaining tokens as distance (unless
        //     it hits a block then do nothing)
        // Chebyshev distance

        var startingDistance = end.squaredDistanceTo(start);
        var toEnd = start.subtract(end);
        var dir = Direction.getFacing(toEnd.x, toEnd.y, toEnd.z); // dont use! SLOW

        if (Math.abs(toEnd.x) > 0.25d)
            end = new Vec3d(end.x + MathHelper.sign(toEnd.x), end.y, end.z);
        else if (Math.abs(toEnd.y) > 0.25d)
            end = new Vec3d(end.x, end.y + MathHelper.sign(toEnd.y), end.z);
        else if (Math.abs(toEnd.z) > 0.25d)
            end = new Vec3d(end.x, end.y + MathHelper.sign(toEnd.y), end.z);

        // CHECK OTHER STUFF IF BOTH ARE OBSTRUCTED ( MAY NOT NEED TO BECAUSE IT SHOULD MOVE UP TO GO TO PLAYERS HEAD ANYWAYS)
        return false;
    }

    public boolean canMove(Vec3d fromVec, Vec3d toVec, BlockPos from) {
        var blockHitResult = hasBlockIntersection(chunkCache, fromVec, toVec);

        QuickStack.LOGGER.info(blockHitResult + "");
        return false;
    }

    // TODO: ignore lantern, lighting rod, end rod, chain, ... and more?
    @SuppressWarnings("ConstantConditions")
    public static boolean hasBlockIntersection(BlockView world, Vec3d start, Vec3d end) {
        return BlockView.raycast(start, end, new RaycastContext(start, end, COLLIDER, WATER, null),
            (raycastContext, blockPos) -> {
                var blockState = world.getBlockState(blockPos);
                var blockShape = raycastContext.getBlockShape(blockState, world, blockPos);
                var raycastResult = world.raycastBlock(start, end, blockPos, blockShape, blockState);

                return raycastResult != null && raycastResult.getType() == HitResult.Type.BLOCK;
            }, (raycastContext)-> false);
    }



    private static class PathFinderNodeMaker extends LandPathNodeMaker {
        public final BlockPos from;
        private static final float NEUTRAL = 0.0f;

        public PathFinderNodeMaker (BlockPos from) { this.from = from; }


        @Nullable @Override
        public PathNode getStart() {
            var pathNode = this.getNode(new BlockPos(from.getX(), MathHelper.floor(from.getY() + 0.5), from.getX()));
            // if (pathNode != null) {
            //     pathNode.type = this.getNodeType(this.entity, pathNode.getBlockPos());
            //     pathNode.penalty = this.entity.getPathfindingPenalty(pathNode.type);
            // }
            return pathNode;
        }

        @Nullable @Override
        public TargetPathNode getNode(double x, double y, double z) { return super.getNode(x, y, z); }

        @Override
        public int getSuccessors(PathNode[] successors, PathNode node) { return super.getSuccessors(successors, node); }

        @Override
        public PathNodeType getNodeType(BlockView world, int x, int y, int z, MobEntity mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors) {
            var enumSet = EnumSet.noneOf(PathNodeType.class);
            var posNodeType = PathNodeType.BLOCKED; // Node type at x y z
            var bestNearbyNodeType = PathNodeType.BLOCKED;
            var blockPos = mob.getBlockPos();

            posNodeType = super.findNearbyNodeTypes(world, x, y, z, sizeX, sizeY, sizeZ, canOpenDoors, canEnterOpenDoors, enumSet, posNodeType, blockPos);

            if (enumSet.contains(PathNodeType.FENCE))
                return PathNodeType.FENCE;

            for (var sampleNodeType : enumSet) {
                if (getPathfindingPenalty(sampleNodeType) < NEUTRAL)
                    return sampleNodeType;

                // Lower path finding penalty is worse
                if (getPathfindingPenalty(sampleNodeType) >= getPathfindingPenalty(bestNearbyNodeType))
                    bestNearbyNodeType = sampleNodeType;
            }
            if (posNodeType == PathNodeType.OPEN && getPathfindingPenalty(bestNearbyNodeType) == NEUTRAL) {
                return PathNodeType.OPEN;
            }
            return bestNearbyNodeType;
        }

        @SuppressWarnings("ConstantConditions")
        private static float getPathfindingPenalty(PathNodeType type) {
            return (float) Maps.newEnumMap(PathNodeType.class).get(type);
        }

        @Override
        public PathNodeType getDefaultNodeType(BlockView world, int x, int y, int z) {
            var pos = new BlockPos.Mutable(x, y, z);
            var pathNodeType = LandPathNodeMaker.getCommonNodeType(world, pos);

            if (pathNodeType == PathNodeType.OPEN && y >= world.getBottomY() + 1) {
                pathNodeType = LandPathNodeMaker.getCommonNodeType(world, pos.set(x, y - 1, z));
                pathNodeType =
                    pathNodeType == PathNodeType.WALKABLE || pathNodeType == PathNodeType.OPEN ||
                        pathNodeType == PathNodeType.WATER || pathNodeType == PathNodeType.LAVA
                        ? PathNodeType.OPEN : PathNodeType.WALKABLE;
            }
            return pathNodeType;
        }
    }
}
