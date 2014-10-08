/**
 * 
 */
package cz.warewolf.etoreador.backtest;


/**
 * @author Denis
 *
 */
public class BacktestPosition {
    public enum PositionType {
        LONG, SHORT
    }
    
    public PositionType type;
    public double openPrice;
    public double closePrice;
    public double stoploss;
    public double profitTarget;
    public boolean isOpen;
    public long openTimestamp;
    public long closeTimestamp;
    
    
    public BacktestPosition(PositionType type, double openPrice, double stoploss, double profitTarget, long timestamp) {
        this.type = type;
        this.openPrice = openPrice;
        this.openTimestamp = timestamp;
        this.stoploss = stoploss;
        this.profitTarget = profitTarget;
        this.isOpen = true;
    }
}
