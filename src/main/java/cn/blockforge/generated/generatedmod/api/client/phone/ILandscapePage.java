package cn.blockforge.generated.generatedmod.api.client.phone;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 横屏页面接口：由横屏手机底盘（{@link LandscapePhoneScreen} 或
 * {@code PhoneLandscapeScreen}）托管并驱动整个生命周期。
 *
 * <p>页面通过 {@link #init(LandscapePhoneChassis, net.minecraft.client.gui.Font)} 拿到底盘引用，
 * 之后即可使用底盘的公共控件（手机边框、右侧功能条、内容区几何）进行绘制，
 * 并在页面内自行处理点击 / 键盘 / 滚轮事件。</p>
 */
public interface ILandscapePage {

    /** 底盘完成初始化后调用：保存 chassis 与字体，创建编辑框等控件。 */
    void init(LandscapePhoneChassis chassis, net.minecraft.client.gui.Font font);

    /** 每帧绘制页面内容（此时旋转动画已完成）。 */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /** 鼠标点击。返回 true 表示已处理。 */
    boolean mouseClicked(double mouseX, double mouseY, int button);

    /** 按键按下。返回 true 表示已处理。 */
    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    /** 字符输入。返回 true 表示已处理。 */
    boolean charTyped(char codePoint, int modifiers);

    /** 鼠标滚轮。返回 true 表示已处理。 */
    boolean mouseScrolled(double mouseX, double mouseY, double delta);

    /** 鼠标拖拽（按住移动）。返回 true 表示已处理。 */
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    /** 鼠标松开。 */
    default void mouseReleased(double mouseX, double mouseY, int button) {
    }

    /** 页面关闭（屏幕 onClose）时回调。 */
    default void onClose() {
    }
}
