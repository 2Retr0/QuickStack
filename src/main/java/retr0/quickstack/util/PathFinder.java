package retr0.quickstack.util;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
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

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.util.math.Direction.*;
import static net.minecraft.world.RaycastContext.FluidHandling.WATER;
import static net.minecraft.world.RaycastContext.ShapeType.COLLIDER;

public class PathFinder {
    private static final Set<Block> IGNORED_BLOCKS = Set.of(Blocks.CHAIN, Blocks.END_ROD, Blocks.LANTERN, Blocks.LIGHTNING_ROD);
    // TODO: Fences should be checked to see if they're in a valid state!
    private static final Set<TagKey<Block>> IGNORED_BLOCK_TAGS = Set.of(BlockTags.FENCES, ConventionalBlockTags.CHESTS);

    private final ChunkCache chunkCache;
    private final BiFunction<Vec3d, Vec3d, Boolean> nearLineOfSightCached;

    public PathFinder(World world, BlockPos from, int radius) {
        chunkCache = new ChunkCache(world, from.add(-radius, -radius, -radius), from.add(radius, radius, radius));
        nearLineOfSightCached = Util.memoize((start, end) -> hasNearLineOfSight(start, end, radius));
    }



    /**
     * @see PathFinder#hasNearLineOfSight(Vec3d, Vec3d)
     */
    private boolean hasNearLineOfSight(Vec3d start, Vec3d end, int radius) {
        var blockPos = new BlockPos(MathHelper.floor(start.x), MathHelper.floor(start.y), MathHelper.floor(start.z));
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
        var dirPriorities = new Direction[] { aDir, bDir, yDir, yDir.getOpposite(), bDir.getOpposite(), aDir.getOpposite() };


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

        return hasLineOfSight(chunkCache, finalPos, end, IGNORED_BLOCKS, IGNORED_BLOCK_TAGS);
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
    public static boolean hasLineOfSight(
            BlockView world, Vec3d start, Vec3d end,
            @Nullable Set<Block> ignoredBlocks, @Nullable Set<TagKey<Block>> ignoredBlockTags)
    {
        BiFunction<RaycastContext, BlockPos, Boolean> onHit = (raycastContext, blockPos) -> {
            var blockState = world.getBlockState(blockPos);
            // We use 'null' to denote intermediary (invalid) states. The ignored set will determine whether we
            // should accept the block for consideration.
            if (blockState.isAir()
                || (ignoredBlocks != null && ignoredBlocks.contains(blockState.getBlock()))
                || (ignoredBlockTags != null && blockState.streamTags().anyMatch(ignoredBlockTags::contains)))
            {
                return null;
            }

            // If a non-ignored block is hit, raycast within the block and see if it hits a face on the model.
            //   * Note: RaycastContext#getBlockShape() returns null if no face is hit.
            var blockShape = raycastContext.getBlockShape(blockState, world, blockPos);
            var raycastResult = world.raycastBlock(start, end, blockPos, blockShape, blockState);

            return raycastResult == null ? null : raycastResult.getType() != HitResult.Type.BLOCK;
        };

        Function<RaycastContext, Boolean> onMiss = raycastContext -> true; // Runs if all onHit is null.

        return BlockView.raycast(start, end, new RaycastContext(start, end, COLLIDER, WATER, ShapeContext.absent()), onHit, onMiss);
    }



    /**
     * Checks the center of the neighboring block position from the center of the target position has a direct line of
     * sight in the given direction.
     * @param target The starting position whose center will be considered.
     * @param direction The direction to check.
     * @return {@code true} if there is a direct line of sight between the centers; otherwise, {@code false}.
     * @see PathFinder#hasLineOfSight(BlockView, Vec3d, Vec3d, Set, Set)
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

        return hasLineOfSight(chunkCache, raycastStart, raycastStart.add(halfDir), IGNORED_BLOCKS, IGNORED_BLOCK_TAGS);
    }



    /**
     * Checks for a *rough* line of sight; that is, if moving ~4 blocks from a starting position, there is a line of sight.
     * The algorithm for doing so is roughly split into two parts:
     * <ol>
     *    <li><b>Pathfinding:</b> The starting position will greedily move 2–4 blocks from its original position
     *        in an attempt to give itself a clear line of sight to the end position.</li>
     *    <li><b>Raycast:</b> From the final starting position, a ray will be casted to check for a clear line of
     *        sight to the end position. This step will fail if the distance to the end position + the distance of the
     *        pathfinding step path is greater than the specified {@code PathFinder} distance.</li>
     * </ol>
     * <em>Note: Because the pathfinding step is done with respect to the starting position, running the method
     *    {@code start}→{@code end} may return a different value than running the method {@code end}→{@code start}.</em>
     * @param from The starting position to check from.
     * @param to The ending position to check to.
     * @return {@code true} if there is a *rough* line of sight from the starting to ending position; otherwise,
     *         {@code false}.
     */
    public boolean hasNearLineOfSight(Vec3d from, Vec3d to) {
        return nearLineOfSightCached.apply(from, to);
    }
}
