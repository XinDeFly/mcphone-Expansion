package cn.blockforge.generated.generatedmod.data;

public final class FuturesPosition {
    public int qty;
    public double avgEntry;
    public double markPrice;
    public double margin;
    public long expiryDay;

    public FuturesPosition(int qty, double avgEntry, double markPrice, double margin, long expiryDay) {
        this.qty = qty;
        this.avgEntry = avgEntry;
        this.markPrice = markPrice;
        this.margin = margin;
        this.expiryDay = expiryDay;
    }
}
