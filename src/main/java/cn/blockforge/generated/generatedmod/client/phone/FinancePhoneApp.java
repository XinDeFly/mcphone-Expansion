package cn.blockforge.generated.generatedmod.client.phone;

import cn.blockforge.generated.generatedmod.api.client.phone.LandscapeApps;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import com.november.mcphone.api.client.app.IPhoneApp;
import com.november.mcphone.api.client.app.RequiredMod;
import com.november.mcphone.core.client.GuiUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class FinancePhoneApp implements IPhoneApp {
    public static final ResourceLocation ID = new ResourceLocation("generated_mod", "finance");
    private static final ResourceLocation ICON = new ResourceLocation("generated_mod", "textures/app/logo.png");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("方块金融");
    }

    @Override
    public ResourceLocation getIconTexture() {
        return ICON;
    }

    @Override
    public void renderIcon(GuiGraphics graphics, int x, int y, int size, float partialTick) {
        ResourceLocation tex = this.getIconTexture();
        if (tex != null) {
            GuiUtil.drawTexture(graphics, tex, x, y, size, size, size, size);
        }
        // 「横屏」标签徽标：仅带该标签的应用（原版应用没有）显示在图标右下角。
        if (LandscapeApps.hasTag(ID)) {
            int badge = Math.max(6, size / 3);
            int bx = x + size - badge - 1;
            int by = y + size - badge - 1;
            graphics.fill(bx, by, x + size - 1, y + size - 1, 0xF2121A2E);
            graphics.fill(bx, by, x + size - 1, by + 1, 0xFFD4AF37);
            graphics.fill(bx, y + size - 2, x + size - 1, y + size - 1, 0xFFD4AF37);
            graphics.fill(bx, by, bx + 1, y + size - 1, 0xFFD4AF37);
            graphics.fill(x + size - 2, by, x + size - 1, y + size - 1, 0xFFD4AF37);
            if (size >= 24) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                graphics.drawString(mc.font, "横", bx + (badge - 8) / 2, by + (badge - 8) / 2, 0xFFFFE28A);
            }
        }
    }

    @Override
    public void onPress() {
        Network.sendToServer(new MarketActionPacket(MarketActionPacket.OPEN_PHONE_HUB, "", 0));
    }

    @Override
    public String getVersion() {
        return "3.5.1";
    }

    @Override
    public String getAuthor() {
        return "Xinde";
    }

    @Override
    public String getDescription() {
        return "股票、期货、现货的金融交易系统。支持横屏使用。";
    }

    @Override
    public List<RequiredMod> requiredMods() {
        return List.of(new RequiredMod("generated_mod", "方块金融"));
    }

    @Override
    public boolean isPreinstalled() {
        return false;
    }
}
