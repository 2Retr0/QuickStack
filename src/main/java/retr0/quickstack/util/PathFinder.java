package retr0.quickstack.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
    // Set of blocks which are occupied in the center, but should be ignored for line-of-sight calculations.
    private static final Set<Block> IGNORED_BLOCKS =
        Set.of(Blocks.CHAIN, Blocks.END_ROD, Blocks.LANTERN, Blocks.LIGHTNING_ROD);

    private final ChunkCache chunkCache;
    private final BiFunction<Vec3d, Vec3d, Boolean> nearLineOfSightCached;

    public PathFinder(World world, int radius, BlockPos from) {
        chunkCache = new ChunkCache(world, from.add(-radius, -radius, -radius), from.add(radius, radius, radius));
        nearLineOfSightCached = Util.memoize((start, end) -> hasNearLineOfSight(start, end, radius));
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

    /**
     * @see PathFinder#hasNearLineOfSight(Vec3d, Vec3d)
     */
    private boolean hasNearLineOfSight(Vec3d start, Vec3d end, int radius) {
        var blockPos = new BlockPos(start.x, start.y, start.z);
        var directionToEnd = start.subtract(end); // Vector from start->end centered about the origin.

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
        var dirPriorities = new Direction[]{ aDir, bDir, yDir, yDir.getOpposite(), bDir.getOpposite(), aDir.getOpposite() };


        //*** PATHFINDING STEP ***//
        var distanceTravelled = 2; // By the end of the first pass, the distance travelled must be 2!
        Direction prevDirection = null;
        for (var i = 0; i < 2; ++i) {
            prevDirection = getNextDirection(blockPos, prevDirection, dirPriorities);

            // Fail if there are no valid spaces to move.
            if (prevDirection == null) return false;

            blockPos = blockPos.add(prevDirection.getVector());
        }

        dirPriorities = new Direction[]{ yDir };
        // We allow *up to* two pathfinding iterations in the y-direction, only stopping if moving in that direction for
        // two iterations will overshoot the y-pos of the end position.
        for (var i = 0; i < Math.min((int) Math.abs(start.y - end.y), 2); ++i) {
            prevDirection = getNextDirection(blockPos, prevDirection, dirPriorities);

            // Unlike the previous two iterations, we will not fail if there are no valid spaces to move.
            if (prevDirection == null) break;

            blockPos = blockPos.add(prevDirection.getVector());
            ++distanceTravelled;
        }


        //*** RAYCAST STEP ***//
        var finalPos = Vec3d.ofCenter(blockPos);
        if (finalPos.distanceTo(end) + distanceTravelled > MathHelper.square(radius))
            return false;

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

            if (!isPreviousDirection && isNeighborFree(target, direction)) return direction;
        }
        return null;
    }



    /**
     * Checks the center of the neighboring block position from the center of the target position has a direct line of
     * sight in the given direction.
     * @param target The starting position whose center will be considered.
     * @param direction The direction to check.
     * @return {@code true} if there is a direct line of sight between the centers; otherwise, {@code false}.
     * @see PathFinder#hasLineOfSight(BlockView, Vec3d, Vec3d, Set)
     */
    public boolean isNeighborFree(BlockPos target, Direction direction) {
        var dir = direction.getVector();
        var neighborPos = target.add(dir);
        var blockState = chunkCache.getBlockState(neighborPos);

        if (blockState.isSideSolidFullSquare(chunkCache, neighborPos, direction.getOpposite()))
            return false;
        else if (blockState.isAir())
            return true;

        var halfDir = Vec3d.of(dir).multiply(0.501);
        var raycastStart = Vec3d.ofCenter(target).add(halfDir);

        return hasLineOfSight(chunkCache, raycastStart, raycastStart.add(halfDir), IGNORED_BLOCKS);
    }



    /**
     * Uses raycasting to determine if there is direct line of sight between a starting and ending position. The raycast
     * hit detection performed is identical to that of the player crosshair hitbox detection when mining, attacking,
     * etc.
     * @param world The world to check in.
     * @param start The exact starting position of the ray.
     * @param end The exact ending position of the ray.
     * @param ignoredBlocks A {@link Set} of blocks to ignore if an intersection occurs.
     * @return {@code true} if there is a direction line of sight from the starting to ending position; otherwise,
     * {@code false}
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean hasLineOfSight(BlockView world, Vec3d start, Vec3d end, @Nullable Set<Block> ignoredBlocks) {
        return BlockView.raycast(start, end, new RaycastContext(start, end, COLLIDER, WATER, null),
            //   * Note: Lambda runs if any block has been hit.
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
            //   * Note: Lambda runs if 'end' is reached and no collision with a non-ignored block occurred.
            (raycastContext) -> true);
    }
}
