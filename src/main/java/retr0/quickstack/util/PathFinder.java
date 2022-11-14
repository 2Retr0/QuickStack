package retr0.quickstack.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;
import org.jetbrains.annotations.Nullable;
import retr0.quickstack.QuickStack;

import java.util.Arrays;
import java.util.function.BiFunction;

import static net.minecraft.util.math.Direction.*;
import static net.minecraft.world.RaycastContext.FluidHandling.WATER;
import static net.minecraft.world.RaycastContext.ShapeType.COLLIDER;

public class PathFinder {
    private final ChunkCache chunkCache;
    private final BiFunction<Vec3d, Vec3d, Boolean> nearLineOfSightCached;

    public PathFinder(World world, int i, BlockPos from) {
        chunkCache = new ChunkCache(world, from.add(-i, -i, -i), from.add(i, i, i));
        nearLineOfSightCached = Util.memoize((start, end) -> hasNearLineOfSight(start, end, i));
    }

    public boolean hasNearLineOfSight(Vec3d start, Vec3d end) {
        return nearLineOfSightCached.apply(start, end);
    }

    private boolean hasNearLineOfSight(Vec3d start, Vec3d end, int distance) {
        var blockPos = new BlockPos(start.x, start.y, start.z);

        var directionToGoal = start.subtract(end); // vector from start to end center about origin
        QuickStack.LOGGER.info("--------- START (" + start + "->" + end + "): " + blockPos);

        var xDir = directionToGoal.x > 0 ? WEST : EAST;
        var yDir = directionToGoal.y > 0 ? DOWN : UP;
        var zDir = directionToGoal.z > 0 ? NORTH : SOUTH;

        var prioritizeXAxis = Math.abs(directionToGoal.x) > Math.abs(directionToGoal.z);
        Direction aDir, bDir;
        if (prioritizeXAxis) {
            aDir = xDir;
            bDir = zDir;
        } else {
            aDir = zDir;
            bDir = xDir;
        }

        Direction prevDirection = null;
        // Assume the opposite direction of the highest priority is the lowest priority, etc...
        var priorityList = new Direction[]{ aDir, bDir, yDir, yDir.getOpposite(), bDir.getOpposite(), aDir.getOpposite() };
        QuickStack.LOGGER.info("--------- --- priority list:" + Arrays.toString(priorityList));
        for (var i = 0; i < 2 + Math.min((int) Math.abs(start.y - end.y), 2); i++) {
            if (i == 2) {
                priorityList = new Direction[]{ yDir };
                QuickStack.LOGGER.info("--------- --- priority list:" + Arrays.toString(priorityList));
            }
            prevDirection = getNextDirection(blockPos, prevDirection, priorityList);

            if (prevDirection == null) return false;

            blockPos = blockPos.add(prevDirection.getVector());
            QuickStack.LOGGER.info("--------- NOW:" + blockPos);
            --distance;
        }

        var finalPos = Vec3d.ofCenter(blockPos);

        // TODO: TEST DISTANCE CHECK
        QuickStack.LOGGER.info(distance + "");
        if (finalPos.squaredDistanceTo(end) > MathHelper.square(distance)) return false;

        // CHECK OTHER STUFF IF BOTH ARE OBSTRUCTED ( MAY NOT NEED TO BECAUSE IT SHOULD MOVE UP TO GO TO PLAYERS HEAD ANYWAYS)
        MinecraftClient.getInstance().debugRenderer.gameTestDebugRenderer.addMarker(blockPos, 0xFFFFFF, "TEST", 5000);
        return hasLineOfSight(chunkCache, Vec3d.ofCenter(blockPos), end);
    }

    /**
     * Gets the next direction—prioritizing based on a specified list—the first neighboring block of {@code target}
     * whose center is open (i.e. player can click through it).
     */
    private Direction getNextDirection(
        BlockPos target, @Nullable Direction previousDirection, Direction[] directionPriorities)
    {
        for (var direction : directionPriorities) {
            var isPreviousDirection = previousDirection != null && previousDirection.getOpposite() == direction;
            if (!isPreviousDirection && isNeighborFree(target, direction)) {
                QuickStack.LOGGER.info("--------- --- found neighbor:" + direction);
                return direction;
            }
        }
        return null;
    }



    public boolean isNeighborFree(BlockPos target, Direction direction) {
        var dir = direction.getVector();
        var neighborPos = target.add(dir);
        var blockState = chunkCache.getBlockState(neighborPos);

        if (blockState.isSideSolidFullSquare(chunkCache, neighborPos, direction.getOpposite())) {
            QuickStack.LOGGER.info("--------- --- found solid block: " + blockState.getBlock().getName().getString());
            return false;
        } else if (blockState.isAir()) {
            QuickStack.LOGGER.info("--------- --- found air");
            return true;
        }

        var halfDir = Vec3d.of(dir).multiply(0.501);
        var raycastStart = Vec3d.ofCenter(target).add(halfDir);

        return hasLineOfSight(chunkCache, raycastStart, raycastStart.add(halfDir));
    }



    // TODO: ignore lantern, lighting rod, end rod, chain, ... and more?
    @SuppressWarnings("ConstantConditions")
    public static boolean hasLineOfSight(BlockView world, Vec3d start, Vec3d end) {
        return BlockView.raycast(start, end, new RaycastContext(start, end, COLLIDER, WATER, null),
            (raycastContext, blockPos) -> {
                var blockState = world.getBlockState(blockPos);

                if (blockState.isAir()) return null;

                var blockShape = raycastContext.getBlockShape(blockState, world, blockPos);
                var raycastResult = world.raycastBlock(start, end, blockPos, blockShape, blockState);
                return raycastResult == null ? null : raycastResult.getType() != HitResult.Type.BLOCK;
            }, (raycastContext) -> true);
    }
}
