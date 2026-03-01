package corruption.entity.custom.baseEntity.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.*;

/**
 * JumpPointPathfinder v2
 *
 * 核心修复：
 *  - resolveStep(): 单步下落 ≤ 2 格，超出则视为坑（不可通行），强制绕路
 *  - isGapAt(): 识别真正的水平间隙
 *  - tryFindGapJump(): 检测 1 格宽 / 2 格宽间隙是否可跳过
 *  - JumpType 区分 UP / GAP_1 / GAP_2，供 SmartMoveGoal 提前触发跳跃
 */
public class JumpPointPathfinder {

    // ===================== 枚举 =====================

    public enum JumpType {
        NONE,   // 正常行走
        UP,     // 跳跃上坡（高度差 1-2 格）
        GAP_1,  // 跨越 1 格宽间隙
        GAP_2   // 跨越 2 格宽间隙
    }

    public enum State { IDLE, SEARCHING, PATH_FOUND, NO_PATH, DIRECT_CHASE }

    // ===================== 节点 =====================

    public static class PathNode implements Comparable<PathNode> {
        public final BlockPos pos;
        public PathNode   parent;
        public float      g, h, f;
        public JumpType   jumpType;

        PathNode(BlockPos pos, @Nullable PathNode parent,
                 float g, float h, JumpType jumpType) {
            this.pos      = pos;
            this.parent   = parent;
            this.g        = g;
            this.h        = h;
            this.f        = g + h;
            this.jumpType = jumpType;
        }

        @Override public int compareTo(PathNode o)  { return Float.compare(f, o.f); }
        @Override public boolean equals(Object o)   { return o instanceof PathNode pn && pos.equals(pn.pos); }
        @Override public int hashCode()             { return pos.hashCode(); }
    }

    // ===================== 常量 =====================

    public static final int MAX_SCAN_DIST        = 16;
    /** 单步允许的最大下落格数（超出 = 深坑，强制绕行） */
    public static final int MAX_STEP_DOWN        = 2;
    public static final int MAX_JUMP_HEIGHT      = 2;
    public static final int MAX_GAP_WIDTH        = 2;
    public static final int DIRECT_CHASE_DIST_SQ = 4;

    // ===================== 字段 =====================

    private final Level   level;
    private final float   entityHeight;
    private final int     maxNodes;
    private final int     partialKeepFromEnd;

    private State                state      = State.IDLE;
    private BlockPos             targetPos;

    private PriorityQueue<PathNode>  openQueue;
    private Map<BlockPos, PathNode>  openMap;
    private Set<BlockPos>            closedSet;
    private int                      expandedCount;

    private final List<BlockPos>  finalPath = new ArrayList<>();
    private final List<JumpType>  jumpTypes = new ArrayList<>();

    @Nullable private List<BlockPos>  keptPrefix     = null;
    @Nullable private List<JumpType>  keptPrefixJump = null;

    private static final int[][] DIRS = {
            {1,0},{-1,0},{0,1},{0,-1},
            {1,1},{1,-1},{-1,1},{-1,-1}
    };

    // ===================== 构造 =====================

    public JumpPointPathfinder(Level level, float entityHeight) {
        this(level, entityHeight, 512, 5);
    }

    public JumpPointPathfinder(Level level, float entityHeight,
                               int maxNodes, int partialKeepFromEnd) {
        this.level              = level;
        this.entityHeight       = entityHeight;
        this.maxNodes           = maxNodes;
        this.partialKeepFromEnd = partialKeepFromEnd;
    }

    // ===================== 公开 API =====================

    public void startSearch(BlockPos from, BlockPos to) {
        targetPos      = to;
        keptPrefix     = null;
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
            int keepCount      = finalPath.size() - partialKeepFromEnd;
            keptPrefix         = new ArrayList<>(finalPath.subList(0, keepCount));
            keptPrefixJump     = new ArrayList<>(jumpTypes.subList(0, keepCount));
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
        openQueue     = new PriorityQueue<>();
        openMap       = new HashMap<>();
        closedSet     = new HashSet<>();
        finalPath.clear(); jumpTypes.clear();
        expandedCount = 0;
        state         = State.SEARCHING;

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
        List<PathNode> result   = new ArrayList<>();
        boolean        diagonal = (dx != 0 && dz != 0);

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

            int      ny      = sr.y;
            JumpType jt      = sr.type;
            BlockPos nextPos = new BlockPos(nx, ny, nz);
            float    gCost   = from.g + moveCost(from.pos, nextPos);
            float    hCost   = heuristic(nextPos, targetPos);

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

    // ===================== 地形解析（核心修复）=====================

    private record StepResult(int y, JumpType type) {}

    /**
     * 解析从 startY 高度移动到 (x, z) 时的目标高度与跳跃类型。
     *
     * - 同高 / 低1-2格（下坡）→ NONE
     * - 高1格（台阶）        → NONE
     * - 高2格（需跳跃）      → UP
     * - 低3格以上（深坑/悬崖）→ null（强制绕行）
     * - 无任何可站立面       → null
     */
    @Nullable
    private StepResult resolveStep(int x, int startY, int z) {
        // 同高
        if (canStandAt(x, startY, z))     return new StepResult(startY, JumpType.NONE);
        // 低 1 格（下坡一步）
        if (canStandAt(x, startY - 1, z)) return new StepResult(startY - 1, JumpType.NONE);
        // 低 2 格（下坡两步，仍可接受）
        if (canStandAt(x, startY - 2, z)) return new StepResult(startY - 2, JumpType.NONE);
        // 高 1 格（台阶，正常行走可翻越）
        if (canStandAt(x, startY + 1, z)) return new StepResult(startY + 1, JumpType.NONE);
        // 高 2 格（需跳跃）
        if (canStandAt(x, startY + 2, z)) return new StepResult(startY + 2, JumpType.UP);
        // 其余情况：深坑（>2格下落）或高墙（>2格上升）→ 不可通行
        return null;
    }

    private record GapJump(BlockPos landPos, JumpType jumpType) {}

    /**
     * 当 (gapX, gapZ) 处无法直接步行到达时，检测是否为可跳过的水平间隙。
     *
     * 判断条件：
     *  1. (gapX, gapZ) 确实是一个间隙（脚下空旷，不是实心墙）
     *  2. 间隙另一侧（1格 or 2格之外）存在可站立位置
     *  3. 落点高度不超过起跳点高度（否则跳跃无法到达）
     */
    @Nullable
    private GapJump tryFindGapJump(int gapX, int startY, int gapZ, int dx, int dz) {
        if (!isGapAt(gapX, startY, gapZ)) return null;

        // ---- 1 格间隙 ----
        int l1x = gapX + dx, l1z = gapZ + dz;
        for (int dy = 1; dy >= -1; dy--) {
            if (canStandAt(l1x, startY + dy, l1z)) {
                return new GapJump(new BlockPos(l1x, startY + dy, l1z), JumpType.GAP_1);
            }
        }

        // ---- 2 格间隙（第2格也是间隙才算）----
        int g2x = gapX + dx, g2z = gapZ + dz;
        if (isGapAt(g2x, startY, g2z)) {
            int l2x = gapX + dx * 2, l2z = gapZ + dz * 2;
            // 2格间隙落点只接受同高或低1格
            if (canStandAt(l2x, startY,     l2z)) return new GapJump(new BlockPos(l2x, startY,     l2z), JumpType.GAP_2);
            if (canStandAt(l2x, startY - 1, l2z)) return new GapJump(new BlockPos(l2x, startY - 1, l2z), JumpType.GAP_2);
        }

        return null;
    }

    /**
     * 判断 (x, y, z) 是间隙（坑/悬崖边）而非固体墙。
     * 条件：脚下无固体 + 身体空间无固体 + 下方确有空气
     */
    private boolean isGapAt(int x, int y, int z) {
        // 脚下有固体 → 正常可走，不是间隙
        if (level.getBlockState(new BlockPos(x, y - 1, z)).isSolid()) return false;
        // 身体空间是固体 → 这是墙，不是坑
        if (level.getBlockState(new BlockPos(x, y, z)).isSolid()) return false;
        // 下方至少一格是空气 → 确认是坑
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

    private boolean canStandAt(int x, int y, int z) {
        if (!level.getBlockState(new BlockPos(x, y - 1, z)).isSolid()) return false;
        int h = (int) Math.ceil(entityHeight);
        for (int i = 0; i < h; i++) {
            BlockPos bp = new BlockPos(x, y + i, z);
            if (level.getBlockState(bp).isSolid()) return false;
            FluidState fs = level.getFluidState(bp);
            if (!fs.isEmpty() && fs.getType() != Fluids.EMPTY) return false;
        }
        return true;
    }

    // ===================== 路径构建 =====================

    private void buildPath(PathNode endNode, boolean bestEffort) {
        LinkedList<BlockPos> path  = new LinkedList<>();
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

    public State          getState()         { return state; }
    public List<BlockPos> getFinalPath()     { return Collections.unmodifiableList(finalPath); }
    public List<JumpType> getJumpTypes()     { return Collections.unmodifiableList(jumpTypes); }
    public BlockPos       getTargetPos()     { return targetPos; }
    public int            getExpandedCount() { return expandedCount; }

    public JumpType getJumpType(int i) {
        return (i >= 0 && i < jumpTypes.size()) ? jumpTypes.get(i) : JumpType.NONE;
    }

    public void reset() {
        state = State.IDLE;
        finalPath.clear(); jumpTypes.clear();
        if (openQueue != null) openQueue.clear();
        if (openMap   != null) openMap.clear();
        if (closedSet != null) closedSet.clear();
        keptPrefix = null; keptPrefixJump = null;
        expandedCount = 0;
    }
}