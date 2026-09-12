package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class StockScreen extends AbstractTradeScreen {
    public StockScreen(MarketMenu menu, Inventory inventory) {
        super(menu, inventory, Component.literal("股票交易"));
    }

    @Override
    protected void trade(boolean buy) {
        if (this.selected >= 0 && this.selected < this.matchCount()) {
            Network.sendToServer(new MarketActionPacket(
                    buy ? MarketActionPacket.BUY_STOCK : MarketActionPacket.SELL_STOCK,
                    this.matchAsset(this.selected), this.amount));
        }
    }

    @Override
    protected String holdingLabel(AssetQuote quote) {
        return "股票持仓: " + quote.stockTotal + "（今日锁定 " + quote.stockLocked + "）";
    }

    @Override
    protected int sellable(AssetQuote quote) {
        return quote.stockTotal - quote.stockLocked;
    }

    @Override
    protected long holdingsOf(AssetQuote quote) {
        return quote.stockTotal;
    }

    @Override
    protected boolean futuresBoard() {
        return false;
    }
}
