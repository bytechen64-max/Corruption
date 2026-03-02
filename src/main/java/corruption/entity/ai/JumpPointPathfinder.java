package corruption.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.*;

/**
 * JumpPointPathfinder v3
 *
 * 优化：
 * - 支持最大跳跃高度、最大跳跃宽度配置
 * - 岩浆等危险液体不可站立
 * - 间隙跳跃验证水平距离
 */
public class JumpPointPathfinder {

    // ===================== 枚举 =====================

    public enum JumpType {
        NONE, // 正常行走
        UP, // 跳跃上坡（高度差 1-2 格）
        GAP_1, // 跨越 1 格宽间隙
        GAP_2 // 跨越 2 格宽间隙
    }

    public enum State { IDLE, SEARCHING, PATH_FOUND, NO_PATH, DIRECT_CHASE }

    // ===================== 节点 =====================

    public static class PathNode implements Comparable<PathNode> {
        public final BlockPos pos;
        public PathNode parent;
        public float g, h, f;
        public JumpType jumpType;

        PathNode(BlockPos pos, @Nullable PathNode parent,
                 float g, float h, JumpType jumpType) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.jumpType = jumpType;
        }

        @Override public int compareTo(PathNode o) { return Float.compare(f, o.f); }
        @Override public boolean equals(Object o) { return o instanceof PathNode pn && pos.equals(pn.pos); }
        @Override public int hashCode() { return pos.hashCode(); }
    }

    // ===================== 常量 =====================

    public static final int MAX_SCAN_DIST = 16;
    /** 单步允许的最大下落格数（超出 = 深坑，强制绕行） */
    public static final int MAX_STEP_DOWN = 2;
    public static final int DIRECT_CHASE_DIST_SQ = 4;

    // ===================== 字段 =====================

    private final Level level;
    private final float entityHeight;
    private final int maxNodes;
    private final int partialKeepFromEnd;
    private final int maxJumpHeight;
    private final int maxJumpWidth;

    private State state = State.IDLE;
    private BlockPos targetPos;

    private PriorityQueue<PathNode> openQueue;
    private Map<BlockPos, PathNode> openMap;
    private Set<BlockPos> closedSet;
    private int expandedCount;

    private final List<BlockPos> finalPath = new ArrayList<>();
    private final List<JumpType> jumpTypes = new ArrayList<>();

    @Nullable private List<BlockPos> keptPrefix = null;
    @Nullable private List<JumpType> keptPrefixJump = null;

    private static final int[][] DIRS = {
            {1,0},{-1,0},{0,1},{0,-1},
            {1,1},{1,-1},{-1,1},{-1,-1}
    };

    // ===================== 构造 =====================

    public JumpPointPathfinder(Level level, float entityHeight) {
        this(level, entityHeight, 512, 5, 2, 2);
    }

    public JumpPointPathfinder(Level level, float entityHeight,
                               int maxNodes, int partialKeepFromEnd) {
        this(level, entityHeight, maxNodes, partialKeepFromEnd, 2, 2);
    }

    public JumpPointPathfinder(Level level, float entityHeight,
                               int maxNodes, int partialKeepFromEnd,
                               int maxJumpHeight, int maxJumpWidth) {
        this.level = level;
        this.entityHeight = entityHeight;
        this.maxNodes = maxNodes;
        this.partialKeepFromEnd = partialKeepFromEnd;
        this.maxJumpHeight = maxJumpHeight;
        this.maxJumpWidth = maxJumpWidth;
    }

    // ===================== 公开 API =====================

    public void startSearch(BlockPos from, BlockPos to) {
        targetPos = to;
        keptPrefix = null;
        keptPrefixJump = null;

        if (from.distSqr(to) <= DIRECT_CHASE_DIST_SQ) {
            state = State.DIRECT_CHASE;
            finalPath.clear(); jumpTypes.clear();
            return;
        }
        initSearch(from);
    }

    public void updateTarget(BlockPos newTarget, BlockPos entityCurrentPos) {
        if (newTarget.equals(targetPos)) return;
        targetPos = newTarget;

        if (entityCurrentPos.distSqr(targetPos) <= DIRECT_CHASE_DIST_SQ) {
            state = State.DIRECT_CHASE;
            finalPath.clear(); jumpTypes.clear();
            return;
        }

        if (state == State.PATH_FOUND && finalPath.size() > partialKeepFromEnd + 1) {
            int keepCount = finalPath.size() - partialKeepFromEnd;
            keptPrefix = new ArrayList<>(finalPath.subList(0, keepCount));
            keptPrefixJump = new ArrayList<>(jumpTypes.subList(0, keepCount));
            initSearch(keptPrefix.get(keptPrefix.size() - 1));
        } else {
            keptPrefix = null; keptPrefixJump = null;
            initSearch(entityCurrentPos);
        }
    }

    public State tickSearch() {
        if (state != State.SEARCHING) return state;

        if (openQueue.isEmpty()) { state = State.NO_PATH; return state; }
        if (expandedCount >= maxNodes) { buildPath(openQueue.peek(), true); return state; }

        PathNode current = openQueue.poll();
        openMap.remove(current.pos);

        if (isNearTarget(current.pos)) { buildPath(current, false); return state; }

        closedSet.add(current.pos);
        expandedCount++;
        expandNode(current);
        return state;
    }

    // ===================== 初始化 =====================

    private void initSearch(BlockPos from) {
        openQueue = new PriorityQueue<>();
        openMap = new HashMap<>();
        closedSet = new HashSet<>();
        finalPath.clear(); jumpTypes.clear();
        expandedCount = 0;
        state = State.SEARCHING;

        PathNode s = new PathNode(from, null, 0, heuristic(from, targetPos), JumpType.NONE);
        openQueue.add(s);
        openMap.put(from, s);
    }

    // ===================== JPS 展开 =====================

    private void expandNode(PathNode current) {
        for (int[] d : DIRS) {
            for (PathNode succ : jpsScan(current, d[0], d[1])) {
                if (closedSet.contains(succ.pos)) continue;
                PathNode ex = openMap.get(succ.pos);
                if (ex == null) {
                    openQueue.add(succ); openMap.put(succ.pos, succ);
                } else if (succ.g < ex.g) {
                    openQueue.remove(ex);
                    ex.g = succ.g; ex.f = succ.g + ex.h;
                    ex.parent = succ.parent; ex.jumpType = succ.jumpType;
                    openQueue.add(ex);
                }
            }
        }
    }

    private List<PathNode> jpsScan(PathNode from, int dx, int dz) {
        List<PathNode> result = new ArrayList<>();
        boolean diagonal = (dx != 0 && dz != 0);

        int cx = from.pos.getX();
        int cy = from.pos.getY();
        int cz = from.pos.getZ();

        for (int step = 1; step <= MAX_SCAN_DIST; step++) {
            int nx = cx + dx * step;
            int nz = cz + dz * step;

            StepResult sr = resolveStep(nx, cy, nz);

            if (sr == null) {
                // 此格不可直接到达，检查是否可以跳过间隙
                GapJump gj = tryFindGapJump(nx, cy, nz, dx, dz);
                if (gj != null) {
                    result.add(makeNode(gj.landPos, from, gj.jumpType));
                }
                break;
            }

            int ny = sr.y;
            JumpType jt = sr.type;
            BlockPos nextPos = new BlockPos(nx, ny, nz);
            float gCost = from.g + moveCost(from.pos, nextPos);
            float hCost = heuristic(nextPos, targetPos);

            if (isNearTarget(nextPos)) {
                result.add(new PathNode(nextPos, from, gCost, hCost, jt));
                break;
            }

            if (diagonal) {
                PathNode vn = new PathNode(nextPos, from, gCost, hCost, jt);
                if (!jpsScan(vn, dx, 0).isEmpty() || !jpsScan(vn, 0, dz).isEmpty()) {
                    result.add(vn); break;
                }
            } else {
                if (hasForcedNeighbor(nx, ny, nz, dx, dz)) {
                    result.add(new PathNode(nextPos, from, gCost, hCost, jt));
                    break;
                }
            }

            cy = ny; // 跟随地形
        }

        return result;
    }

    // ===================== 地形解析 =====================

    private record StepResult(int y, JumpType type) {}

    /**
     * 解析从 startY 高度移动到 (x, z) 时的目标高度与跳跃类型。
     * 支持最大跳跃高度配置。
     */
    @Nullable
    private StepResult resolveStep(int x, int startY, int z) {
        // 同高
        if (canStandAt(x, startY, z)) return new StepResult(startY, JumpType.NONE);
        // 低 1 格
        if (canStandAt(x, startY - 1, z)) return new StepResult(startY - 1, JumpType.NONE);
        // 低 2 格
        if (canStandAt(x, startY - 2, z)) return new StepResult(startY - 2, JumpType.NONE);
        // 高 1 格
        if (canStandAt(x, startY + 1, z)) return new StepResult(startY + 1, JumpType.NONE);
        // 高 2 格及以上（但不超过最大跳跃高度）
        for (int h = 2; h <= maxJumpHeight; h++) {
            if (canStandAt(x, startY + h, z)) {
                return new StepResult(startY + h, JumpType.UP);
            }
        }
        return null; // 不可通行
    }

    private record GapJump(BlockPos landPos, JumpType jumpType) {}

    /**
     * 检测水平间隙是否可跳过，并验证跳跃距离是否在最大跳跃宽度内。
     */
    @Nullable
    private GapJump tryFindGapJump(int gapX, int startY, int gapZ, int dx, int dz) {
        if (!isGapAt(gapX, startY, gapZ)) return null;

        // ---- 1 格间隙 ----
        int l1x = gapX + dx, l1z = gapZ + dz;
        for (int dy = 1; dy >= -1; dy--) {
            if (canStandAt(l1x, startY + dy, l1z)) {
                // 水平距离不超过最大跳跃宽度
                if (Math.abs(dx) + Math.abs(dz) <= maxJumpWidth) {
                    return new GapJump(new BlockPos(l1x, startY + dy, l1z), JumpType.GAP_1);
                }
            }
        }

        // ---- 2 格间隙 ----
        if (maxJumpWidth >= 2) {
            int g2x = gapX + dx, g2z = gapZ + dz;
            if (isGapAt(g2x, startY, g2z)) {
                int l2x = gapX + dx * 2, l2z = gapZ + dz * 2;
                for (int dy = 1; dy >= -1; dy--) {
                    if (canStandAt(l2x, startY + dy, l2z)) {
                        if (Math.abs(dx * 2) + Math.abs(dz * 2) <= maxJumpWidth) {
                            return new GapJump(new BlockPos(l2x, startY + dy, l2z), JumpType.GAP_2);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 判断 (x, y, z) 是间隙（坑/悬崖边）而非固体墙。
     */
    private boolean isGapAt(int x, int y, int z) {
        if (level.getBlockState(new BlockPos(x, y - 1, z)).isSolid()) return false;
        if (level.getBlockState(new BlockPos(x, y, z)).isSolid()) return false;
        for (int dy = 1; dy <= 4; dy++) {
            if (!level.getBlockState(new BlockPos(x, y - dy, z)).isSolid()) return true;
        }
        return false;
    }

    private boolean hasForcedNeighbor(int nx, int ny, int nz, int dx, int dz) {
        if (dz == 0) {
            return (resolveStep(nx, ny, nz + 1) != null && resolveStep(nx - dx, ny, nz + 1) == null)
                    || (resolveStep(nx, ny, nz - 1) != null && resolveStep(nx - dx, ny, nz - 1) == null);
        } else if (dx == 0) {
            return (resolveStep(nx + 1, ny, nz) != null && resolveStep(nx + 1, ny, nz - dz) == null)
                    || (resolveStep(nx - 1, ny, nz) != null && resolveStep(nx - 1, ny, nz - dz) == null);
        }
        return false;
    }

    // ===================== 地形基础 =====================

    /**
     * 判断某个位置是否可站立（地面固体 + 头部空间无固体 + 无危险液体）
     */
    private boolean canStandAt(int x, int y, int z) {
        BlockPos ground = new BlockPos(x, y - 1, z);
        if (!level.getBlockState(ground).isSolid()) return false;

        // 防止站在岩浆上
        FluidState groundFluid = level.getFluidState(ground);
        if (groundFluid.getType() == Fluids.LAVA || groundFluid.getType() == Fluids.FLOWING_LAVA) {
            return false;
        }

        int h = (int) Math.ceil(entityHeight);
        for (int i = 0; i < h; i++) {
            BlockPos bp = new BlockPos(x, y + i, z);
            if (level.getBlockState(bp).isSolid()) return false;
            FluidState fs = level.getFluidState(bp);
            if (!fs.isEmpty()) {
                // 岩浆不可站立，水等可根据需求允许（此处仅禁止岩浆）
                if (fs.getType() == Fluids.LAVA || fs.getType() == Fluids.FLOWING_LAVA) {
                    return false;
                }
            }
        }
        return true;
    }

    // ===================== 路径构建 =====================

    private void buildPath(PathNode endNode, boolean bestEffort) {
        LinkedList<BlockPos> path = new LinkedList<>();
        LinkedList<JumpType> jumps = new LinkedList<>();
        PathNode cur = endNode;
        while (cur != null) { path.addFirst(cur.pos); jumps.addFirst(cur.jumpType); cur = cur.parent; }

        finalPath.clear(); jumpTypes.clear();
        if (keptPrefix != null) {
            finalPath.addAll(keptPrefix); jumpTypes.addAll(keptPrefixJump);
            keptPrefix = null; keptPrefixJump = null;
        }
        finalPath.addAll(path); jumpTypes.addAll(jumps);
        state = State.PATH_FOUND;
    }

    // ===================== 工具 =====================

    private PathNode makeNode(BlockPos pos, PathNode parent, JumpType jt) {
        return new PathNode(pos, parent,
                parent.g + moveCost(parent.pos, pos),
                heuristic(pos, targetPos), jt);
    }

    private boolean isNearTarget(BlockPos pos) {
        return pos.equals(targetPos) || pos.distSqr(targetPos) <= 2;
    }

    private static float heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return Math.max(Math.max(dx, dz), dy);
    }

    private static float moveCost(BlockPos a, BlockPos b) {
        float dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
        return (float) Math.sqrt(dx*dx + dz*dz) + Math.abs(dy) * 0.5f;
    }

    // ===================== Getter =====================

    public State getState() { return state; }
    public List<BlockPos> getFinalPath() { return Collections.unmodifiableList(finalPath); }
    public List<JumpType> getJumpTypes() { return Collections.unmodifiableList(jumpTypes); }
    public BlockPos getTargetPos() { return targetPos; }
    public int getExpandedCount() { return expandedCount; }

    public JumpType getJumpType(int i) {
        return (i >= 0 && i < jumpTypes.size()) ? jumpTypes.get(i) : JumpType.NONE;
    }

    public void reset() {
        state = State.IDLE;
        finalPath.clear(); jumpTypes.clear();
        if (openQueue != null) openQueue.clear();
        if (openMap != null) openMap.clear();
        if (closedSet != null) closedSet.clear();
        keptPrefix = null; keptPrefixJump = null;
        expandedCount = 0;
    }
}