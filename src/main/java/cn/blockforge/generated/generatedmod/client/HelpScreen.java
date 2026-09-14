package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.api.client.ScrollableText;
import cn.blockforge.generated.generatedmod.api.client.UiDraw;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class HelpScreen extends AbstractContainerScreen<MarketMenu> {
    /** 顶部醒目警示（图标由 UiDraw 自绘，不依赖字体中的特殊符号）。 */
    public static final String WARNING = "徒手可挖掉方块，但方块内已存入的物品不会掉落！";

    /** 帮助条目（供桌面帮助页与手机应用的使用说明页共用）。 */
    public static final List<String> HELP_LINES = List.of(
            "欢迎使用世界金融中心",
            "",
            "【防丢物品】机箱、显示器、交易平台等方块都可以徒手挖掉并返还自身，但方块内已经存入的物品（含内存条、现货存储与机箱货物）不会掉落——挖掉前请先把里面的东西全部取出。",
            "",
            "【钱包】初始余额为 0；卖出物品即可获得资金，钱包随存档持久保存。金额以「分」精算，界面统一显示两位小数。",
            "【启动资金】推荐把世界里获得的资源（石头、矿石、圆木等）放到现货市场出售，快速积累启动资金。",
            "【股票】代表物品所有权，可长期持有；当日买入次日才能卖出（T+1）；买入收佣金、卖出收印花税，每日按持仓派发分红。",
            "【期货】支持做多/做空双向开仓，开仓只需 10% 保证金；每日盯市结算盈亏，3 个游戏日后到期自动平仓，亏损过大会被强制平仓。期货价格没有地板限制，高位波动下可能跌到负数（与现实市场一致），做多方会因此赔掉保证金以外的资金。",
            "【现货】买入直接扣余额并把物品放进背包；卖出从独立存储区、背包和物品栏中扣除物品，并按物品稀有度收取 5.0%~15.0% 的手续费（越稀有越高）。",
            "【现货存储】现货界面右侧为 27 格独立存储，点击拾取/放置、Shift 快速移动，物品随存档保存，重开不丢失。",
            "【机箱】动态容量存储：基础 9 格（一排）；点击机箱打开存储元件界面，CPU 左右各 4 个内存插槽（共 8 个）可放置「内存条」（每个 +9 格，最多 81 格）；内存条可由图书管理员以 3 金锭兑换或按配方合成；漏斗/管道等可直接向机箱传输物品，货物存储空间在显示器控制台中操作。",
            "【显示器】紧邻机箱时点击可打开控制台：选择四周机箱并查看其存储空间、打开其存储元件槽，设置角色、红石模式与每物品自动规则（阈值/数量/触发时机），资金流走机箱绑定玩家（放置者）钱包。",
            "【搜索】支持中文名称和英文物品 ID 实时检索。",
            "【数量】最大=单格堆叠上限，最小=1，已拥有=存储区+背包+物品栏中的物品数。",
            "【行情】股票与现货共用同一套价格、涨跌幅和 15 日 K 线；期货拥有独立的价格与走势。",
            "【大跌】连涨后的大跌幅度：股票/现货为该档最大跌幅的 0.6~0.95 倍，期货为 0.5~1.5 倍。",
            "【稀有度】物品按 7 级稀有度定价（普通/稀有/罕见/史诗/传说/神话/唯一）：等级越高，基准价区间、日涨跌幅与卖出手续费都越大；已安装「Rarity Core」时主动采用它的评级，未安装时使用本模组内置的稀有度快照（1262 条），快照中也未收录的物品会按获取难度自动归入前 4 级。",
            "【手机】点击带「横」标签的应用，手机旋转至横屏即可交易；右侧功能键：返回=上一层、主屏=回桌面、任务=预留。",
            "【时间】界面右上角显示游戏时间与距下一次行情更新的剩余时间。"
    );

    /** 帮助正文（复用公共滚动文本控件：可滚动、滑条、底部淡出）。 */
    private final ScrollableText text = new ScrollableText();

    public HelpScreen(MarketMenu menu, Inventory inventory) {
        super(menu, inventory, Component.literal("帮助与提示"));
        this.imageWidth = 320;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        this.text.setLines(HELP_LINES, this.font, this.imageWidth - 32);
        this.addRenderableWidget(Button.builder(Component.literal("返回"), b -> back())
                .bounds(this.leftPos + 216, this.topPos + 210, 96, 22).build());
    }

    private void back() {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (this.minecraft.player.containerMenu == this.menu) {
                this.minecraft.setScreen(new MarketScreen(this.menu, this.menu.playerInventory, Component.literal("世界金融中心")));
            } else {
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void onClose() {
        back();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xff0a0e17);
        graphics.fill(this.leftPos + 6, this.topPos + 4, this.leftPos + this.imageWidth - 6, this.topPos + 32, 0xff111827);
        graphics.fill(this.leftPos + 6, this.topPos + 36, this.leftPos + this.imageWidth - 6, this.topPos + this.imageHeight - 6, 0xff0e1524);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 注意：父类已对 pose 执行 translate(leftPos, topPos)，此处使用面板相对坐标。
        graphics.drawString(this.font, this.title, 10, 8, 0xffffffff);
        // 顶部醒目警示条（常驻显示，不随帮助正文滚动）。
        UiDraw.warningBanner(graphics, this.font, WARNING, 10, 36, this.imageWidth - 20);
        this.text.render(graphics, this.font, 10, 55, this.imageWidth - 20, 149, 16, 0xFF0E1524);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.text.mouseScrolled(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.text.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.text.mouseDragged(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.text.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
