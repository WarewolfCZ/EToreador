/**
 * 
 */
package cz.warewolf.etoreador.strategy;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import cz.warewolf.etoreador.strategy.Order.OrderType;

/**
 * @author Denis
 * 
 */
public class SimpleStrategy implements StrategyInterface {

    @SuppressWarnings("unused")
    private String instrumentName;
    private boolean longOpened;
    private boolean shortOpened;
    private double avgSell;
    private Queue<Double> sellPrices;
    private double lastSellPrice;
    private double lastBuyPrice;
    private double balance;
    private double amount;
    private double stopLoss;
    private double profitTarget;
    private static int QUEUE_SIZE = 20;
    public static double DEFAULT_STOPLOSS = 0.7; // dollars 0.57
    public static double DEFAULT_PROFIT_TARGET = 0.5; // dollars 0.64

    public SimpleStrategy(String instrumentName, double amount, double stopLoss2, double profitTarget) {
        this.instrumentName = instrumentName;
        this.avgSell = 0.0;
        this.stopLoss = stopLoss2;
        this.profitTarget = profitTarget;
        this.amount = amount;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
    }

    public SimpleStrategy(String instrumentName, double amount) {
        this.instrumentName = instrumentName;
        this.avgSell = 0.0;
        this.stopLoss = DEFAULT_STOPLOSS;
        this.profitTarget = DEFAULT_PROFIT_TARGET;
        this.amount = amount;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
    }

    public void reset() {
        this.avgSell = 0.0;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
        this.balance = 0.0;
        this.longOpened = false;
        this.shortOpened = false;
        this.lastBuyPrice = 0.0;
        this.lastSellPrice = 0.0;
    }
    
    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#update(double, double, double, double, long)
     */
    @Override
    public void update(double sellPrice, double buyPrice, double balance, double equity, long timestamp) {
        if (this.sellPrices.size() >= QUEUE_SIZE) {
            this.sellPrices.poll();
        }
        this.sellPrices.add(sellPrice);
        Iterator<Double> it = this.sellPrices.iterator();
        double avg = 0.0;
        double sum = 0.0;
        int count = 0;
        while (it.hasNext()) {
            count++;
            sum += it.next();
        }
        avg = sum / count;
//        System.out.println("Average sell openPrice: " + avg);
        this.avgSell = avg;
        this.lastSellPrice = sellPrice;
        this.lastBuyPrice = buyPrice;
        this.balance = balance;
    }
    
    private double roundDouble(double value) {
        return (double) Math.round(value * 100) / 100;
    }


    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#getOrder()
     */
    @Override
    public Order getOrder() {
        Order result = null;
        if (balance > amount && this.sellPrices.size() >= QUEUE_SIZE) {
            if (avgSell > lastSellPrice && !isShortOpened()) result = new Order(OrderType.OPEN_SHORT, roundDouble(lastSellPrice), roundDouble(lastSellPrice + stopLoss), roundDouble(lastSellPrice - profitTarget));
            else if (avgSell < lastSellPrice && !isLongOpened()) result = new Order(OrderType.OPEN_LONG, roundDouble(lastBuyPrice), roundDouble(lastBuyPrice - stopLoss), roundDouble(lastBuyPrice + profitTarget));
        }
        return result;
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markLongOpened()
     */
    @Override
    public void markLongOpened() {
        this.setLongOpened(true);
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markShortOpened()
     */
    @Override
    public void markShortOpened() {
        this.setShortOpened(true);
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markShortClosed()
     */
    @Override
    public void markShortClosed() {
        this.setShortOpened(false);
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#markLongClosed()
     */
    @Override
    public void markLongClosed() {
        this.setLongOpened(false);
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#isLongOpened()
     */
    @Override
    public boolean isLongOpened() {
        return longOpened;
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#setLongOpened(boolean)
     */
    @Override
    public void setLongOpened(boolean longOpened) {
        this.longOpened = longOpened;
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#isShortOpened()
     */
    @Override
    public boolean isShortOpened() {
        return shortOpened;
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#setShortOpened(boolean)
     */
    @Override
    public void setShortOpened(boolean shortOpened) {
        this.shortOpened = shortOpened;
    }

    /* (non-Javadoc)
     * @see cz.warewolf.etoreador.strategy.StrategyInterface#getRemainingIdleCycles()
     */
    @Override
    public int getRemainingIdleCycles() {
        return QUEUE_SIZE - this.sellPrices.size();
    }
}
