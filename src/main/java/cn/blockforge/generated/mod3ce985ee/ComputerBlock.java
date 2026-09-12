package cn.blockforge.generated.mod3ce985ee;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 电脑方块：显示器只有连接机箱时才显示桌面；机箱为动态容量自动化存储容器（9+9×存储元件 格）。
 * 显示器点击播放开机动画后打开控制台；机箱单独交互不打开界面（存储界面经显示器打开）。
 * 放置时正面朝向玩家。
 */
public final class ComputerBlock extends BaseEntityBlock {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    /** 水平朝向：正面（屏幕/机箱面板）所在方向。 */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final boolean monitor;

    public ComputerBlock(boolean monitor) {
        super(BlockBehaviour.Properties.of()
                .strength(2.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.monitor = monitor;
        registerDefaultState(stateDefinition.any()
                .setValue(CONNECTED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return monitor ? null : new ComputerTowerBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED).add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        // 正面（屏幕/面板）朝向放置玩家
        return state.setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CONNECTED,
                        monitor && hasTowerNeighbor(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!monitor) {
            return state;
        }
        boolean connected = hasTowerNeighbor(level, pos);
        return state.getValue(CONNECTED) == connected
                ? state
                : state.setValue(CONNECTED, connected);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        if (monitor) {
            refreshMonitorState(level, pos);
        } else {
            refreshAdjacentMonitors(level, pos);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        if (monitor) {
            refreshMonitorState(level, pos);
        } else {
            refreshAdjacentMonitors(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !monitor && state.getBlock() != newState.getBlock()) {
            refreshAdjacentMonitors(level, pos);
        }
    }

    private static boolean hasTowerNeighbor(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction))
                    .is(GeneratedMod.COMPUTER_TOWER.get())) {
                return true;
            }
        }
        return false;
    }

    private static void refreshAdjacentMonitors(LevelAccessor level, BlockPos center) {
        for (Direction direction : Direction.values()) {
            BlockPos monitorPos = center.relative(direction);
            if (level.getBlockState(monitorPos).is(GeneratedMod.COMPUTER_MONITOR.get())) {
                refreshMonitorState(level, monitorPos);
            }
        }
    }

    private static void refreshMonitorState(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(GeneratedMod.COMPUTER_MONITOR.get())) {
            return;
        }
        boolean connected = hasTowerNeighbor(level, pos);
        if (state.getValue(CONNECTED) != connected) {
            level.setBlock(pos, state.setValue(CONNECTED, connected),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            if (monitor) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> cn.blockforge.generated.mod3ce985ee.client.ComputerClient.openMonitor(pos));
            } else {
                // 机箱：开机动画后打开存储元件界面（放置内存条扩展容量）
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> cn.blockforge.generated.mod3ce985ee.client.ComputerClient.openTower(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
