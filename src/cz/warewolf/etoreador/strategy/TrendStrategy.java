/**
 * 
 */
package cz.warewolf.etoreador.strategy;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import cz.warewolf.etoreador.indicator.RSI;
import cz.warewolf.etoreador.strategy.Order.OrderType;

/**
 * @author Denis
 * 
 */
public class TrendStrategy implements StrategyInterface {

    @SuppressWarnings("unused")
    private String instrumentName;
    private boolean longOpened;
    private boolean shortOpened;
    private Queue<Double> sellPrices;
    private Queue<Double> sma;
    private double lastSellPrice;
    private double lastBuyPrice;
    private double lastRsiValue;
    private double balance;
    private double amount;
    private double stopLoss;
    private double profitTarget;
    private double smaBegin;
    private double smaEnd;
    private RSI rsi;
    private double rsiValue;
    private double maxRsiValue;
    private double minRsiValue;
    private static int QUEUE_SIZE = 8;
    private static int SMA_PERIOD = 12;
    private static int RSI_PERIOD = 2;
    private static double RSI_OVERSOLD = 16;
    private static double RSI_OVERBOUGHT = 84;
    // private static double TREND_STRENGTH = 0.05;
    public static double DEFAULT_STOPLOSS = 0.6; // dollars 0.57
    public static double DEFAULT_PROFIT_TARGET = 0.5; // dollars 0.64

    public TrendStrategy(String instrumentName, double amount, double stopLoss2, double profitTarget) {
        this.instrumentName = instrumentName;
        this.stopLoss = stopLoss2;
        this.profitTarget = profitTarget;
        this.amount = amount;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
        this.sma = new ArrayDeque<Double>(QUEUE_SIZE);
        this.rsi = new RSI(RSI_PERIOD);
        this.maxRsiValue = 0.0;
        this.minRsiValue = Double.MAX_VALUE;
    }

    public TrendStrategy(String instrumentName, double amount) {
        this.instrumentName = instrumentName;
        this.stopLoss = DEFAULT_STOPLOSS;
        this.profitTarget = DEFAULT_PROFIT_TARGET;
        this.amount = amount;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
        this.sma = new ArrayDeque<Double>(QUEUE_SIZE);
        this.rsi = new RSI(RSI_PERIOD);
        this.maxRsiValue = 0.0;
        this.minRsiValue = Double.MAX_VALUE;
    }

    public void reset() {
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
        this.sma = new ArrayDeque<Double>(QUEUE_SIZE);
        this.rsi = new RSI(RSI_PERIOD);
        this.maxRsiValue = 0.0;
        this.minRsiValue = Double.MAX_VALUE;
        this.lastRsiValue = 0.0;
        this.smaBegin = 0.0;
        this.smaEnd = 0.0;
        this.balance = 0.0;
        this.longOpened = false;
        this.shortOpened = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#update(double,
     * double, double, double, long)
     */
    @Override
    public void update(double sellPrice, double buyPrice, double balance, double equity, long timestamp) {
        if (this.sellPrices.size() >= QUEUE_SIZE) {
            this.sellPrices.poll();
        }
        if (this.sma.size() >= SMA_PERIOD) {
            this.sma.poll();
        }
        this.sellPrices.add(sellPrice);
        this.lastRsiValue = this.rsiValue;
        this.rsiValue = this.rsi.calculate(sellPrice);

        if (this.rsiValue > 0 && this.rsiValue < RSI_OVERSOLD && this.rsiValue < this.minRsiValue) {
            this.minRsiValue = this.rsiValue;
        } else if (this.rsiValue > 0 && this.rsiValue > RSI_OVERBOUGHT && this.rsiValue > this.maxRsiValue) {
            this.maxRsiValue = this.rsiValue;
        }

        // System.out.println("RSI: " + rsiValue);
        Iterator<Double> it = this.sellPrices.iterator();
        double avg = 0.0;
        double sum = 0.0;
        int count = 0;
        while (it.hasNext()) {
            count++;
            sum += it.next();
        }
        avg = sum / count;

        this.sma.add(avg);

        it = this.sma.iterator();
        sum = 0;
        int count1 = 0, count2 = 0;
        double sum2 = 0;

        while (it.hasNext()) {
            double val = it.next();
            if (count1 > this.sma.size() / 2) {
                count2++;
                sum2 += val;
            } else {
                count1++;
                sum += val;
            }
        }
        this.lastSellPrice = sellPrice;
        this.lastBuyPrice = buyPrice;
        this.smaBegin = sum / count1;
        this.smaEnd = sum2 / count2;
        this.balance = balance;
    }

    private double roundDouble(double value) {
        return (double) Math.round(value * 100) / 100;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#getOrder()
     */
    @Override
    public Order getOrder() {
        Order result = null;
        boolean adjustProfitTarget = true;
        if (balance > amount && this.sellPrices.size() >= QUEUE_SIZE) {

            if (this.rsiValue > 0 && this.lastRsiValue > 0) {
                /************************** LONG ************************/
                if (this.rsiValue > RSI_OVERSOLD && this.lastRsiValue < RSI_OVERSOLD
                                && !isLongOpened() && !isShortOpened()
                                && smaBegin < smaEnd) {
                    double weight = RSI_OVERSOLD - this.minRsiValue;
                    double magicCoeficient = (10 - weight) / 8;
                    if (magicCoeficient <= 0)
                        magicCoeficient = 0.0;
                    else if (magicCoeficient > profitTarget) magicCoeficient = profitTarget * 0.9;
                    // if (weight > 10) {
                    double tmp = 0.0;
                    if (adjustProfitTarget)
                        tmp = stopLoss - (magicCoeficient / 1.3);
                    else
                        tmp = stopLoss;

                    if (tmp > amount / 10) tmp = amount / 10;
                    double sl = lastBuyPrice - tmp;

                    double pt = 0;
                    if (adjustProfitTarget)
                        pt = lastBuyPrice + profitTarget - magicCoeficient;
                    else
                        pt = lastBuyPrice + profitTarget;
                    System.out.println("RSI value: " + roundDouble(this.rsiValue) + ", weight: " + roundDouble(weight)
                                    + ", previous RSI value: " + this.lastRsiValue);
                    this.minRsiValue = Double.MAX_VALUE;
                    result = new Order(OrderType.OPEN_LONG, roundDouble(lastBuyPrice),
                                    roundDouble(sl), roundDouble(pt));
                    // }
                    /************************** SHORT ************************/
                } else if (this.rsiValue < RSI_OVERBOUGHT && this.lastRsiValue > RSI_OVERBOUGHT
                                && this.rsiValue > RSI_OVERSOLD
                                && !isLongOpened() && !isShortOpened()
                                && smaBegin > smaEnd) {
                    double weight = this.maxRsiValue - RSI_OVERBOUGHT;
                    double magicCoeficient = (10 - weight) / 8;
                    if (magicCoeficient <= 0)
                        magicCoeficient = 0.0;
                    else if (magicCoeficient > profitTarget) magicCoeficient = profitTarget * 0.9;
                    // if (weight > 10) {
                    double tmp = 0.0;
                    if (adjustProfitTarget)
                        tmp = stopLoss + (magicCoeficient / 1.3);
                    else
                        tmp = stopLoss;

                    if (tmp > amount / 10) tmp = amount / 10;
                    double sl = lastSellPrice + tmp;
                    double pt = 0;
                    if (adjustProfitTarget)
                        pt = lastSellPrice - profitTarget + magicCoeficient;
                    else
                        pt = lastSellPrice - profitTarget;
                    System.out.println("RSI value: " + roundDouble(this.rsiValue) + ", weight: " + roundDouble(weight)
                                    + ", previous RSI value: " + this.lastRsiValue);
                    this.maxRsiValue = 0.0;
                    result = new Order(OrderType.OPEN_SHORT, roundDouble(lastSellPrice), roundDouble(sl),
                                    roundDouble(pt));
                    // }
                }
            }
            /*
             * if (this.smaBegin > this.smaEnd && (this.smaBegin - this.smaEnd)
             * > TREND_STRENGTH && !isShortOpened()) {
             * result = new Order(OrderType.OPEN_SHORT,
             * roundDouble(lastSellPrice), roundDouble(lastSellPrice
             * + stopLoss), roundDouble(lastSellPrice - profitTarget));
             * } else if (this.smaBegin < this.smaEnd && (this.smaEnd -
             * this.smaBegin) > TREND_STRENGTH && !isLongOpened()) {
             * result = new Order(OrderType.OPEN_LONG,
             * roundDouble(lastBuyPrice),
             * roundDouble(lastBuyPrice - stopLoss), roundDouble(lastBuyPrice +
             * profitTarget));
             * }
             */
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markLongOpened()
     */
    @Override
    public void markLongOpened() {
        this.setLongOpened(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markShortOpened()
     */
    @Override
    public void markShortOpened() {
        this.setShortOpened(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markShortClosed()
     */
    @Override
    public void markShortClosed() {
        this.setShortOpened(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markLongClosed()
     */
    @Override
    public void markLongClosed() {
        this.setLongOpened(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#isLongOpened()
     */
    @Override
    public boolean isLongOpened() {
        return longOpened;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * cz.warewolf.etoreador.strategy.StrategyInterface#setLongOpened(boolean)
     */
    @Override
    public void setLongOpened(boolean longOpened) {
        this.longOpened = longOpened;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#isShortOpened()
     */
    @Override
    public boolean isShortOpened() {
        return shortOpened;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * cz.warewolf.etoreador.strategy.StrategyInterface#setShortOpened(boolean)
     */
    @Override
    public void setShortOpened(boolean shortOpened) {
        this.shortOpened = shortOpened;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * cz.warewolf.etoreador.strategy.StrategyInterface#getRemainingIdleCycles()
     */
    @Override
    public int getRemainingIdleCycles() {
        return QUEUE_SIZE - this.sellPrices.size();
    }
}
