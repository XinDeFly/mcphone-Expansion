package cn.blockforge.generated.generatedmod.client.phone;

import cn.blockforge.generated.generatedmod.api.client.phone.ILandscapeApp;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 市场扩展「横屏」标签应用：实现 {@link ILandscapeApp} 即表示该应用
 * 支持横屏使用（应用商店点击后手机旋转至横屏）。
 *
 * <p>通过 {@code META-INF/services/...ILandscapeApp} 自动注册。</p>
 */
public final class FinanceLandscapeApp implements ILandscapeApp {

    public static final ResourceLocation ID = new ResourceLocation("generated_mod", "finance_landscape");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.literal("市场扩展");
    }

    @Override
    public ResourceLocation iconTexture() {
        return new ResourceLocation("generated_mod", "textures/app/logo.png");
    }

    @Override
    public String description() {
        return "横屏使用的金融交易系统：股票、期货与现货。";
    }
}
