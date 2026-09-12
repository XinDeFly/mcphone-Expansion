package cn.blockforge.generated.mod3ce985ee;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GeneratedMod {
    public static final String MOD_ID = "mod_3ce985ee";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Block> COMPUTER_MONITOR = BLOCKS.register("computer_monitor",
            () -> new ComputerBlock(true));
    public static final RegistryObject<Block> COMPUTER_TOWER = BLOCKS.register("computer_tower",
            () -> new ComputerBlock(false));
    public static final RegistryObject<Item> COMPUTER_MONITOR_ITEM = ITEMS.register("computer_monitor",
            () -> new BlockItem(COMPUTER_MONITOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> COMPUTER_TOWER_ITEM = ITEMS.register("computer_tower",
            () -> new BlockItem(COMPUTER_TOWER.get(), new Item.Properties()));
    /** 内存条：机箱存储元件道具，放入机箱左侧存储元件槽后每个增加 9 格（一排）存储容量。 */
    public static final RegistryObject<Item> MEMORY_STICK = ITEMS.register("memory_stick",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<BlockEntityType<ComputerTowerBlockEntity>> COMPUTER_TOWER_BE = BLOCK_ENTITIES.register("computer_tower",
            () -> BlockEntityType.Builder.of(ComputerTowerBlockEntity::new, COMPUTER_TOWER.get()).build(null));

    private GeneratedMod() {
    }

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
