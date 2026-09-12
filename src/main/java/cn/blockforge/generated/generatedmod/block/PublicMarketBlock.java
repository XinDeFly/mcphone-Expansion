package cn.blockforge.generated.generatedmod.block;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.MarketServerActions;
import cn.blockforge.generated.generatedmod.blockentity.PublicMarketBlockEntity;
import cn.blockforge.generated.generatedmod.data.MarketData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class PublicMarketBlock extends BaseEntityBlock {
    public PublicMarketBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PublicMarketBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PublicMarketBlockEntity be
                && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> be.createMenu(id, inv, p), be.getDisplayName()),
                    buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeByte(1);
                    });
            MarketServerActions.sendSnapshot(serverPlayer, MarketData.get(serverLevel), serverLevel, "");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
