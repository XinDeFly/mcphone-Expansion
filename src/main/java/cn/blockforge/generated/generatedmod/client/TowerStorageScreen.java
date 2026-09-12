package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.api.client.ScaledDeviceScreen;
import cn.blockforge.generated.generatedmod.api.client.UiDraw;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity;
import cn.blockforge.generated.mod3ce985ee.ComputerTowerElementLayout;
import cn.blockforge.generated.mod3ce985ee.client.ComputerLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 机箱存储元件界面（主板式，铺满机箱屏幕）：
 * 中线以上为大片电脑主板区域——深色 PCB 底，CPU 座（金属散热盖 + 蚀刻文字）居中，
 * 左右各 4 条服务器级内存插槽（槽底与电源面板顶部保持 25px 间隔）；
 * 中线以下为光泽金属灰白面板，显示玩家背包/物品栏 9×4。
 * 内容按基准尺寸绘制，由 {@link ScaledDeviceScreen} 统一缩放——
 * 边框、背景、槽位框、物品图标随「视频设置 → 界面尺寸」一同缩放，交互命中共用同一变换。
 */
public final class TowerStorageScreen extends ScaledDeviceScreen<MarketMenu> {

    /** 主板贴图（1920×920 绘制后等比缩小为 768×367；宽高比与主板区域一致，走线不被拉伸）。 */
    private static final ResourceLocation BOARD_TEX =
            new ResourceLocation("mod_3ce985ee", "textures/gui/tower_motherboard.png");
    private static final int TEX_W = 768;
    private static final int TEX_H = 367;
    /** 底部金属面板贴图（参考 ROG 雷神 III 带 LOGO 一侧：拉丝铝 + 黑金凹槽 + ROG 之眼，1920×1080 绘制后拉伸）。 */
    private static final ResourceLocation PANEL_TEX =
            new ResourceLocation("mod_3ce985ee", "textures/gui/tower_metal_panel.png");
    private static final int PANEL_TEX_W = 1920;
    private static final int PANEL_TEX_H = 1080;
    /** CPU 贴图（以 AMD 线程撕裂者 PRO 9995WX 为原型，横向加宽，1080P+ 绘制后缩小）。 */
    private static final ResourceLocation CPU_TEX =
            new ResourceLocation("mod_3ce985ee", "textures/gui/tower_cpu.png");
    private static final int CPU_TEX_W = 1280;
    private static final int CPU_TEX_H = 920;

    private ComputerTowerElementLayout.Layout layout;

    public TowerStorageScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("机箱存储元件"));
    }

    @Override
    protected void init() {
        super.init();
        // 设备几何 + 内容缩放：内容按机箱屏幕基准尺寸布局，渲染时整体缩放
        initDevice(false, ComputerLayout.TOWER_CONTENT_W, ComputerLayout.TOWER_CONTENT_H);
        this.layout = ComputerTowerElementLayout.of(contentWidth(), contentHeight());
    }

    @Override
    protected void renderDeviceCasing(GuiGraphics graphics) {
        // 机箱外壳（真实坐标，不随内容缩放；与开机动画共用外壳几何）
        ComputerLayout.drawCasing(graphics, device(), false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 窗口底色（内容坐标，随内容一同缩放）
        graphics.fill(this.leftPos, this.topPos,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xff0f1624);

        drawMotherboard(graphics);
        drawMetallicPanel(graphics);
        // 槽位描边与背包/快捷栏分隔线（复用公共控件 UiDraw）
        UiDraw.slotFrames(graphics, this.leftPos, this.topPos, this.menu.slots,
                ComputerTowerBlockEntity.ELEMENT_SLOTS, 0xFF5A6470);
        UiDraw.hotbarDivider(graphics, this.leftPos, this.topPos, this.menu.slots, 0xFF6B7684);
    }

    /** 中线以上：主板贴图（含微小芯片/电容/电感/电阻与走线）+ CPU 插座 + 内存插槽。 */
    private void drawMotherboard(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;
        ComputerTowerElementLayout.Layout l = this.layout;
        int bw = this.imageWidth - 12;
        int bh = l.boardBottom - l.boardTop;
        // 主板贴图（11 参 blit：目标尺寸 + 完整采样区域，按比例铺满主板区域）
        g.blit(BOARD_TEX, x + 6, y + l.boardTop, bw, bh, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);
        // 板面描边
        g.fill(x + 6, y + l.boardTop, x + 6 + bw, y + l.boardTop + 1, 0xFF0B1F19);
        g.fill(x + 6, y + l.boardBottom - 1, x + 6 + bw, y + l.boardBottom, 0xFF0B1F19);
        g.fill(x + 6, y + l.boardTop, x + 7, y + l.boardBottom, 0xFF0B1F19);
        g.fill(x + 5 + bw, y + l.boardTop, x + 6 + bw, y + l.boardBottom, 0xFF0B1F19);

        // CPU 插座基底（深色金属框）+ CPU 高清贴图（线程撕裂者 PRO 9995WX 原型，横向加宽）
        g.fill(x + l.cpuX, y + l.cpuY, x + l.cpuX + l.cpuW, y + l.cpuY + l.cpuH, 0xFF23282E);
        g.fill(x + l.cpuX, y + l.cpuY, x + l.cpuX + l.cpuW, y + l.cpuY + 1, 0xFF4A5260);
        g.blit(CPU_TEX, x + l.cpuX + 1, y + l.cpuY + 1, l.cpuW - 2, l.cpuH - 2,
                0.0F, 0.0F, CPU_TEX_W, CPU_TEX_H, CPU_TEX_W, CPU_TEX_H);

        // 内存插槽：CPU 左右各 4 条（服务器级 8 槽），黑色细长 DIMM 槽体 + 槽内两侧金色针脚
        // 槽体高度取布局常量（与菜单槽位、25px 面板间距同源，勿另行硬编码）
        int above = ComputerTowerElementLayout.DIMM_ABOVE;
        int below = ComputerTowerElementLayout.DIMM_BELOW;
        for (int i = 0; i < ComputerTowerBlockEntity.ELEMENT_SLOTS; i++) {
            int bx = x + l.elementSlotX(i);
            int by = y + l.ramY;
            // 黑色槽体（宽 10 × 高 96）
            g.fill(bx + 3, by - above, bx + 13, by + below, 0xFF0C0C0E);
            g.fill(bx + 3, by - above, bx + 4, by + below, 0xFF3A3F46);
            g.fill(bx + 12, by - above, bx + 13, by + below, 0xFF3A3F46);
            g.fill(bx + 3, by - above, bx + 13, by - above + 1, 0xFF3A3F46);
            g.fill(bx + 3, by + below - 1, bx + 13, by + below, 0xFF3A3F46);
            // 金色针脚：沿槽内两侧竖直排布
            g.fill(bx + 4, by - above + 4, bx + 5, by + below - 4, 0xFFD8B23A);
            g.fill(bx + 11, by - above + 4, bx + 12, by + below - 4, 0xFFD8B23A);
            g.fill(bx + 4, by - above + 4, bx + 5, by - above + 6, 0xFFF2D06A);
            g.fill(bx + 11, by - above + 4, bx + 12, by - above + 6, 0xFFF2D06A);
        }
    }

    /** 中线以下：金属面板（ROG 雷神 III 风格贴图，拉伸铺满）。 */
    private void drawMetallicPanel(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;
        ComputerTowerElementLayout.Layout l = this.layout;
        // 中线分隔
        g.fill(x + 6, y + l.panelTop - 4, x + this.imageWidth - 6, y + l.panelTop - 2, 0xFF0B1F19);
        // 金属面板贴图（11 参 blit：拉伸完整 1920×1080 贴图至面板区域）
        int pw = this.imageWidth - 12;
        int ph = this.imageHeight - 8 - l.panelTop;
        g.blit(PANEL_TEX, x + 6, y + l.panelTop, pw, ph, 0.0F, 0.0F,
                PANEL_TEX_W, PANEL_TEX_H, PANEL_TEX_W, PANEL_TEX_H);
        // 面板描边
        g.fill(x + 6, y + l.panelTop, x + this.imageWidth - 6, y + l.panelTop + 1, 0xFFE8EBEF);
        g.fill(x + 6, y + l.panelTop, x + 7, y + this.imageHeight - 8, 0xFF8A93A0);
        g.fill(x + this.imageWidth - 7, y + l.panelTop, x + this.imageWidth - 6, y + this.imageHeight - 8, 0xFF8A93A0);
        g.fill(x + 6, y + this.imageHeight - 9, x + this.imageWidth - 6, y + this.imageHeight - 8, 0xFF8A93A0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 注意：父类已对 pose 执行 translate(leftPos, topPos)，此处使用面板相对坐标。
        ComputerTowerElementLayout.Layout l = this.layout;
        graphics.drawString(this.font, this.title, 10, 6, 0xffffffff);
        // 提示文字放在顶部标题栏右侧（避免与绿色电路板重叠）
        String hint = "内存条：每个 +9 格";
        graphics.drawString(this.font, hint,
                this.imageWidth - 12 - this.font.width(hint), 6, 0xFFB8C7DF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
