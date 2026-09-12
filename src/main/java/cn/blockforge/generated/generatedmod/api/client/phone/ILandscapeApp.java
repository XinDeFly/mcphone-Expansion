package cn.blockforge.generated.generatedmod.api.client.phone;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 横屏应用标签接口。
 *
 * <p>实现本接口即表示：该手机应用支持横屏使用，并获得「横屏」标签。
 * 原版 MCphone 应用不实现本接口，因此不会带有该标签。</p>
 *
 * <p>注册方式：在类路径中提供
 * {@code META-INF/services/cn.blockforge.generated.generatedmod.api.client.phone.ILandscapeApp}，
 * 并在其中按行写入实现类的全限定名。注册后可通过 {@link LandscapeApps} 查询。</p>
 */
public interface ILandscapeApp {

    /** 应用唯一 ID（建议使用本模组的命名空间）。 */
    ResourceLocation id();

    /** 应用显示名称。 */
    Component displayName();

    /** 应用图标贴图（纹理地址），可为 null。 */
    ResourceLocation iconTexture();

    /** 应用简介。 */
    String description();
}
