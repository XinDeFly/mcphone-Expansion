package cn.blockforge.generated.generatedmod.api.client.phone;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 横屏应用注册表：通过 {@code META-INF/services} 自动发现所有
 * {@link ILandscapeApp}（即带「横屏」标签的应用）。
 */
public final class LandscapeApps {

    private static volatile List<ILandscapeApp> cached;

    private LandscapeApps() {
    }

    /** 所有已注册的横屏应用。 */
    public static List<ILandscapeApp> all() {
        List<ILandscapeApp> list = cached;
        if (list == null) {
            synchronized (LandscapeApps.class) {
                list = cached;
                if (list == null) {
                    list = StreamSupport.stream(
                                    ServiceLoader.load(ILandscapeApp.class).spliterator(), false)
                            .collect(Collectors.toUnmodifiableList());
                    cached = list;
                }
            }
        }
        return list;
    }

    /** 指定的应用是否带有「横屏」标签。 */
    public static boolean hasTag(ResourceLocation appId) {
        if (appId == null) {
            return false;
        }
        for (ILandscapeApp app : all()) {
            if (appId.equals(app.id())) {
                return true;
            }
        }
        return false;
    }
}
