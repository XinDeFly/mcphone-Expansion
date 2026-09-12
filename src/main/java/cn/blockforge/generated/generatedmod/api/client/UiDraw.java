package cn.blockforge.generated.generatedmod.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * 可复用的界面绘制工具集：统一配色常量与常用控件绘制。
 *
 * <p>提供 1px 描边底框、描边按钮、选项行、槽位边框、背包/快捷栏分隔线与颜色插值，
 * 供本模组任意容器界面（现货/期货/机箱/显示器/手机等）共用，也可被其它模组直接调用。</p>
 */
public final class UiDraw {

    /** 面板底色（滚动淡出遮罩等需与其保持一致）。 */
    public static final int PANEL_BG = 0xFF101827;
    /** 深色面板底色。 */
    public static final int PANEL_BG_DARK = 0xFF0F1624;
    /** 控件描边。 */
    public static final int BORDER = 0xFF3B526F;
    /** 控件底色。 */
    public static final int CONTROL_BG = 0xFF16212D;
    /** 悬停底色。 */
    public static final int HOVER_BG = 0xFF2A4358;
    /** 选中底色。 */
    public static final int SELECT_BG = 0xFF1E3A50;
    /** 主要文字。 */
    public static final int TEXT = 0xFFE5E9EE;
    /** 白色文字。 */
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    /** 次要文字。 */
    public static final int TEXT_DIM = 0xFF8FA3BF;
    /** 金色（余额/强调）。 */
    public static final int GOLD = 0xFFFFD86B;

    private UiDraw() {
    }

    /** 命中测试（相对坐标）。 */
    public static boolean hit(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** 颜色线性插值（t 为 0~1）。 */
    public static int lerpColor(int from, int to, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * k);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * k);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * k);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** 带 1px 描边的底框（悬停/选中可指定底色）。 */
    public static void borderedBox(GuiGraphics graphics, int x, int y, int w, int h, int fill, int border) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, border);
        graphics.fill(x, y + h - 1, x + w, y + h, border);
        graphics.fill(x, y, x + 1, y + h, border);
        graphics.fill(x + w - 1, y, x + w, y + h, border);
    }

    /** 标准描边控件（悬停高亮）。 */
    public static void borderedBox(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered) {
        borderedBox(graphics, x, y, w, h, hovered ? HOVER_BG : CONTROL_BG, BORDER);
    }

    /** 描边按钮：可指定选中态（选中时使用选中底色 + 白色文字）。 */
    public static void button(GuiGraphics graphics, Font font, String label,
                              int x, int y, int w, int h, boolean hovered, boolean selected) {
        borderedBox(graphics, x, y, w, h, selected ? SELECT_BG : (hovered ? HOVER_BG : CONTROL_BG), BORDER);
        graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2,
                selected ? TEXT_WHITE : TEXT, false);
    }

    /** 单格槽位 1px 描边。 */
    public static void slotFrame(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 1, y - 1, x + 17, y, color);
        graphics.fill(x - 1, y + 16, x + 17, y + 17, color);
        graphics.fill(x - 1, y, x, y + 16, color);
        graphics.fill(x + 16, y, x + 17, y + 17, color);
    }

    /** 为容器槽位列表绘制 1px 描边（fromIndex 起，用于跳过特殊槽位）。 */
    public static void slotFrames(GuiGraphics graphics, int leftPos, int topPos,
                                  List<Slot> slots, int fromIndex, int color) {
        for (int i = Math.max(0, fromIndex); i < slots.size(); i++) {
            Slot slot = slots.get(i);
            slotFrame(graphics, leftPos + slot.x, topPos + slot.y, color);
        }
    }

    /**
     * 背包与快捷栏之间的分隔线（与玩家正常打开背包时一致的明显区分）。
     *
     * <p>约定：槽位列表末尾 9 格为快捷栏，其前 27 格为背包三行；两者之间绘制一条分隔线。</p>
     */
    public static void hotbarDivider(GuiGraphics graphics, int leftPos, int topPos,
                                     List<Slot> slots, int color) {
        int n = slots.size();
        if (n < 36) {
            return;
        }
        int hotbarTop = Integer.MAX_VALUE;
        int invBottom = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int i = n - 9; i < n; i++) {
            Slot slot = slots.get(i);
            hotbarTop = Math.min(hotbarTop, slot.y);
            minX = Math.min(minX, slot.x);
            maxX = Math.max(maxX, slot.x + 16);
        }
        for (int i = n - 36; i < n - 9; i++) {
            Slot slot = slots.get(i);
            invBottom = Math.max(invBottom, slot.y + 16);
        }
        if (hotbarTop > invBottom && minX <= maxX) {
            int y = topPos + (invBottom + hotbarTop) / 2;
            graphics.fill(leftPos + minX, y, leftPos + maxX, y + 1, color);
        }
    }
}
