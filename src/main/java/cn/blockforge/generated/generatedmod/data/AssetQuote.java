package cn.blockforge.generated.generatedmod.data;

public final class AssetQuote {
    public final double price;
    public final double change;
    public final double[] history;
    public final int stockTotal;
    public final int stockLocked;
    public final int futureQty;
    public final double futureEntry;
    public final double futureMargin;
    public final long futureExpiryDay;

    public AssetQuote(double price, double change, double[] history, int stockTotal, int stockLocked,
                      int futureQty, double futureEntry, double futureMargin, long futureExpiryDay) {
        this.price = price;
        this.change = change;
        this.history = history;
        this.stockTotal = stockTotal;
        this.stockLocked = stockLocked;
        this.futureQty = futureQty;
        this.futureEntry = futureEntry;
        this.futureMargin = futureMargin;
        this.futureExpiryDay = futureExpiryDay;
    }
}
