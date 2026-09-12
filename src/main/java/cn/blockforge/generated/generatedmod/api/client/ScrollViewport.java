package cn.blockforge.generated.generatedmod.api.client;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;

/**
 * 可复用的行滚动视口：固定显示 N 行，超出部分通过右侧滑条或鼠标滚轮滚动。
 *
 * <p>滚动采用「平滑上下滑动」：旧内容整体滑出、新内容从边缘滑入（无淡入淡出遮罩）。
 * 适用于任意以「行」为单位的列表或槽位区域（例如显示器控制台的机箱存储区）。
 * 坐标使用与渲染一致的空间（容器界面内通常为面板相对坐标），
 * 由调用方在命中测试前把鼠标坐标换算到同一空间。</p>
 *
 * <p>滑动期间的可见内容由调用方根据 {@link #slideFromRow()} / {@link #slideTargetRow()} /
 * {@link #slideProgress()} 自行绘制（内容切换发生在滑动结束时）。</p>
 */
public final class ScrollViewport {

    /** 滑条宽度。 */
    public static final int BAR_WIDTH = 4;
    /** 默认滑动时长（毫秒）。 */
    public static final long DEFAULT_SLIDE_MS = 180L;

    private int x;
    private int y;
    private int width;
    private int height;
    private final int visibleRows;
    private final int rowPitch;
    private final long slideMs;

    private int contentRows = 1;
    private int scrollRow;
    private int fromRow;
    private int pendingRow = -1;
    /** 滑动状态：0 空闲 / 1 滑动中。 */
    private int phase;
    private long phaseStart;
    private boolean dragging;
    private IntConsumer rowListener = row -> {
    };

    /**
     * @param x, y        视口左上角
     * @param width       视口宽度（滑条绘制在其右侧 BAR_WIDTH 内）
     * @param height      视口高度（= 可见行数 × 行高）
     * @param visibleRows 可见行数
     * @param slideMs     滑动时长（毫秒）
     */
    public ScrollViewport(int x, int y, int width, int height, int visibleRows, long slideMs) {
        this(x, y, width, height, visibleRows, Math.max(1, height / Math.max(1, visibleRows)), slideMs);
    }

    /**
     * @param rowPitch 行间距（与槽位行距一致，保证滑动位移与格子对齐）
     */
    public ScrollViewport(int x, int y, int width, int height, int visibleRows, int rowPitch, long slideMs) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visibleRows = Math.max(1, visibleRows);
        this.rowPitch = Math.max(1, rowPitch);
        this.slideMs = Math.max(1L, slideMs);
    }

    /** 更新视口位置与尺寸（界面缩放/布局变化时调用）。 */
    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** 设置内容总行数（决定滑条与最大滚动行）。 */
    public void setContentRows(int rows) {
        this.contentRows = Math.max(1, rows);
        this.scrollRow = Math.min(this.scrollRow, maxScrollRow());
    }

    public int scrollRow() {
        return scrollRow;
    }

    public int visibleRows() {
        return visibleRows;
    }

    public int maxScrollRow() {
        return Math.max(0, contentRows - visibleRows);
    }

    /** 注册滚动回调（滑动结束时触发，用于同步菜单/服务端）。 */
    public void onRowChanged(IntConsumer listener) {
        this.rowListener = listener == null ? row -> {
        } : listener;
    }

    /** 注册「滚动请求」回调（发起滑动的瞬间触发，用于立即同步内容数据/服务端）。 */
    public void onScrollRequest(IntConsumer listener) {
        this.scrollRequestListener = listener == null ? row -> {
        } : listener;
    }

    private IntConsumer scrollRequestListener = row -> {
    };

    /** 立即设置滚动行（无动画）。 */
    public void setScrollRow(int row) {
        this.scrollRow = Math.max(0, Math.min(maxScrollRow(), row));
    }

    /** 请求滚动到指定行（平滑上下滑动动画）。 */
    public void requestScroll(int row) {
        int clamped = Math.max(0, Math.min(maxScrollRow(), row));
        if (clamped == this.scrollRow) {
            return;
        }
        this.scrollRequestListener.accept(clamped);
        if (this.phase == 1) {
            // 滑动中：仅更新目标（不重复启动）
            this.pendingRow = clamped;
            return;
        }
        this.fromRow = this.scrollRow;
        this.pendingRow = clamped;
        this.phase = 1;
        this.phaseStart = System.currentTimeMillis();
    }

    /** 每帧调用：推进滑动；结束时切换内容并触发回调。返回是否处于滑动中。 */
    public boolean update() {
        if (this.phase == 0) {
            return false;
        }
        if (System.currentTimeMillis() - this.phaseStart < this.slideMs) {
            return true;
        }
        if (this.pendingRow >= 0) {
            this.scrollRow = this.pendingRow;
            this.pendingRow = -1;
            this.rowListener.accept(this.scrollRow);
        }
        this.phase = 0;
        return false;
    }

    /** 是否正在滑动；返回 -1 表示空闲，否则返回进度 0~1。 */
    public float slideProgress() {
        if (this.phase == 0) {
            return -1.0F;
        }
        return Math.min(1.0F,
                (System.currentTimeMillis() - this.phaseStart) / (float) this.slideMs);
    }

    /** 滑动起始行（用于绘制滑出的旧内容）。 */
    public int slideFromRow() {
        return this.fromRow;
    }

    /** 滑动目标行（用于绘制滑入的新内容）。 */
    public int slideTargetRow() {
        return this.pendingRow;
    }

    /** 单元格绘制回调（坐标为视口内容坐标，调用方在既有 pose 下绘制即可）。 */
    @FunctionalInterface
    public interface CellPainter {
        /**
         * @param index 容器/内容索引（内容行 × 9 + 列，由调用方约定）
         * @param x     单元格左上角 x（内容坐标）
         * @param y     单元格左上角 y（内容坐标，已含滑动的整体位移）
         */
        void paint(GuiGraphics graphics, int index, int x, int y);
    }

    /**
     * 绘制滑动中的内容条带（旧内容滑出、新内容滑入），并补画**不随滚动位移**的左右竖边。
     *
     * <p>约定：单元格 16×16，左上角即 {@code (x, y)}（与原版 {@code renderSlot} 同坐标）；
     * 裁剪只作用于上下位移的内容，因此左右竖边在裁剪之外单独绘制，滚动期间不会消失；
     * 上下沿保持随内容滑动的效果。</p>
     *
     * @param contentRows     内容总行数（超出可见行数的部分不绘制）
     * @param cellsPerRow     每行单元格数（**必须与调用方槽位布局一致**；索引 = 行×列数 + 列）
     * @param cellBorder      单元格 1px 描边颜色（传 0 跳过）
     * @param sideBorder      左右两条竖边颜色（传 0 跳过）
     * @param screenScale     内容缩放（{@code ScaledDeviceScreen#contentScale()}；无缩放传 1）
     * @param contentOriginX  内容原点屏幕 X（{@code ScaledDeviceScreen} 的 leftPos；无缩放传 0）
     * @param contentOriginY  内容原点屏幕 Y
     * @param painter         单元格绘制回调（绘制物品图标等）
     */
    public void renderSlide(GuiGraphics graphics, int contentRows, int cellsPerRow, int cellBorder, int sideBorder,
                            float screenScale, int contentOriginX, int contentOriginY, CellPainter painter) {
        float progress = slideProgress();
        int from = this.fromRow;
        int to = this.pendingRow;
        if (progress < 0.0F || to < 0 || to == from) {
            return;
        }
        // 列数必须由调用方给出：视口宽度含边框余量，用 width/pitch 整除会少算一列，
        // 导致滑动期间读取到相邻格子的物品（表现为"滚动时物品临时错位"）
        int columns = Math.max(1, cellsPerRow);
        int slideDelta = (to - from) * this.rowPitch;
        // 裁剪区用真实屏幕坐标（内容可能被整体缩放过）
        graphics.enableScissor(
                Math.round(contentOriginX + screenScale * this.x),
                Math.round(contentOriginY + screenScale * this.y),
                Math.round(contentOriginX + screenScale * (this.x + this.width)),
                Math.round(contentOriginY + screenScale * (this.y + this.height)));
        int first = Math.min(from, to);
        int last = Math.max(from, to) + this.visibleRows - 1;
        int offset = Math.round(slideDelta * progress);
        for (int row = first; row <= last; row++) {
            if (row < 0 || row >= contentRows) {
                continue;
            }
            int y = this.y + (row - from) * this.rowPitch - offset;
            for (int col = 0; col < columns; col++) {
                int x = this.x + col * this.rowPitch;
                if (cellBorder != 0) {
                    UiDraw.slotFrame(graphics, x, y, cellBorder);
                }
                if (painter != null) {
                    painter.paint(graphics, row * columns + col, x, y);
                }
            }
        }
        graphics.disableScissor();
        // 左右竖边：独立于滚动，任何时刻都可见（上下沿仍随内容滑动）
        if (sideBorder != 0) {
            graphics.fill(this.x - 1, this.y - 1, this.x, this.y + this.height + 1, sideBorder);
            graphics.fill(this.x + this.width, this.y - 1,
                    this.x + this.width + 1, this.y + this.height + 1, sideBorder);
        }
    }

    /** 内容坐标命中测试（含 2px 容差，供界面屏蔽滑动期间的点击等使用）。 */
    public boolean contains(double contentMouseX, double contentMouseY) {
        return inArea(contentMouseX, contentMouseY);
    }

    /** 绘制滑条（仅当内容超出可见行数时）。 */
    public void renderScrollbar(GuiGraphics graphics, int trackColor, int thumbColor, int borderColor) {
        int max = maxScrollRow();
        if (max <= 0) {
            return;
        }
        int bx = this.x + this.width + 2;
        graphics.fill(bx, this.y, bx + BAR_WIDTH, this.y + this.height, trackColor);
        graphics.fill(bx, this.y, bx + 1, this.y + this.height, borderColor);
        graphics.fill(bx + BAR_WIDTH - 1, this.y, bx + BAR_WIDTH, this.y + this.height, borderColor);
        int thumbH = thumbHeight();
        int thumbY = this.y + (this.height - thumbH) * this.scrollRow / max;
        graphics.fill(bx + 1, thumbY, bx + BAR_WIDTH - 1, thumbY + thumbH, thumbColor);
    }

    /** 鼠标滚轮（相对坐标）；消费事件返回 true。 */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScrollRow() <= 0 || !inArea(mouseX, mouseY)) {
            return false;
        }
        requestScroll(this.scrollRow + (delta < 0 ? 1 : -1));
        return true;
    }

    /** 鼠标点击（相对坐标）：命中滑条则开始拖拽。 */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (maxScrollRow() <= 0) {
            return false;
        }
        int bx = this.x + this.width + 2;
        if (mouseX >= bx - 2 && mouseX < bx + BAR_WIDTH + 2 && mouseY >= this.y && mouseY < this.y + this.height) {
            this.dragging = true;
            scrollTo(mouseY);
            return true;
        }
        return false;
    }

    /** 鼠标拖拽（相对坐标）。 */
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!this.dragging) {
            return false;
        }
        scrollTo(mouseY);
        return true;
    }

    /** 鼠标释放。 */
    public boolean mouseReleased() {
        boolean was = this.dragging;
        this.dragging = false;
        return was;
    }

    private void scrollTo(double mouseY) {
        int max = maxScrollRow();
        if (max <= 0) {
            return;
        }
        int thumbH = thumbHeight();
        double rel = Math.max(0.0, Math.min(this.height - thumbH, mouseY - this.y - thumbH / 2.0));
        int row = (int) Math.round(rel * max / Math.max(1.0, this.height - thumbH));
        requestScroll(row);
    }

    private int thumbHeight() {
        int max = maxScrollRow();
        if (max <= 0) {
            return this.height;
        }
        int rows = max + this.visibleRows;
        return Math.max(10, this.height * this.visibleRows / rows);
    }

    private boolean inArea(double mouseX, double mouseY) {
        return mouseX >= this.x - 2 && mouseX <= this.x + this.width + 2
                && mouseY >= this.y - 2 && mouseY <= this.y + this.height + 2;
    }
}
