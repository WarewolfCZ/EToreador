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
public class Strategy {

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
    private static int QUEUE_SIZE = 32;
    public static double DEFAULT_STOPLOSS = 0.6; // dollars 0.57
    public static double DEFAULT_PROFIT_TARGET = 0.7; // dollars 0.64

    public Strategy(String instrumentName, double amount, double stopLoss2, double profitTarget) {
        this.instrumentName = instrumentName;
        this.avgSell = 0.0;
        this.stopLoss = stopLoss2;
        this.profitTarget = profitTarget;
        this.amount = amount;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
    }

    public Strategy(String instrumentName, double amount) {
        this.instrumentName = instrumentName;
        this.avgSell = 0.0;
        this.stopLoss = DEFAULT_STOPLOSS;
        this.profitTarget = DEFAULT_PROFIT_TARGET;
        this.amount = amount;
        this.sellPrices = new ArrayDeque<Double>(QUEUE_SIZE);
    }

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

    public Order getOrder() {
        Order result = null;
        if (balance > amount && this.sellPrices.size() >= QUEUE_SIZE) {
            if (avgSell > lastSellPrice && !shortOpened) result = new Order(OrderType.OPEN_SHORT, lastSellPrice, lastSellPrice + stopLoss, lastSellPrice - profitTarget);
            else if (avgSell < lastSellPrice && !longOpened) result = new Order(OrderType.OPEN_LONG, lastBuyPrice, lastBuyPrice - stopLoss, lastBuyPrice + profitTarget);
        }
        return result;
    }

    public void markLongOpened() {
        this.longOpened = true;
    }

    public void markShortOpened() {
        this.shortOpened = true;
    }

    public void markShortClosed() {
        this.shortOpened = false;
    }

    public void markLongClosed() {
        this.longOpened = false;
    }
}
