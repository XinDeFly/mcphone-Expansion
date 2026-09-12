package cn.blockforge.generated.mod3ce985ee;

/**
 * 电脑设备（显示器 / 机箱）的**几何常量**（纯数据，客户端与服务端共用）。
 *
 * <p>这里只放数值，不引用任何客户端类——因此专用服务器加载本类不会触发客户端类缺失。
 * 渲染相关的外壳绘制与自适应缩放见 {@code client.ComputerLayout}（客户端专用），
 * 菜单槽位坐标则通过 {@link ComputerTowerElementLayout#contentSize()} 取得**内容基准尺寸**，
 * 两端使用同一来源，保证槽位与渲染永远对齐。</p>
 */
public final class ComputerGeometry {

    /** 显示器外框基准与边框厚度。 */
    public static final int MONITOR_FRAME_W = 536;
    public static final int MONITOR_FRAME_H = 296;
    public static final int MONITOR_BEZEL = 8;
    /** 机箱外框基准与边框厚度。 */
    public static final int TOWER_FRAME_W = 306;
    public static final int TOWER_FRAME_H = 291;
    public static final int TOWER_BEZEL = 8;

    /** 显示器屏幕内容基准尺寸（未缩放，GUI 像素）。 */
    public static final int MONITOR_CONTENT_W = MONITOR_FRAME_W - MONITOR_BEZEL * 2;
    public static final int MONITOR_CONTENT_H = MONITOR_FRAME_H - MONITOR_BEZEL * 2;
    /** 机箱屏幕内容基准尺寸（未缩放，GUI 像素）。 */
    public static final int TOWER_CONTENT_W = TOWER_FRAME_W - TOWER_BEZEL * 2;
    public static final int TOWER_CONTENT_H = TOWER_FRAME_H - TOWER_BEZEL * 2;

    private ComputerGeometry() {
    }
}
