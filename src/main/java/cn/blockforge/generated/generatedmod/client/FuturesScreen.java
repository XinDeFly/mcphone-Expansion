package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FuturesScreen extends AbstractTradeScreen {
    public FuturesScreen(MarketMenu menu, Inventory inventory) {
        super(menu, inventory, Component.literal("期货交易"));
    }

    @Override
    protected void trade(boolean buy) {
        if (this.selected >= 0 && this.selected < this.matchCount()) {
            Network.sendToServer(new MarketActionPacket(
                    buy ? MarketActionPacket.BUY_FUTURE : MarketActionPacket.SELL_FUTURE,
                    this.matchAsset(this.selected), this.amount));
        }
    }

    @Override
    protected String holdingLabel(AssetQuote quote) {
        if (quote.futureQty == 0) {
            return "无期货持仓";
        }
        String direction = quote.futureQty > 0 ? "多" : "空";
        return "期货: " + quote.futureQty + " " + direction
                + "，保证金 $" + Math.round(quote.futureMargin)
                + "，到期日 第 " + quote.futureExpiryDay + " 天";
    }

    @Override
    protected int sellable(AssetQuote quote) {
        return Math.abs(quote.futureQty);
    }

    @Override
    protected long holdingsOf(AssetQuote quote) {
        return Math.abs(quote.futureQty);
    }

    @Override
    protected boolean futuresBoard() {
        return true;
    }
}
