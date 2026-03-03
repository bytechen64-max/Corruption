package corruption.block.custom.baseBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Remains extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // 修正后的碰撞箱定义 - 确保每个方向都正确对应
    private static final VoxelShape SHAPE_DOWN = Block.box(0.8D, 12.0D, 0.8D, 15.2D, 16.0D, 15.2D);
    private static final VoxelShape SHAPE_UP = Block.box(0.8D, 0.0D, 0.8D, 15.2D, 4.0D, 15.2D);
    private static final VoxelShape SHAPE_NORTH = Block.box(0.8D, 0.8D, 12.0D, 15.2D, 15.2D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.8D, 0.8D, 0.0D, 15.2D, 15.2D, 4.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(12.0D, 0.8D, 0.8D, 16.0D, 15.2D, 15.2D);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 0.8D, 0.8D, 4.0D, 15.2D, 15.2D);
    public Remains(Properties properties) {
        super(Properties.of()
                //.offsetType(OffsetType.XZ)
                .sound(SoundType.GRASS)
                .noOcclusion()
        );

        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);

    }

    // 添加放置逻辑
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 获取玩家点击的面，并设置方块朝向
        Direction direction = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        LevelReader level = context.getLevel();

        // 检查是否可以放置在这个位置
        BlockState state = this.defaultBlockState().setValue(FACING, direction);
        if (state.canSurvive(level, pos)) {
            return state;
        }

        return null; // 无法放置
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }


    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // 玩家可以穿过
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(direction.getOpposite());
        BlockState attachedState = world.getBlockState(attachedPos);

        return attachedState.isFaceSturdy(world, attachedPos, direction);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }


    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
}