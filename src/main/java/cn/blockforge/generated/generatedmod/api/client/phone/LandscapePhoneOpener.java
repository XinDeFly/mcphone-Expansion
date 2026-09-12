package cn.blockforge.generated.generatedmod.api.client.phone;

import net.minecraft.client.Minecraft;

/**
 * 横屏界面打开入口：以「旋转至横屏 + 公共底盘」的方式打开任意
 * {@link ILandscapePage}。
 *
 * <pre>{@code
 * LandscapePhoneOpener.open(new MyLandscapePage());
 * }</pre>
 */
public final class LandscapePhoneOpener {

    private LandscapePhoneOpener() {
    }

    /** 在当前客户端打开横屏界面（应用商店点击带「横屏」标签的应用时调用）。 */
    public static void open(ILandscapePage page) {
        if (page == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new LandscapePhoneScreen(page));
    }
}
