package cn.blockforge.generated.mod3ce985ee;

/**
 * 机箱存储元件界面的几何布局（纯数学，双端共用）：
 * 标题栏正下方即为主板（PCB）区域——CPU 横向加宽居中，左右两侧各 4 条服务器级 DIMM 内存插槽；
 * PCB 上边紧贴标题、下边贴近电源面板（仅 6px 深色隔断），插槽下沿距电源面板顶部 25px；
 * 面板内为玩家背包 9×4（靠左侧排列，右侧留给电源 LOGO 图案）。
 * 槽位坐标按内容区**基准尺寸**计算，渲染时由 {@code api.client.ScaledDeviceScreen}
 * 连同外框一起等比缩放，保证在不同「界面尺寸」设置下与渲染完全对齐。
 */
public final class ComputerTowerElementLayout {

    /** 内存插槽间距（16px 槽位 + 2px 间隙，与真实 DIMM 插槽相邻一致）。 */
    public static final int RAM_PITCH = 18;
    /** 单侧 4 条插槽所占宽度。 */
    public static final int RAM_BANK_WIDTH = 3 * RAM_PITCH + 16;

    /** 标题栏高度：PCB 顶边紧贴标题下沿（不留粗隔断）。 */
    public static final int TITLE_BAND = 18;
    /** CPU 与 PCB 顶边的间距。 */
    public static final int CPU_TOP_INSET = 12;
    /** 内存插槽槽体高度（相对 ramY：上 46 / 下 50），槽位框与贴图共用。 */
    public static final int DIMM_ABOVE = 46;
    public static final int DIMM_BELOW = 50;
    public static final int DIMM_HEIGHT = DIMM_ABOVE + DIMM_BELOW;
    /** 内存插槽下沿与电源面板顶部的间距（需求值，勿随意改动）。 */
    public static final int DIMM_PANEL_GAP = 25;
    /** PCB 底边与电源面板顶部之间的深色隔断（越小越贴合）。 */
    public static final int PCB_BOTTOM_GAP = 6;
    /** 电源面板自身高度（含背包 9×4 与右侧 LOGO 区）。 */
    public static final int PANEL_HEIGHT = 118;

    private ComputerTowerElementLayout() {
    }

    /** 一次性布局结果。 */
    public static final class Layout {
        public final int boardTop;
        public final int boardBottom;
        /** CPU（横向加宽，顶部居中）。 */
        public final int cpuX;
        public final int cpuY;
        public final int cpuW;
        public final int cpuH;
        /** 左右两侧内存插槽组的起点 x 与插槽 y。 */
        public final int leftBankX;
        public final int rightBankX;
        public final int ramY;
        /** 金属面板顶边与玩家背包第 1 格坐标。 */
        public final int panelTop;
        public final int cellX;
        public final int cellY;

        Layout(int boardTop, int boardBottom, int cpuX, int cpuY, int cpuW, int cpuH,
               int leftBankX, int rightBankX, int ramY, int panelTop, int cellX, int cellY) {
            this.boardTop = boardTop;
            this.boardBottom = boardBottom;
            this.cpuX = cpuX;
            this.cpuY = cpuY;
            this.cpuW = cpuW;
            this.cpuH = cpuH;
            this.leftBankX = leftBankX;
            this.rightBankX = rightBankX;
            this.ramY = ramY;
            this.panelTop = panelTop;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        /** 第 index 个内存槽位（0~3 左侧，4~7 右侧）的 x。 */
        public int elementSlotX(int index) {
            return index < 4 ? leftBankX + index * RAM_PITCH
                    : rightBankX + (index - 4) * RAM_PITCH;
        }
    }

    /** 按内容区尺寸计算布局（**自上而下**紧凑排布，深色隔断仅保留数像素）。 */
    public static Layout of(int w, int h) {
        // 标题栏正下方即为主板顶边（PCB 紧贴标题，不留粗隔断）
        int boardTop = TITLE_BAND;
        // CPU：横向加宽（受左右内存插槽组限制）
        int cpuW = Math.max(48, Math.min(w * 34 / 100, w - 12 - 2 * RAM_BANK_WIDTH - 2 * 14));
        int cpuH = Math.max(36, cpuW * 72 / 100);
        int cpuX = 6 + (w - 12 - cpuW) / 2;
        int cpuY = boardTop + CPU_TOP_INSET;
        // 内存插槽与 CPU 下部对齐（同真实主板内存布局）
        int ramY = cpuY + cpuH - 18;
        int dimmBottom = ramY + DIMM_BELOW;
        // 金属面板：顶部与槽体下沿保持 DIMM_PANEL_GAP；同时保证面板自身高度不少于 PANEL_HEIGHT-8
        int panelTop = Math.min(dimmBottom + DIMM_PANEL_GAP,
                Math.max(boardTop + 1, h - PANEL_HEIGHT));
        // PCB 底边贴近面板顶部（仅 PCB_BOTTOM_GAP 的深色隔断）
        int boardBottom = panelTop - PCB_BOTTOM_GAP;
        int leftBankX = Math.max(6, cpuX - 14 - RAM_BANK_WIDTH);
        int rightBankX = Math.min(w - 6 - RAM_BANK_WIDTH, cpuX + cpuW + 14);
        // 背包/快捷栏靠左排列，右侧留给电源 LOGO 图案
        int cellX = 12;
        int cellY = panelTop + 24;
        return new Layout(boardTop, boardBottom, cpuX, cpuY, cpuW, cpuH,
                leftBankX, rightBankX, ramY, panelTop, cellX, cellY);
    }

    /**
     * 机箱屏幕内容**基准尺寸**（未缩放，GUI 像素）——与窗口大小及「视频设置 → 界面尺寸」无关。
     *
     * <p>槽位坐标固定在该基准空间中计算，渲染时由 {@code api.client.ScaledDeviceScreen}
     * 连同外框一起等比缩放：这样 GUI 尺寸变化时，边框、背景、槽位框、物品图标与交互命中
     * 始终使用同一缩放，不会出现「外框缩放而格子不跟随」的错位。</p>
     *
     * <p>数值取自 {@link ComputerGeometry}（双端共用、不引用客户端类），
     * 因此**服务端也可安全调用**（菜单槽位坐标即由此得出）。</p>
     */
    public static int[] contentSize() {
        return new int[]{ComputerGeometry.TOWER_CONTENT_W, ComputerGeometry.TOWER_CONTENT_H};
    }
}
