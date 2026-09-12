package cn.blockforge.generated.generatedmod.api.client.phone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.november.mcphone.core.client.PhoneChassis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Rectangle;

/**
 * 横屏手机底盘：可重复使用的公共控件。
 *
 * <p>封装了横屏手机的全部通用部分——旋转动画（1.2s）、手机边框与内屏（MCphone 原版贴图）、
 * 右侧功能条（原版控件放大 + 蒙版条 + 悬停提亮）以及内容区几何。
 * 页面通过 {@link #contentX()}、{@link #contentY()}、{@link #contentW()}、{@link #contentH()}
 * 在其内绘制，通过 {@link #navTasks()} / {@link #navHome()} / {@link #navBack()}
 * 获得三个功能键的屏幕空间矩形。</p>
 */
public final class LandscapePhoneChassis {

    /** 手机屏幕尺寸（横屏旋转后）。 */
    public static final int PHONE_W = 380;
    public static final int PHONE_H = 240;

    /** 旋转动画时长（毫秒）。 */
    public static final long ROTATE_MS = 1200L;

    /** 原版导航条 120×14 放大到边框可视内区等长（208）：描边随比例同步加粗。 */
    private static final float NAV_SCALE = 208.0F / 120.0F;
    /** 边框贴图实测（272×432、边框 13px，拉伸 236×366）后的可视内缘，手机局部坐标 y≈169.4，取 169 留有安全边际。 */
    private static final float NAV_EDGE = 169.0F;
    private static final float NAV_THICKNESS = 14.0F * NAV_SCALE;
    private static final float NAV_INNER = NAV_EDGE - NAV_THICKNESS;
    /** 原版 drawNavBar 的入参：让原版条底边落在局部 y=0，再整体平移到 NAV_EDGE。 */
    private static final int NAV_PHONE_LEFT = -60;
    private static final int NAV_PHONE_TOP = -200;

    private final long startTime = System.currentTimeMillis();
    private int leftPos;
    private int topPos;
    private Rectangle navBack;
    private Rectangle navHome;
    private Rectangle navTasks;

    /** 由宿主屏幕在 init 时调用（leftPos/topPos 为屏幕坐标）。 */
    public void init(int leftPos, int topPos) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        // 功能条在屏幕空间的矩形：横向是条的厚度（局部 y 从 NAV_INNER 到 NAV_EDGE），
        // 纵向按原版三键比例三等分为 任务 / 主屏 / 返回，整体贴住边框内侧。
        int navX1 = leftPos + 190 + Math.round(NAV_INNER);
        int navX2 = leftPos + 190 + Math.round(NAV_EDGE);
        int half = Math.round(60.0F * NAV_SCALE);
        int navY1 = topPos + 120 - half;
        int navY2 = topPos + 120 + half;
        int third = Math.round((half * 2.0F) / 3.0F);
        this.navTasks = new Rectangle(navX1, navY1, navX2 - navX1, third);
        this.navHome = new Rectangle(navX1, navY1 + third, navX2 - navX1, third);
        this.navBack = new Rectangle(navX1, navY1 + third * 2, navX2 - navX1, navY2 - navY1 - third * 2);
    }

    /** 是否仍在旋转动画中（此时不接受页面交互）。 */
    public boolean isBusy() {
        return progress() < 1.0F;
    }

    /** 旋转进度 0..1。 */
    public float progress() {
        return Math.min(1.0F, (System.currentTimeMillis() - this.startTime) / (float) ROTATE_MS);
    }

    /** 底盘创建以来的毫秒时间（供动画使用）。 */
    public long startMillis() {
        return this.startTime;
    }

    public int leftPos() {
        return this.leftPos;
    }

    public int topPos() {
        return this.topPos;
    }

    /** 内容区左缘（屏幕坐标）。 */
    public int contentX() {
        return this.leftPos + 25;
    }

    /** 内容区上缘（屏幕坐标）。 */
    public int contentY() {
        return this.topPos + 27;
    }

    /** 内容区宽度。 */
    public int contentW() {
        return 301;
    }

    /** 内容区高度。 */
    public int contentH() {
        return 186;
    }

    /** 功能键「任务」矩形（屏幕坐标）。 */
    public Rectangle navTasks() {
        return this.navTasks;
    }

    /** 功能键「主屏」矩形（屏幕坐标）。 */
    public Rectangle navHome() {
        return this.navHome;
    }

    /** 功能键「返回」矩形（屏幕坐标）。 */
    public Rectangle navBack() {
        return this.navBack;
    }

    /**
     * 绘制手机本体：旋转动画期间绘制旋转过程；动画结束后绘制内屏背景、
     * 蒙版条、放大后的原版功能条与手机边框（边框最后绘制，始终置顶）。
     */
    public void renderPhone(GuiGraphics graphics, Font font) {
        float p = progress();
        if (p < 1.0F) {
            renderRotating(graphics, p);
            return;
        }
        int cx = this.leftPos + 190;
        int cy = this.topPos + 120;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        graphics.fill(-108, -173, 108, 173, 0xFF0C1445);
        // 蒙版条：收进边框可视内区（不压边框），与放大后的功能条同厚。
        graphics.fill(-104, Math.round(NAV_INNER), 104, Math.round(NAV_EDGE), 0xFF13193D);
        // 原版控件整体按比例放大，描边随之加粗；鼠标传哨兵值以关闭原版自带的悬停。
        pose.pushPose();
        pose.translate(0.0F, NAV_EDGE, 0.0F);
        pose.scale(NAV_SCALE, NAV_SCALE, 1.0F);
        PhoneChassis.drawNavBar(graphics, font, NAV_PHONE_LEFT, NAV_PHONE_TOP, -10000, -10000);
        pose.popPose();
        // 边框贴图最后绘制（内部区域透明），保证手机边框始终显示在最上面。
        PhoneChassis.drawFrame(graphics, -110, -175, 220, 350);
        pose.popPose();
    }

    private void renderRotating(GuiGraphics graphics, float p) {
        float angle = -p * 90.0F;
        int cx = this.leftPos + 190;
        int cy = this.topPos + 120;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.fill(-108, -173, 108, 173, 0xFF0C1445);
        PhoneChassis.drawFrame(graphics, -110, -175, 220, 350);
        pose.popPose();
    }

    /** 右侧功能条悬停提亮（屏幕空间矩形，与放大后的按键区域精确对齐）。 */
    public void renderNavHover(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Rectangle rect : new Rectangle[]{this.navTasks, this.navHome, this.navBack}) {
            if (rect != null && rect.contains(mouseX, mouseY)) {
                graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, 0x33FFFFFF);
            }
        }
    }

    /**
     * 功能键命中检测。
     *
     * @return 0=任务 1=主屏 2=返回；未命中返回 -1
     */
    public int hitNav(int x, int y) {
        if (this.navTasks != null && this.navTasks.contains(x, y)) {
            return 0;
        }
        if (this.navHome != null && this.navHome.contains(x, y)) {
            return 1;
        }
        if (this.navBack != null && this.navBack.contains(x, y)) {
            return 2;
        }
        return -1;
    }
}
