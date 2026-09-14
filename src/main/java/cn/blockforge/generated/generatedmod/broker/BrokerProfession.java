package cn.blockforge.generated.generatedmod.broker;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 做市商村民的职业注册。
 *
 * <p><b>关键设计：工作站点方块「券商柜台」故意不注册物品形态</b> —— 世界里无法放置该方块，
 * 因此玩家<b>无法</b>通过摆放工作方块诱导村民转职；该职业只能由
 * {@link BrokerSpawn} 在村民生成时直接指定。</p>
 *
 * <p>由于工作站点在世界上不存在，该村民也不会走原版补货/升级流程（正合需求）；
 * 其报价完全由本模组的做市引擎按每日行情驱动。</p>
 */
public final class BrokerProfession {

    /** 做市商职业的注册名（完整 ID 为 {@code generated_mod:broker}）。 */
    public static final String NAME = "broker";

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedMod.MOD_ID);
    private static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, GeneratedMod.MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, GeneratedMod.MOD_ID);

    /**
     * 券商柜台（工作站点方块）：<b>不注册对应物品</b>，生存与创造都无法获得/放置。
     *
     * <p>方块本身仍需注册 —— 原版职业体系要求职业绑定一个 POI 类型，
     * 而 POI 类型必须绑定方块状态。</p>
     */
    public static final RegistryObject<Block> BROKER_DESK = BLOCKS.register("broker_desk",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD)));

    /** 券商柜台对应的 POI 类型（世界中不会自然存在）。 */
    public static final RegistryObject<PoiType> BROKER_POI = POI_TYPES.register(NAME,
            () -> new PoiType(java.util.Set.copyOf(BROKER_DESK.get().getStateDefinition().getPossibleStates()), 1, 1));

    /** 做市商职业（无原版交易列表，交易由本模组的做市界面处理）。 */
    public static final RegistryObject<VillagerProfession> BROKER = PROFESSIONS.register(NAME,
            () -> new VillagerProfession(NAME,
                    holder -> holder.is(BROKER_POI.getKey()),
                    holder -> holder.is(BROKER_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_LIBRARIAN));

    private BrokerProfession() {
    }

    /** 由 {@link GeneratedMod} 在构造时调用，注册全部相关对象。 */
    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
