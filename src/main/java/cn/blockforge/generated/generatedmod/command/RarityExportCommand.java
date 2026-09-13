package cn.blockforge.generated.generatedmod.command;

import cn.blockforge.generated.generatedmod.api.rarity.RaritySources;
import cn.blockforge.generated.generatedmod.api.rarity.RarityTier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 稀有度快照命令：把当前游戏里所有物品的稀有度等级导出为快照文件。
 *
 * <ul>
 *   <li>{@code /mcme rarity info} —— 查看当前数据来源与快照条目数</li>
 *   <li>{@code /mcme rarity export} —— 导出快照（JSON + CSV）到 {@code config/generated_mod/}</li>
 * </ul>
 *
 * <p>安装「Rarity Core」时导出的是其评级结果（与游戏内显示一致）；
 * 未安装时导出的是本模组当前生效的数据（内置快照 + 原版稀有度映射）。
 * 导出的 JSON 可直接替换模组内置的 {@code data/generated_mod/rarity_snapshot.json}。</p>
 */
public final class RarityExportCommand {

    /** 命令根名称。 */
    public static final String ROOT = "mcme";

    private RarityExportCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .then(Commands.literal("rarity")
                        .then(Commands.literal("info")
                                .executes(RarityExportCommand::info))
                        .then(Commands.literal("export")
                                .requires(source -> source.hasPermission(2))
                                .executes(RarityExportCommand::export))));
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[市场扩展] 稀有度数据来源：§f" + RaritySources.activeSource()), false);
        source.sendSuccess(() -> Component.literal("§6[市场扩展] 内置快照条目：§f" + RaritySources.snapshotEntries().size()
                + " §7（导出命令可刷新）"), false);
        return 1;
    }

    private static int export(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("generated_mod");
            Files.createDirectories(dir);
            Path jsonPath = dir.resolve("rarity_snapshot.json");
            Path csvPath = dir.resolve("rarity_snapshot.csv");

            Map<String, Integer> levels = collect();
            writeJson(jsonPath, levels);
            writeCsv(csvPath, levels);

            int total = levels.size();
            source.sendSuccess(() -> Component.literal("§a[市场扩展] 已导出 §f" + total
                    + " §a条稀有度快照：§f" + jsonPath), true);
            source.sendSuccess(() -> Component.literal("§7（CSV 便于阅读：§f" + csvPath + "§7）"), false);
            source.sendSuccess(() -> Component.literal("§7数据来源：§f" + RaritySources.activeSource()), false);
            return total;
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c[市场扩展] 导出失败：" + e.getMessage()));
            return 0;
        }
    }

    /** 遍历全部已注册物品，逐项解析稀有度（使用 RaritySources，因此自动优先采用 Rarity Core）。 */
    private static Map<String, Integer> collect() {
        Map<String, Integer> levels = new TreeMap<>();
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            items.add(item);
        }
        items.sort(Comparator.comparing(item -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            return id == null ? "" : id.toString();
        }));
        for (Item item : items) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            levels.put(id.toString(), RaritySources.level(stack));
        }
        return levels;
    }

    private static void writeJson(Path path, Map<String, Integer> levels) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"_meta\": {\n");
        sb.append("    \"source\": \"").append(escape(RaritySources.activeSource())).append("\",\n");
        sb.append("    \"coreMod\": \"").append(RaritySources.RARITYCORE_MODID).append("\",\n");
        sb.append("    \"coreInstalled\": ").append(RaritySources.coreAvailable()).append(",\n");
        sb.append("    \"levels\": \"").append(RarityTier.MIN_LEVEL).append("-").append(RarityTier.MAX_LEVEL).append("\",\n");
        sb.append("    \"entries\": ").append(levels.size()).append("\n");
        sb.append("  },\n");
        sb.append("  \"items\": {\n");
        int index = 0;
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            index++;
            sb.append("    \"").append(escape(entry.getKey())).append("\": ").append(entry.getValue());
            sb.append(index < levels.size() ? ",\n" : "\n");
        }
        sb.append("  }\n");
        sb.append("}\n");
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void writeCsv(Path path, Map<String, Integer> levels) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("item_id,level,tier\n");
            for (Map.Entry<String, Integer> entry : levels.entrySet()) {
                RarityTier tier = RarityTier.of(entry.getValue());
                writer.write(entry.getKey() + "," + entry.getValue() + "," + tier.displayName() + "\n");
            }
        }
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
