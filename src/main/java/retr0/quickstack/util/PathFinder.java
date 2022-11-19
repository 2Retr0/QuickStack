package retr0.quickstack.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import java.util.Set;
import java.util.function.BiFunction;

import static net.minecraft.util.math.Direction.*;
import static net.minecraft.world.RaycastContext.FluidHandling.WATER;
import static net.minecraft.world.RaycastContext.ShapeType.COLLIDER;

public class PathFinder {
    private final ChunkCache chunkCache;
    private final BiFunction<Vec3d, Vec3d, Boolean> nearLineOfSightCached;

    // Set of blocks which are occupied in the center, but should be ignored for line-of-sight calculations.
    private static final Set<Block> IGNORED_BLOCKS = Set.of(
        Blocks.CHAIN, Blocks.END_ROD, Blocks.LANTERN, Blocks.LIGHTNING_ROD);

    public PathFinder(World world, int i, BlockPos from) {
        chunkCache = new ChunkCache(world, from.add(-i, -i, -i), from.add(i, i, i));
        nearLineOfSightCached = Util.memoize((start, end) -> hasNearLineOfSight(start, end, i));
    }

    /**
     * Changes the probabilities for the fall distance at which farmland will break as such:
     * <ol>
     *    <li><b>Pathfinding Step:</b> The starting position will greedily move 2–4 blocks from its original position
     *        in an attempt to give itself a clear line of sight to the end position.</li>
     *    <li><b>Raycast Step:</b> From the final starting position, a ray will be casted to check for a clear line of
     *        sight to the end position. This step will fail if the distance to the end position + the distance of the
     *        pathfinding step path is greater than the specified {@code PathFinder} distance.</li>
     * </ol>
     * <em>Note: Because the pathfinding step is done with respect to the starting position, running the method
     *    {@code start}→{@code end} may return a different value than running the method {@code end}→{@code start}.</em>
     * @param start
     * @param end
     * @return
     */
    public boolean hasNearLineOfSight(Vec3d start, Vec3d end) {
        return nearLineOfSightCached.apply(start, end);
    }

    private boolean hasNearLineOfSight(Vec3d start, Vec3d end, int distance) {
        var blockPos = new BlockPos(start.x, start.y, start.z);
        var directionToEnd = start.subtract(end); // Vector from start->end centered about the origin.
        QuickStack.LOGGER.info("--------- START (" + start + "->" + end + "): " + blockPos);

        //*** DIRECTION PRIORITIZATION STEP ***//
        // Based on the direction to the end, we assign a direction to each axis.
        var xDir = directionToEnd.x > 0 ? WEST : EAST;
        var yDir = directionToEnd.y > 0 ? DOWN : UP;
        var zDir = directionToEnd.z > 0 ? NORTH : SOUTH;

        // We prioritize the axis with the greatest magnitude the highest priority (for the first two path steps).
        var prioritizeXAxis = Math.abs(directionToEnd.x) > Math.abs(directionToEnd.z);
        Direction aDir, bDir;
        if (prioritizeXAxis) {
            aDir = xDir;
            bDir = zDir;
        } else {
            aDir = zDir;
            bDir = xDir;
        }
        // Assume the opposite direction of the highest priority is the lowest priority, etc...
        var priorityList = new Direction[]{ aDir, bDir, yDir, yDir.getOpposite(), bDir.getOpposite(), aDir.getOpposite() };
        QuickStack.LOGGER.info("--------- --- priority list:" + Arrays.toString(priorityList));

        //*** PATHFINDING STEP ***//
        var distanceTravelled = 2;
        Direction prevDirection = null;
        for (var i = 0; i < 2; ++i) {
            prevDirection = getNextDirection(blockPos, prevDirection, priorityList);

            if (prevDirection == null) return false;

            blockPos = blockPos.add(prevDirection.getVector());
            QuickStack.LOGGER.info("--------- NOW:" + blockPos);
        }

        priorityList = new Direction[]{ yDir }; // Only allow movement in the previously-determined y-axis direction.
        QuickStack.LOGGER.info("--------- --- priority list:" + Arrays.toString(priorityList));
        // We allow *up to* two pathfinding iterations in the y-direction, only limited if moving in that direction for
        // two iterations will overshoot the y-pos of the end position.
        for (var i = 0; i < Math.min((int) Math.abs(start.y - end.y), 2); ++i) {
            prevDirection = getNextDirection(blockPos, prevDirection, priorityList);

            // Unlike the previous two iterations, we will not fail if there are no valid spaces to move.
            if (prevDirection == null) break;

            blockPos = blockPos.add(prevDirection.getVector());
            QuickStack.LOGGER.info("--------- NOW:" + blockPos);
            ++distanceTravelled;
        }

        var finalPos = Vec3d.ofCenter(blockPos);
        // Distance check TODO: TEST
        if (finalPos.distanceTo(end) + distanceTravelled > MathHelper.square(distance))
            return false;

        //*** RAYCAST STEP ***//
        MinecraftClient.getInstance().debugRenderer.gameTestDebugRenderer.addMarker(blockPos, 0xFFFFFF, ">O<", 5000);
        return hasLineOfSight(chunkCache, finalPos, end, IGNORED_BLOCKS);
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

        return hasLineOfSight(chunkCache, raycastStart, raycastStart.add(halfDir), IGNORED_BLOCKS);
    }




    @SuppressWarnings("ConstantConditions")
    public static boolean hasLineOfSight(BlockView world, Vec3d start, Vec3d end, @Nullable Set<Block> ignoredBlocks) {
        //   * Note: 'blockHitFactory' lambda runs if any block has been hit.
        //   * Note: 'missFactory' lambda runs if 'end' is reached and no collision with a non-ignored block occured.
        return BlockView.raycast(start, end, new RaycastContext(start, end, COLLIDER, WATER, null),
            (raycastContext, blockPos) -> {
                var blockState = world.getBlockState(blockPos);
                // We use 'null' to denote intermediary (invalid) states. The ignored set will determine whether we
                // should accept the block for consideration.
                if (blockState.isAir() || (ignoredBlocks != null && ignoredBlocks.contains(blockState.getBlock())))
                    return null;

                // If a non-ignored block is hit, raycast within the block and see if it hits a face on the model.
                //   * Note: RaycastContext#getBlockShape() returns null if no face is hit.
                var blockShape = raycastContext.getBlockShape(blockState, world, blockPos);
                var raycastResult = world.raycastBlock(start, end, blockPos, blockShape, blockState);

                return raycastResult == null ? null : raycastResult.getType() != HitResult.Type.BLOCK;
            },
            (raycastContext) -> true);
    }
}
