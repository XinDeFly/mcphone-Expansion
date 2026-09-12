package cn.blockforge.generated.generatedmod.data;

public final class MarketSnapshot {
    private final long balance;
    private final int dollarItems;
    private final long day;
    private final long dayTime;
    private final String status;
    private final String asset;
    private final boolean hasQuote;
    private final boolean futures;
    private final double price;
    private final double change;
    private final double[] history;
    private final int stockTotal;
    private final int stockLocked;
    private final int futureQty;
    private final double futureEntry;
    private final double futureMargin;
    private final long futureExpiryDay;

    public MarketSnapshot(long balance, int dollarItems, long day, long dayTime, String status) {
        this(balance, dollarItems, day, dayTime, status, "", false, 0.0, 0.0, new double[0], 0, 0, 0, 0.0, 0.0, 0L);
    }

    public MarketSnapshot(long balance, int dollarItems, long day, long dayTime, String status, String asset,
                          boolean futures, double price, double change, double[] history, int stockTotal, int stockLocked,
                          int futureQty, double futureEntry, double futureMargin, long futureExpiryDay) {
        this.balance = balance;
        this.dollarItems = dollarItems;
        this.day = day;
        this.dayTime = dayTime;
        this.status = status == null ? "" : status;
        this.asset = asset == null ? "" : asset;
        this.hasQuote = !this.asset.isEmpty();
        this.futures = futures;
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

    public static MarketSnapshot empty() {
        return new MarketSnapshot(0, 0, 0, 0, "");
    }

    public long balance() {
        return balance;
    }

    public int dollarItems() {
        return dollarItems;
    }

    public long day() {
        return day;
    }

    public long dayTime() {
        return dayTime;
    }

    public String status() {
        return status;
    }

    public String asset() {
        return asset;
    }

    public boolean hasQuote() {
        return hasQuote;
    }

    public boolean futures() {
        return futures;
    }

    public double price() {
        return price;
    }

    public double change() {
        return change;
    }

    public double[] history() {
        return history;
    }

    public int stockTotal() {
        return stockTotal;
    }

    public int stockLocked() {
        return stockLocked;
    }

    public int futureQty() {
        return futureQty;
    }

    public double futureEntry() {
        return futureEntry;
    }

    public double futureMargin() {
        return futureMargin;
    }

    public long futureExpiryDay() {
        return futureExpiryDay;
    }

    public AssetQuote toQuote() {
        return hasQuote ? new AssetQuote(price, change, history, stockTotal, stockLocked,
                futureQty, futureEntry, futureMargin, futureExpiryDay) : null;
    }
}
