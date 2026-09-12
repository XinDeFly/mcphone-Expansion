package cn.blockforge.generated.generatedmod.api.client;

import cn.blockforge.generated.mod3ce985ee.client.ComputerLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 可复用的「设备屏幕容器界面」基类：内容始终按**基准尺寸**（{@code baseContentW/H}）绘制，
 * 渲染时整体等比缩放到设备屏幕的实际矩形，交互坐标按同一比例反算。
 *
 * <p>解决的问题：设备外框（外壳/背景）随窗口与「视频设置 → 界面尺寸」变化而缩放，
 * 而容器槽位（原版固定 18 GUI 像素）、物品图标、控件尺寸固定，导致 GUI 尺寸一变就与外框错位、溢出。
 * 本基类把外壳之外的全部内容（背景、主板/面板贴图、槽位框、物品、控件、提示框锚点）
 * 放进同一缩放变换，并用 {@link #isHovering} / 鼠标坐标反算保证点击与悬停落在正确格子上。</p>
 *
 * <p>使用方式：子类在 {@code init()} 中调用 {@link #initDevice(boolean, int, int)}，
 * 覆盖 {@link #renderDeviceCasing(GuiGraphics)} 绘制外壳（真实坐标，不缩放），
 * 其余渲染沿用 {@code renderBg/renderLabels}（内容坐标 = 基准坐标）。
 * 子类若重写鼠标事件，请在方法开头用 {@link #contentX(double)} / {@link #contentY(double)} 换算坐标。</p>
 */
public abstract class ScaledDeviceScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private ComputerLayout.Device device;
    /** 内容缩放 = 设备屏幕实际宽度 / 内容基准宽度。 */
    private float contentScale = 1.0F;
    private int contentW = 1;
    private int contentH = 1;
    /** 是否正在缩放变换内渲染（用于把提示框留到变换外绘制，保持原生字号）。 */
    private boolean renderingContent;
    /** 传入的鼠标坐标是否已经是内容坐标（避免二次反算）。 */
    private boolean contentCoords;

    protected ScaledDeviceScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /**
     * 计算设备几何、内容缩放与内容原点（在 {@code init()} 中调用）。
     *
     * @param monitor      是否显示器（否则为机箱）
     * @param baseContentW 内容基准宽（未缩放，GUI 像素）
     * @param baseContentH 内容基准高（未缩放，GUI 像素）
     */
    protected void initDevice(boolean monitor, int baseContentW, int baseContentH) {
        this.device = ComputerLayout.Device.of(this.width, this.height, monitor);
        ComputerLayout.Rect screen = this.device.screen();
        this.contentW = Math.max(1, baseContentW);
        this.contentH = Math.max(1, baseContentH);
        this.imageWidth = this.contentW;
        this.imageHeight = this.contentH;
        this.contentScale = Math.max(0.05F, screen.w / (float) this.contentW);
        // 内容原点 = 屏幕左上角；内容按基准尺寸绘制后再由 render 统一缩放
        this.leftPos = screen.x;
        this.topPos = screen.y;
    }

    /** 设备几何（外壳 + 屏幕矩形，真实 GUI 坐标）。 */
    protected ComputerLayout.Device device() {
        return this.device;
    }

    /** 内容缩放比例（1.0 = 基准尺寸）。 */
    public float contentScale() {
        return this.contentScale;
    }

    /** 内容基准宽（未缩放）。 */
    public int contentWidth() {
        return this.contentW;
    }

    /** 内容基准高（未缩放）。 */
    public int contentHeight() {
        return this.contentH;
    }

    /** 真实鼠标坐标 → 内容坐标 X（用于命中测试与子类鼠标逻辑）。 */
    public double contentX(double mouseX) {
        return this.leftPos + (mouseX - this.leftPos) / this.contentScale;
    }

    /** 真实鼠标坐标 → 内容坐标 Y。 */
    public double contentY(double mouseY) {
        return this.topPos + (mouseY - this.topPos) / this.contentScale;
    }

    // ---- 渲染：内容整体缩放 ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 屏幕外蒙版 + 设备外壳（真实坐标，不随内容缩放）
        renderBackground(graphics);
        renderDeviceCasing(graphics);
        beforeContentRender();

        int mx = (int) contentX(mouseX);
        int my = (int) contentY(mouseY);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.leftPos, this.topPos, 0.0F);
        pose.scale(this.contentScale, this.contentScale, 1.0F);
        pose.translate(-this.leftPos, -this.topPos, 0.0F);
        this.renderingContent = true;
        // super.render 收到的鼠标坐标已是内容坐标，标记后 isHovering 不再二次反算
        // （否则悬停槽位判定会偏移，高亮框不落在格子上）
        this.contentCoords = true;
        try {
            super.render(graphics, mx, my, partialTick);
        } finally {
            this.contentCoords = false;
            this.renderingContent = false;
            pose.popPose();
        }
        // 提示框在缩放变换之外绘制：位置随格子，字号保持原生
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 绘制设备外壳（真实坐标，不缩放）；子类覆盖。 */
    protected void renderDeviceCasing(GuiGraphics graphics) {
    }

    /** 内容渲染前的每帧钩子（动画推进等）；子类可覆盖。 */
    protected void beforeContentRender() {
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // 缩放变换内的提示框跳过，渲染结束后统一在变换外绘制
        if (this.renderingContent) {
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * 槽位/控件命中测试：把真实鼠标坐标反算到内容坐标后再判断。
     *
     * <p>反算默认在此发生一次；若调用方（子类）已把坐标换算成内容坐标，
     * 请改用 {@code containerMouseXxx(...)} 系列方法，它们会标记坐标已换算，避免二次换算。</p>
     */
    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        if (this.contentCoords) {
            return super.isHovering(x, y, w, h, mouseX, mouseY);
        }
        return super.isHovering(x, y, w, h, contentX(mouseX), contentY(mouseY));
    }

    // ---- 内容坐标交互入口（子类自定义命中后调用；坐标已换算，不再二次换算） ----

    /** 以内容坐标执行原版容器点击（槽位点击 + 控件派发）。 */
    protected boolean containerMouseClicked(double contentMouseX, double contentMouseY, int button) {
        this.contentCoords = true;
        try {
            return super.mouseClicked(contentMouseX, contentMouseY, button);
        } finally {
            this.contentCoords = false;
        }
    }

    /** 以内容坐标执行原版容器释放。 */
    protected boolean containerMouseReleased(double contentMouseX, double contentMouseY, int button) {
        this.contentCoords = true;
        try {
            return super.mouseReleased(contentMouseX, contentMouseY, button);
        } finally {
            this.contentCoords = false;
        }
    }

    /** 以内容坐标执行原版容器拖拽（拖拽位移同步除以缩放）。 */
    protected boolean containerMouseDragged(double contentMouseX, double contentMouseY, int button,
                                            double dragX, double dragY) {
        this.contentCoords = true;
        try {
            return super.mouseDragged(contentMouseX, contentMouseY, button,
                    dragX / this.contentScale, dragY / this.contentScale);
        } finally {
            this.contentCoords = false;
        }
    }

    /** 以内容坐标执行原版容器滚轮。 */
    protected boolean containerMouseScrolled(double contentMouseX, double contentMouseY, double delta) {
        this.contentCoords = true;
        try {
            return super.mouseScrolled(contentMouseX, contentMouseY, delta);
        } finally {
            this.contentCoords = false;
        }
    }

    // ---- 真实坐标入口（子类未重写时的默认路径） ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return containerMouseClicked(contentX(mouseX), contentY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return containerMouseReleased(contentX(mouseX), contentY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return containerMouseDragged(contentX(mouseX), contentY(mouseY), button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return containerMouseScrolled(contentX(mouseX), contentY(mouseY), delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
