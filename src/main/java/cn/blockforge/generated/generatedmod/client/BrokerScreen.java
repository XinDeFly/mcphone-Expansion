package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 做市商交易界面（菜单 mode 6）。
 *
 * <p>复用现货终端界面 {@link AbstractSpotScreen} 的全部交互（搜索、数量、买入/卖出、存储与背包），
 * 由 {@code menu.mode == 6} 切换为「做市商模式」：</p>
 * <ul>
 *   <li>价格显示改为<b>双列报价</b>：买价 bid（券商收货）/ 卖价 ask（券商出货）+ 价差；</li>
 *   <li>买入按 <b>ask</b>、卖出按 <b>bid</b> 成交，<b>不额外收手续费</b>（价差即成本）；</li>
 *   <li>成交价锚定当日行情中间价，由服务端按稀有度价差（1%~10%）计算。</li>
 * </ul>
 */
public final class BrokerScreen extends AbstractSpotScreen {

    public BrokerScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
