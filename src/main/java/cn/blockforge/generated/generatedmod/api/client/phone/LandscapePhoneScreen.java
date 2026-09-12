package cn.blockforge.generated.generatedmod.api.client.phone;

import com.november.mcphone.core.client.PhoneScreenOpener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 通用横屏手机屏幕：以 {@link ILandscapePage} 为内容，基于公共底盘
 * {@link LandscapePhoneChassis} 展示，托管旋转动画、手机边框、右侧功能条与页面生命周期。
 *
 * <p>适合不依赖容器菜单的横屏应用直接打开；需要服务端数据交互的应用
 * 请参照MCphone Market Expansion自身流程（通过 {@code MarketMenu(mode=3)} 打开对应屏幕）。</p>
 */
public class LandscapePhoneScreen extends Screen {

    private final ILandscapePage page;
    private final LandscapePhoneChassis chassis = new LandscapePhoneChassis();

    public LandscapePhoneScreen(ILandscapePage page) {
        super(Component.empty());
        this.page = page;
    }

    @Override
    protected void init() {
        super.init();
        this.chassis.init((this.width - LandscapePhoneChassis.PHONE_W) / 2,
                (this.height - LandscapePhoneChassis.PHONE_H) / 2);
        this.page.init(this.chassis, this.font);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0x3A262626);
        this.chassis.renderPhone(graphics, this.font);
        if (this.chassis.isBusy()) {
            return;
        }
        this.page.render(graphics, mouseX, mouseY, partialTick);
        this.chassis.renderNavHover(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || this.chassis.isBusy()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.chassis.hitNav((int) mouseX, (int) mouseY) == 1) {
            this.returnToDesktop();
            return true;
        }
        return this.page.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.page.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.page.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.page.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.page.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.page.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        this.page.onClose();
        super.onClose();
    }

    private void returnToDesktop() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            PhoneScreenOpener.open(this.minecraft.player);
        } else {
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
