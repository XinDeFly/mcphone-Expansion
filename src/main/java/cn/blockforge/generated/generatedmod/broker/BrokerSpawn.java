package cn.blockforge.generated.generatedmod.broker;

import cn.blockforge.generated.generatedmod.api.economy.Money;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * 做市商村民的生成规则与每日结算。
 *
 * <p>三条约束（按需求确定）：</p>
 * <ol>
 *   <li><b>不能被玩家诱导转职</b> —— 由 {@link BrokerProfession} 的工作站点无物品形态保证；</li>
 *   <li><b>少量随机生成在村庄</b> —— 仅当村民加入世界、成年、无职业、且所在村庄达到规模门槛时，
 *       按 {@link #BASE_CHANCE} 概率转化；</li>
 *   <li><b>每村上限 2，且 80% 的村庄只刷 1 只</b> —— 上限由村庄中心坐标哈希决定（同一村庄恒定），
 *       村庄规模过小则不生成。</li>
 * </ol>
 */
public final class BrokerSpawn {

    /** 单个村民转化为做市商的概率。 */
    public static final double BASE_CHANCE = 0.08;
    /** 村庄判定半径（方块）。 */
    public static final int VILLAGE_RADIUS = 48;
    /** 村庄规模下限：附近村民数（低于此值视为小村庄，不生成做市商）。 */
    public static final int MIN_VILLAGERS = 4;
    /** 每村上限（受 80% 规则进一步限制为 1）。 */
    public static final int MAX_PER_VILLAGE = 2;

    private BrokerSpawn() {
    }

    /** 该村民是否为做市商。 */
    public static boolean isBroker(Villager villager) {
        return BrokerData.isBroker(villager);
    }

    /** 村民加入世界时的转化判定。 */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Villager villager) || villager.isBaby() || isBroker(villager)) {
            return;
        }
        if (villager.getVillagerData().getProfession() != VillagerProfession.NONE) {
            return;
        }
        if (villager.getRandom().nextDouble() >= BASE_CHANCE) {
            return;
        }
        BlockPos center = villager.blockPosition();
        int cap = villageCap(center);
        if (countBrokers(level, center) >= cap) {
            return;
        }
        if (countVillagers(level, center) < MIN_VILLAGERS) {
            return;
        }
        convert(villager);
    }

    /** 该村庄的做市商上限：80% 的村庄为 1，20% 为 2（由村庄位置哈希决定，同一村庄恒定）。 */
    public static int villageCap(BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return Math.floorMod(chunk.hashCode(), 10) < 8 ? 1 : MAX_PER_VILLAGE;
    }

    /** 附近做市商数量。 */
    public static int countBrokers(ServerLevel level, BlockPos center) {
        int count = 0;
        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(center).inflate(VILLAGE_RADIUS))) {
            if (isBroker(villager)) {
                count++;
            }
        }
        return count;
    }

    /** 附近村民数量：作为村庄规模判据（低于 {@link #MIN_VILLAGERS} 视为小村庄，不生成做市商）。 */
    public static int countVillagers(ServerLevel level, BlockPos center) {
        return level.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(center).inflate(VILLAGE_RADIUS)).size();
    }

    /** 把该村民转化为做市商：设定职业、初始化独立钱包、刷新名字牌。 */
    public static void convert(Villager villager) {
        villager.setVillagerData(villager.getVillagerData()
                .setProfession(BrokerProfession.BROKER.get())
                .setLevel(1));
        BrokerData data = BrokerData.of(villager);
        BrokerName.refresh(villager, data);
        villager.setPersistenceRequired();
    }

    /**
     * 为该维度内所有已加载的做市商村民做每日结算（记录当日盈亏并刷新名字牌）。
     *
     * @return 结算的做市商数量
     */
    public static int settleAll(ServerLevel level, long day) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Villager villager && isBroker(villager)) {
                BrokerData data = BrokerData.of(villager);
                if (data.lastDay != day) {
                    data.settle(villager, level, day);
                    count++;
                }
            }
        }
        return count;
    }

    /** 调试/指令用：读取做市商钱包文本。 */
    public static String walletText(Villager villager) {
        return Money.format(BrokerData.of(villager).wallet);
    }
}
