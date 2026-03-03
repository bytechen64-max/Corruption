package corruption.util.effect;

import corruption.block.ModBlocks;
import corruption.block.custom.baseBlock.Remains;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class RipBodyUtil {

    // 获取已注册的残骸方块列表
    private static List<RegistryObject<Block>> getRemainsTypes() {
        List<RegistryObject<Block>> remainsTypes = new ArrayList<>();

        // 添加已注册的残骸方块
        remainsTypes.add(ModBlocks.HOST_REMAINS_LARGE);
        remainsTypes.add(ModBlocks.HOST_REMAINS_MEDIUM);
        remainsTypes.add(ModBlocks.HOST_REMAINS_SMALL);

        return remainsTypes;
    }

    /**
     * 以实体为中心，辐射状生成残骸方块
     * @param entity 中心实体
     * @param radius 生成半径
     * @param count 生成数量
     */
    public static void hostRip(Entity entity, int radius, int count) {
        Level level = entity.level();
        if (level.isClientSide()) return; // 只在服务端执行

        BlockPos centerPos = entity.blockPosition();
        RandomSource random = level.getRandom();

        // 获取已注册的残骸方块类型
        List<RegistryObject<Block>> remainsRegistryObjects = getRemainsTypes();

        // 生成候选位置列表（球形区域）
        List<BlockPos> candidatePositions = generateCandidatePositions(centerPos, radius, random);

        int placedCount = 0;
        int attempts = 0;
        int maxAttempts = count * 10; // 防止无限循环

        while (placedCount < count && attempts < maxAttempts && !candidatePositions.isEmpty()) {
            attempts++;

            // 随机选择一个候选位置
            int index = random.nextInt(candidatePositions.size());
            BlockPos candidatePos = candidatePositions.get(index);
            candidatePositions.remove(index);

            // 尝试在该位置放置残骸
            if (tryPlaceRemains(level, candidatePos, random, remainsRegistryObjects)) {
                placedCount++;
            }
        }

        // 输出调试信息（可选）
        if (placedCount > 0) {

        }
    }

    /**
     * 生成候选位置列表（球形区域）
     */
    private static List<BlockPos> generateCandidatePositions(BlockPos center, int radius, RandomSource random) {
        List<BlockPos> positions = new ArrayList<>();
        int radiusSquared = radius * radius;

        // 生成球形区域内的所有可能位置
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // 检查是否在球体范围内
                    if (x * x + y * y + z * z <= radiusSquared) {
                        BlockPos pos = center.offset(x, y, z);
                        positions.add(pos);
                    }
                }
            }
        }

        // 随机打乱位置列表，实现随机分布
        java.util.Collections.shuffle(positions, new java.util.Random(random.nextLong()));
        return positions;
    }

    /**
     * 尝试在指定位置放置残骸方块
     */
    private static boolean tryPlaceRemains(Level level, BlockPos pos, RandomSource random, List<RegistryObject<Block>> remainsRegistryObjects) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }

        if (remainsRegistryObjects.isEmpty()) {
            return false;
        }

        RegistryObject<Block> remainsRegistryObject = remainsRegistryObjects.get(random.nextInt(remainsRegistryObjects.size()));
        Block remainsBlock = remainsRegistryObject.get();

        if (!(remainsBlock instanceof Remains)) {
            return false;
        }


        Direction attachDirection = findValidAttachment(level, pos, random);
        if (attachDirection == null) {
            return false;
        }

        BlockState remainsState = remainsBlock.defaultBlockState()
                .setValue(Remains.FACING, attachDirection);

        if (!remainsState.canSurvive(level, pos)) {
            return false;
        }

        return level.setBlock(pos, remainsState, 3);
    }

    /**
     * 寻找有效的附着方向
     */
    private static Direction findValidAttachment(Level level, BlockPos pos, RandomSource random) {
        List<Direction> validDirections = new ArrayList<>();

        // 检查所有可能的方向
        for (Direction direction : Direction.values()) {
            BlockPos attachedPos = pos.relative(direction.getOpposite());
            BlockState attachedState = level.getBlockState(attachedPos);

            // 检查附着面是否坚固
            if (attachedState.isFaceSturdy(level, attachedPos, direction)) {
                validDirections.add(direction);
            }
        }

        // 如果没有找到有效方向，返回null
        if (validDirections.isEmpty()) {
            return null;
        }

        // 随机选择一个有效方向
        return validDirections.get(random.nextInt(validDirections.size()));
    }

    /**
     * 增强版本：带有配置参数的hostRip方法
     */
    public static void hostRip(Entity entity, int radius, int count, boolean prioritizeVertical, float largeChance, float mediumChance) {
        // 这里可以添加更多配置选项的实现
        // 目前调用基础版本
        hostRip(entity, radius, count);
    }
}