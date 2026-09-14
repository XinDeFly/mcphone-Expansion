package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.block.PublicMarketBlock;
import cn.blockforge.generated.generatedmod.block.TradingPlatformBlock;
import cn.blockforge.generated.generatedmod.blockentity.PublicMarketBlockEntity;
import cn.blockforge.generated.generatedmod.blockentity.TradingPlatformBlockEntity;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "generated_mod";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<Item> DOLLAR = ITEMS.register("dollar", () -> new Item(new Item.Properties()));
    /** 存储元件道具 tag：凡带有此 tag 的物品放入机箱存储元件槽后，每个增加 9 格（一排）存储容量。 */
    public static final net.minecraft.tags.TagKey<Item> STORAGE_ELEMENTS_TAG =
            net.minecraft.tags.ItemTags.create(new ResourceLocation(MOD_ID, "storage_elements"));
    public static final RegistryObject<Block> TRADING_PLATFORM = BLOCKS.register("trading_platform", () -> new TradingPlatformBlock(Block.Properties.of().strength(3.5f).noOcclusion()));
    public static final RegistryObject<Block> PUBLIC_MARKET = BLOCKS.register("public_market", () -> new PublicMarketBlock(Block.Properties.of().strength(3.5f).noOcclusion()));
    public static final RegistryObject<Item> TRADING_PLATFORM_ITEM = ITEMS.register("trading_platform", () -> new BlockItem(TRADING_PLATFORM.get(), new Item.Properties()));
    public static final RegistryObject<Item> PUBLIC_MARKET_ITEM = ITEMS.register("public_market", () -> new BlockItem(PUBLIC_MARKET.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PublicMarketBlockEntity>> PUBLIC_MARKET_BE = BLOCK_ENTITIES.register("public_market", () -> BlockEntityType.Builder.of(PublicMarketBlockEntity::new, PUBLIC_MARKET.get()).build(null));
    public static final RegistryObject<BlockEntityType<TradingPlatformBlockEntity>> TRADING_PLATFORM_BE = BLOCK_ENTITIES.register("trading_platform", () -> BlockEntityType.Builder.of(TradingPlatformBlockEntity::new, TRADING_PLATFORM.get()).build(null));
public static final RegistryObject<net.minecraft.world.inventory.MenuType<MarketMenu>> MARKET_MENU = MENUS.register("market_menu",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create((id, inventory, buf) -> {
                net.minecraft.core.BlockPos pos = buf.readBlockPos();
                int mode = buf.readByte();
                int brokerId = mode == 6 ? buf.readVarInt() : -1;
                MarketMenu menu = new MarketMenu(id, inventory, pos, mode);
                menu.brokerId = brokerId;
                return menu;
            }));

    @SubscribeEvent
    public static void addMcphoneTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(new ResourceLocation("mcphone", "main"))) {
            event.accept(TRADING_PLATFORM_ITEM);
            event.accept(PUBLIC_MARKET_ITEM);
            event.accept(DOLLAR);
            event.accept(cn.blockforge.generated.mod3ce985ee.GeneratedMod.COMPUTER_MONITOR_ITEM);
            event.accept(cn.blockforge.generated.mod3ce985ee.GeneratedMod.COMPUTER_TOWER_ITEM);
            event.accept(cn.blockforge.generated.mod3ce985ee.GeneratedMod.MEMORY_STICK);
        }
    }

    public GeneratedMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Network.register();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        // 做市商村民：职业 + 工作站点（工作站点不注册物品，无法被玩家放置诱导转职）
        cn.blockforge.generated.generatedmod.broker.BrokerProfession.init(bus);
        bus.addListener(GeneratedMod::addMcphoneTabItems);
        cn.blockforge.generated.mod3ce985ee.GeneratedMod.init(bus);
    }
}
