package cn.blockforge.generated.generatedmod.client.phone;

import cn.blockforge.generated.generatedmod.api.client.phone.ILandscapePage;
import cn.blockforge.generated.generatedmod.api.client.phone.LandscapePhoneChassis;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 方块金融横屏手机屏幕（容器菜单式托管）。
 *
 * <p>适配层：负责与 {@link MarketMenu}（mode=3）的容器联动，
 * 实际的横屏 UI 全部委托给公共底盘 {@link LandscapePhoneChassis} 与
 * 页面 {@link MarketPage}。</p>
 */
public final class PhoneLandscapeScreen extends AbstractContainerScreen<MarketMenu> {

    private final LandscapePhoneChassis chassis = new LandscapePhoneChassis();
    private final MarketPage page;

    public PhoneLandscapeScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = LandscapePhoneChassis.PHONE_W;
        this.imageHeight = LandscapePhoneChassis.PHONE_H;
        this.page = new MarketPage(menu);
    }

    @Override
    protected void init() {
        super.init();
        this.chassis.init(this.leftPos, this.topPos);
        this.page.init(this.chassis, this.font);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
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
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.page.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 文本框编辑中：吞掉会误触发的游戏快捷键（E 关界面、Q 丢弃、F 副手、数字键交换等），
        // 字母/数字仍由 charTyped 正常写入
        if (cn.blockforge.generated.generatedmod.api.client.TextInputGuard
                .consumeHotkeysWhileEditing(keyCode, scanCode, this.page.isEditingText())) {
            return true;
        }
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
    public boolean isPauseScreen() {
        return false;
    }
}
