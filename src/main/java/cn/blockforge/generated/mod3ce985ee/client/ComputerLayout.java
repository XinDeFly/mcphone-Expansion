package cn.blockforge.generated.mod3ce985ee.client;

import cn.blockforge.generated.mod3ce985ee.ComputerGeometry;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 电脑设备（显示器 / 机箱）外壳布局（**客户端专用**：负责外壳绘制与自适应缩放）：
 * 开机动画界面与功能界面（机箱存储 / 显示器控制台）共用同一套几何，
 * 保证功能内容始终显示在设备“屏幕”范围内；外壳按当前 GUI 尺寸等比例缩放，完整可见。
 *
 * <p>几何数值本身放在 {@link ComputerGeometry}（双端共用、不引用客户端类），
 * 这里保留同名别名并负责渲染；菜单侧请使用
 * {@link cn.blockforge.generated.mod3ce985ee.ComputerTowerElementLayout#contentSize()}，
 * 不要在服务端可达代码里引用本类。</p>
 */
public final class ComputerLayout {
    /** 基准尺寸（GUI 像素）与边框厚度（数值来源见 {@link ComputerGeometry}）。 */
    public static final int MONITOR_FRAME_W = ComputerGeometry.MONITOR_FRAME_W;
    public static final int MONITOR_FRAME_H = ComputerGeometry.MONITOR_FRAME_H;
    public static final int MONITOR_BEZEL = ComputerGeometry.MONITOR_BEZEL;
    public static final int TOWER_FRAME_W = ComputerGeometry.TOWER_FRAME_W;
    public static final int TOWER_FRAME_H = ComputerGeometry.TOWER_FRAME_H;
    public static final int TOWER_BEZEL = ComputerGeometry.TOWER_BEZEL;

    /**
     * 屏幕内容基准尺寸（未缩放，GUI 像素）：布局与槽位坐标一律使用该尺寸，
     * 渲染时由 {@code api.client.ScaledDeviceScreen} 统一缩放到设备屏幕实际矩形——
     * 保证「视频设置 → 界面尺寸（GUI 尺寸）」变化时，边框、背景、槽位框、物品图标、
     * 控件与交互命中共用同一缩放，不再出现外框缩放而格子不跟随的错位。
     */
    public static final int TOWER_CONTENT_W = ComputerGeometry.TOWER_CONTENT_W;
    public static final int TOWER_CONTENT_H = ComputerGeometry.TOWER_CONTENT_H;
    /** 显示器屏幕内容基准尺寸。 */
    public static final int MONITOR_CONTENT_W = ComputerGeometry.MONITOR_CONTENT_W;
    public static final int MONITOR_CONTENT_H = ComputerGeometry.MONITOR_CONTENT_H;

    private ComputerLayout() {
    }

    /** 矩形（GUI 坐标），公开字段。 */
    public static final class Rect {
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        public Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /** 设备几何：外壳矩形 + 屏幕矩形。 */
    public record Device(boolean monitor, Rect frame, Rect screen) {

        /**
         * 整机（外壳 + 屏幕）水平偏移：正数右移、负数左移。
         *
         * <p>作用于<b>设备矩形本身</b>，因此边框与界面内容会一起平移，
         * 不会出现「内容动了、边框没动」的错位。</p>
         */
        public static final int UI_SHIFT_X = -12;

        /**
         * 按 GUI 尺寸计算：外壳保持基准宽高比，等比例缩放到可完整显示（不放大）。
         */
        public static Device of(int guiWidth, int guiHeight, boolean monitor) {
            float baseW = monitor ? MONITOR_FRAME_W : TOWER_FRAME_W;
            float baseH = monitor ? MONITOR_FRAME_H : TOWER_FRAME_H;
            float scale = Math.min(1.0F,
                    Math.min((guiWidth - 32F) / baseW, (guiHeight - 48F) / baseH));
            int fw = Math.max(8, Math.round(baseW * scale));
            int fh = Math.max(8, Math.round(baseH * scale));
            int bezel = Math.max(4, Math.round((monitor ? MONITOR_BEZEL : TOWER_BEZEL) * (fw / baseW)));
            // 整机平移：外壳与屏幕使用同一个偏移，保证二者始终对齐
            Rect frame = new Rect(((guiWidth - fw) / 2) + UI_SHIFT_X, (guiHeight - fh) / 2, fw, fh);
            Rect screen = new Rect(frame.x + bezel, frame.y + bezel, fw - bezel * 2, fh - bezel * 2);
            return new Device(monitor, frame, screen);
        }
    }

    /** 绘制设备外壳：外壳体、内边框、屏幕底色与指示灯。 */
    public static void drawCasing(GuiGraphics graphics, Device device) {
        drawCasing(graphics, device, true);
    }

    /** 绘制设备外壳；decor=false 时不画指示灯等装饰（如机箱元件界面）。 */
    public static void drawCasing(GuiGraphics graphics, Device device, boolean decor) {
        Rect f = device.frame();
        Rect s = device.screen();
        graphics.fill(f.x, f.y, f.x + f.w, f.y + f.h,
                device.monitor ? 0xFF11151B : 0xFF0D1218);
        graphics.fill(s.x - 2, s.y - 2, s.x + s.w + 2, s.y + s.h + 2,
                device.monitor ? 0xFF3B4654 : 0xFF27313D);
        graphics.fill(s.x, s.y, s.x + s.w, s.y + s.h,
                device.monitor ? 0xFF05070A : 0xFF070B10);

        if (!decor) {
            return;
        }
        if (device.monitor) {
            // 边框右上电源指示灯 + 底部底座
            graphics.fill(s.x + s.w - 14, f.y + 6, s.x + s.w - 4, f.y + 15, 0xFF27A9C8);
            graphics.fill(f.x + f.w / 2 - 38, f.y + f.h - 13, f.x + f.w / 2 + 38, f.y + f.h - 9, 0xFF68717D);
        }
        // 机箱：无装饰（开机动画与元件界面均不绘制灯条/电源按钮）
    }
}
