/**
 * 
 */
package cz.warewolf.etoreador.backtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cz.warewolf.etoreador.backtest.BacktestPosition.PositionType;
import cz.warewolf.etoreador.config.Configuration;
import cz.warewolf.etoreador.gui.EWindow;
import cz.warewolf.etoreador.strategy.Order;
import cz.warewolf.etoreador.strategy.StrategyInterface;
import cz.warewolf.etoreador.strategy.TrendStrategy;

/**
 * @author Denis
 * 
 */
public class Backtest {

    private File dataFile;
    private Configuration config;
    private List<BacktestPosition> positions;
    private long profitTrades;
    private long lossTrades;
    private long totalTrades;
    private static final boolean backtestOutput = true;

    public Backtest(String dataFilePath, Configuration config) {
        this.dataFile = new File(dataFilePath);
        this.config = config;
        this.positions = new ArrayList<BacktestPosition>();
        this.profitTrades = 0;
        this.lossTrades = 0;
    }

    public List<Double> findOptimalSLAndPT(double balance, double amountUnit, double dollarTickSize) throws IOException {
        double sl = 0, pt = 0;
        double testBalance = 0;
        double maxBalance = 0;
        List<Double> result = new ArrayList<Double>();
        LinkedList<String> lines = new LinkedList<String>();
        BufferedReader br = new BufferedReader(new FileReader(this.dataFile));

        String line = br.readLine(); // skip header
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
        for (int i = 0; i < 100; i++) {
            sl += 0.01;
            pt = 0;
            for (int j = 0; j < 100; j++) {
                pt += 0.01;
                testBalance = this.runBacktest(balance, amountUnit, dollarTickSize, sl, pt, lines);
                if (testBalance > maxBalance) {
                    maxBalance = testBalance;
                    result.clear();
                    result.add(sl);
                    result.add(pt);
                    result.add(maxBalance);
                }
            }
        }
        return result;
    }

    public double runBacktest(double balance, double amountUnit, double dollarTickSize, double stopLoss,
                    double profitTarget) throws IOException {
        return this.runBacktest(balance, amountUnit, dollarTickSize, stopLoss, profitTarget, null, 0, 0, 0, 0);
    }

    public double runBacktest(double balance, double amountUnit, double dollarTickSize, double stopLoss,
                    double profitTarget, Date backtestDate, int startTime, int endTime) throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(backtestDate);
        cal.set(Calendar.SECOND, 0);
        if (startTime == 0) {
            cal.set(Calendar.HOUR_OF_DAY, 5);
            cal.set(Calendar.MINUTE, 30);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, startTime / 100);
            cal.set(Calendar.MINUTE, startTime - ((startTime / 100) * 100));
        }
        System.out.println("Start time: " + cal.getTime());
        long startTimestamp = cal.getTimeInMillis();
        if (endTime == 0) {
            cal.set(Calendar.HOUR_OF_DAY, 22);
            cal.set(Calendar.MINUTE, 15);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, endTime / 100);
            cal.set(Calendar.MINUTE, endTime - ((endTime / 100) * 100));
        }
        System.out.println("End time: " + cal.getTime());
        long endTimestamp = cal.getTimeInMillis();
        return this.runBacktest(balance, amountUnit, dollarTickSize, stopLoss, profitTarget, null, startTimestamp,
                        endTimestamp, startTime, endTime);

    }

    public double runBacktest(double balance, double amountUnit, double dollarTickSize, double stopLoss,
                    double profitTarget, Date backtestDate) throws IOException {
        return runBacktest(balance, amountUnit, dollarTickSize, stopLoss, profitTarget, backtestDate, 0, 0);
    }

    public double runBacktest(double balance, double amountUnit, double dollarTickSize, double stopLoss,
                    double profitTarget, long startTimestamp, long endTimestamp) throws IOException {
        return this.runBacktest(balance, amountUnit, dollarTickSize, stopLoss, profitTarget, null, startTimestamp,
                        endTimestamp, 0, 0);
    }

    // called from findOptimalSLAndPT();
    public double runBacktest(double balance, double amountUnit, double dollarTickSize, double stopLoss,
                    double profitTarget, List<String> lines) throws IOException {
        return this.runBacktest(balance, amountUnit, dollarTickSize, stopLoss, profitTarget, lines, 0, 0, 0, 0);
    }

    public double runBacktest(double balance, double amountUnit, double dollarTickSize, double stopLoss,
                    double profitTarget, List<String> lines, long startTimestamp, long endTimestamp, int startTime,
                    int endTime)
                    throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(this.dataFile));
        double equity = balance, profit = 0, open = 0, close = 0, high = 0, low = 0, sellPrice = 0, buyPrice = 0;
        double initialBalance = balance;
        double minimumBalance = balance;
        double maximumBalance = balance;
        double dailyStopLoss = Double.valueOf(config.getValue("stoploss.daily"));
        double dailyProfitTarget = Double.valueOf(config.getValue("profit.target.daily"));
        long lineNumber = 1;
        long timestamp = 0;
        String line;
        StrategyInterface st = new TrendStrategy("OIL", amountUnit, stopLoss, profitTarget);
        Calendar cal = Calendar.getInstance();
        BacktestPosition bp = null;
        if (lines == null) {
            lines = new LinkedList<String>();
            @SuppressWarnings("unused")
            String header = br.readLine();
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        boolean skipped = false;
        double dailyBalance = 0.0;
        
        Iterator<String> linesIterator = lines.iterator();
        while (linesIterator.hasNext()) {
            line = linesIterator.next();
            lineNumber++;
            if (line.isEmpty())
                continue;
            // else if (balance < amountUnit) {
            // if (backtestOutput)
            // System.out.println("Lost all money, exiting...");
            // break;
            // }
            String[] items = line.split(";");
            open = Double.valueOf(items[0]);
            close = Double.valueOf(items[1]);
            high = Double.valueOf(items[2]);
            low = Double.valueOf(items[3]);
            sellPrice = open;
            buyPrice = open + 0.1;
            cal.setTimeInMillis(Long.valueOf(items[4]));
            timestamp = Long.valueOf(items[4]);
            int time = (cal.get(Calendar.HOUR_OF_DAY) * 100) + cal.get(Calendar.MINUTE);
            if (startTimestamp > 0 && timestamp < startTimestamp) continue;
            if (endTimestamp > 0 && timestamp > endTimestamp) break;

            if (startTime > 0 && time < startTime) {
//                System.out.println("Skipping time " + time + " < " + startTime);
                continue;
            } else if (endTime > 0 && endTime < time) {
//                System.out.println("Skipping time " + time + " > " + endTime);
                if (!skipped) {
                    balance += this.closePositions(close, close + 0.1, amountUnit, timestamp, dollarTickSize, st);
                    balance = roundDouble(balance);
                    profit = roundDouble((balance - dailyBalance));
                    if (backtestOutput) System.out.println("=== End of day balance: " + balance);
                    if (backtestOutput)
                        System.out.println("=== Daily profit: " + profit + " (" + roundDouble((profit / dailyBalance) * 100) + "%)");
                    skipped = true;
                    dailyBalance = 0.0;
                }
                continue;
            }
            
            if (dailyBalance == 0.0) dailyBalance = balance;
            skipped = false;
            this.updatePositions(open, open + 0.1, timestamp, dollarTickSize, st);
            this.updatePositions(low, low + 0.1, timestamp, dollarTickSize, st);
            this.updatePositions(high, high + 0.1, timestamp, dollarTickSize, st);
            this.updatePositions(close, close + 0.1, timestamp, dollarTickSize, st);

            balance = this.getProfit(balance, dollarTickSize, amountUnit);

            if (dailyBalance - balance >= dailyStopLoss) {
                System.out.println("---Daily stoploss reached: balance: " + balance + " (" + (balance - dailyBalance) + ")");
                balance += this.closePositions(close, close + 0.1, amountUnit, timestamp, dollarTickSize, st);
                balance = roundDouble(balance);
                profit = roundDouble((balance - dailyBalance));
                if (backtestOutput) System.out.println("=== End of day balance: " + balance);
                if (backtestOutput)
                    System.out.println("=== Daily profit: " + profit + " (" + roundDouble((profit / dailyBalance) * 100) + "%)");
                Calendar c = Calendar.getInstance();
                // set startTimestamp to next day
                c.setTimeInMillis(timestamp);
                c.add(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                startTimestamp = c.getTimeInMillis();
                dailyBalance = 0.0;
                continue;
            } else if (roundDouble(balance - dailyBalance) >= dailyProfitTarget) {
                EWindow.setText("Daily profit target reached: " + (balance - dailyBalance));
                System.out.println("+++Daily profit target reached: " + (balance - dailyBalance));
                balance += this.closePositions(close, close + 0.1, amountUnit, timestamp, dollarTickSize, st);
                balance = roundDouble(balance);
                profit = roundDouble((balance - dailyBalance));
                if (backtestOutput) System.out.println("=== End of day balance: " + balance);
                if (backtestOutput)
                    System.out.println("=== Daily profit: " + profit + " (" + roundDouble((profit / dailyBalance) * 100) + "%)");
                Calendar c = Calendar.getInstance();
                // set startTimestamp to next day
                c.setTimeInMillis(timestamp);
                c.add(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                startTimestamp = c.getTimeInMillis();
                dailyBalance = 0.0;
                continue;
            }

            if (balance < minimumBalance) minimumBalance = balance;
            if (balance > maximumBalance) maximumBalance = balance;
            st.update(sellPrice, buyPrice, balance, equity, timestamp);
            Order o = st.getOrder();
            if (o != null && balance >= amountUnit) {
                /*
                 * FileManager fm = new FileManager();
                 * if (backtestOutput)
                 * fm.appendToTxtFile(config.getValue("backtest.log"), o.type +
                 * ";" + o.price + ";" + o.stoploss + ";"
                 * + o.profitTarget
                 * + timestamp + "\n");
                 */
                switch (o.type) {
                case OPEN_LONG:
                    bp = new BacktestPosition(PositionType.LONG, o.price, o.stoploss, o.profitTarget, timestamp);
                    if (backtestOutput) System.out.println("Line " + lineNumber + ": Opening LONG position at price "
                                    + roundDouble(o.price) + ", SL: " + roundDouble(o.stoploss) + ", PT: "
                                    + roundDouble(o.profitTarget) + ", Time: " + cal.getTime());
                    // balance -= amountUnit;
                    st.markLongOpened();
                    balance -= amountUnit;
                    this.positions.add(bp);
                    break;
                case OPEN_SHORT:
                    bp = new BacktestPosition(PositionType.SHORT, o.price, o.stoploss, o.profitTarget, timestamp);
                    if (backtestOutput) System.out.println("Line " + lineNumber + ": Opening SHORT position at price "
                                    + roundDouble(o.price) + ", SL: " + roundDouble(o.stoploss) + ", PT: "
                                    + roundDouble(o.profitTarget) + ", Time: " + cal.getTime());
                    // balance -= amountUnit;
                    st.markShortOpened();
                    balance -= amountUnit;
                    this.positions.add(bp);
                    break;
                default:
                    break;

                }
            }
        }
        balance += this.closePositions(close, close + 0.1, amountUnit, timestamp, dollarTickSize, st);
        balance = roundDouble(balance);
        profit = roundDouble((balance - initialBalance));
        minimumBalance = roundDouble(minimumBalance);
        br.close();
        if (backtestOutput) System.out.println("Initial balance: " + initialBalance);
        if (backtestOutput) System.out.println("Final balance: " + balance);
        if (backtestOutput)
            System.out.println("Profit: " + profit + " (" + roundDouble((profit / initialBalance) * 100) + "%)");
        if (backtestOutput) System.out.println("Maximum balance: " + roundDouble(maximumBalance));
        if (backtestOutput) System.out.println("Minimum balance: " + roundDouble(minimumBalance));
        if (backtestOutput) System.out.println("Profit trades: " + this.profitTrades);
        if (backtestOutput) System.out.println("Loss trades: " + this.lossTrades);
        if (backtestOutput) System.out.println("Total trades: " + this.totalTrades);
        if (backtestOutput) System.out.println("Success rate: "
                        + roundDouble((((double) this.profitTrades / (double) this.totalTrades) * 100)) + "%");

        return balance;
    }

    private double roundDouble(double value) {
        return (double) Math.round(value * 100) / 100;
    }

    private void updatePositions(double sellPrice, double buyPrice, long timestamp, double tickSize,
                    StrategyInterface st) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        for (BacktestPosition bp : this.positions) {
            switch (bp.type) {
            case LONG:
                if (bp.isOpen && (bp.profitTarget <= sellPrice || bp.stoploss >= sellPrice)) {

                    bp.isOpen = false;
                    if (bp.profitTarget <= sellPrice) {
                        bp.closePrice = bp.profitTarget;
                        bp.closeTimestamp = timestamp;
                        if (backtestOutput)
                            System.out.println("Closing LONG position " + roundDouble(bp.openPrice) + " at price: "
                                            + roundDouble(sellPrice)
                                            + " on profit target (" + roundDouble(bp.closePrice - bp.openPrice) + ", "
                                            + roundDouble((bp.closePrice - bp.openPrice) / tickSize) + "$)"
                                            + ", Time: "
                                            + cal.getTime());
                        this.profitTrades++;
                        this.totalTrades++;
                        st.markLongClosed();
                    } else if (bp.stoploss >= sellPrice) {
                        bp.closePrice = bp.stoploss;
                        bp.closeTimestamp = timestamp;
                        this.lossTrades++;
                        this.totalTrades++;
                        st.markLongClosed();
                        if (backtestOutput)
                            System.out.println("Closing LONG position " + roundDouble(bp.openPrice) + " at price: "
                                            + roundDouble(sellPrice)
                                            + " on STOPLOSS (" + roundDouble(bp.closePrice - bp.openPrice) + ", "
                                            + roundDouble((bp.closePrice - bp.openPrice) / tickSize) + "$)"
                                            + ", Time: "
                                            + cal.getTime());
                    }
                }
                break;
            case SHORT:
                if (bp.isOpen && (bp.profitTarget >= buyPrice || bp.stoploss <= buyPrice)) {
                    bp.isOpen = false;
                    if (bp.profitTarget >= buyPrice) {
                        bp.closePrice = bp.profitTarget;
                        this.profitTrades++;
                        this.totalTrades++;
                        st.markShortClosed();
                        if (backtestOutput)
                            System.out.println("Closing SHORT position " + roundDouble(bp.openPrice) + " at price: "
                                            + roundDouble(buyPrice)
                                            + " on profit target (" + roundDouble(bp.closePrice - bp.openPrice) + ", "
                                            + roundDouble(-1 * (bp.closePrice - bp.openPrice) / tickSize) + "$)"
                                            + ", Time: "
                                            + cal.getTime());
                    } else if (bp.stoploss <= buyPrice) {
                        bp.closePrice = bp.stoploss;
                        this.lossTrades++;
                        this.totalTrades++;
                        st.markShortClosed();
                        if (backtestOutput)
                            System.out.println("Closing SHORT position " + roundDouble(bp.openPrice) + " at price: "
                                            + roundDouble(buyPrice)
                                            + " on STOPLOSS (" + roundDouble(bp.closePrice - bp.openPrice) + ", "
                                            + roundDouble(-1 * (bp.closePrice - bp.openPrice) / tickSize) + "$)"
                                            + ", Time: "
                                            + cal.getTime());
                    }
                }
                break;
            default:
                break;

            }

        }
    }

    private double closePositions(double sellPrice, double buyPrice, double amountUnit, long timestamp,
                    double tickSize,
                    StrategyInterface st) {
        double result = 0.0;
        double balanceDelta = 0.0;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        for (BacktestPosition bp : this.positions) {
            bp.isOpen = false;
            bp.closeTimestamp = timestamp;
            switch (bp.type) {
            case LONG:
                bp.closePrice = sellPrice;
                if (bp.closePrice > bp.openPrice)
                    this.profitTrades++;
                else
                    this.lossTrades++;
                this.totalTrades++;
                st.markLongClosed();
                result += bp.closePrice - bp.openPrice;
                balanceDelta += amountUnit;
                if (backtestOutput)
                    System.out.println("Closing LONG position " + roundDouble(bp.openPrice) + " at price: "
                                    + roundDouble(sellPrice)
                                    + " on CLOSING EXCHANGE (" + roundDouble(bp.closePrice - bp.openPrice) + ", "
                                    + roundDouble((bp.closePrice - bp.openPrice) / tickSize) + "$)" + ", Time: "
                                    + cal.getTime());
                break;
            case SHORT:
                bp.closePrice = buyPrice;
                if (bp.closePrice < bp.openPrice)
                    this.profitTrades++;
                else
                    this.lossTrades++;
                this.totalTrades++;
                result += bp.openPrice - bp.closePrice;
                balanceDelta += amountUnit;
                st.markShortClosed();
                if (backtestOutput)
                    System.out.println("Closing SHORT position " + roundDouble(bp.openPrice) + " at price: "
                                    + roundDouble(buyPrice)
                                    + " on CLOSING EXCHANGE (" + roundDouble(bp.openPrice - bp.closePrice) + ", "
                                    + roundDouble(-1 * (bp.closePrice - bp.openPrice) / tickSize) + "$)" + ", Time: "
                                    + cal.getTime());
                break;
            default:
                break;

            }
        }
        result = result / tickSize;
        result += balanceDelta;
        this.positions.clear();
        return result;
    }

    /**
     * 
     * @param tickSize
     *            tick size in dollars
     * @param amountUnit
     * @return
     */
    private double getProfit(double balance, double tickSize, double amountUnit) {
        double result = 0.0;
        List<BacktestPosition> toRemove = new ArrayList<BacktestPosition>();
        for (BacktestPosition bp : this.positions) {
            if (!bp.isOpen) {
                toRemove.add(bp);
                switch (bp.type) {
                case LONG:
                    result += bp.closePrice - bp.openPrice;
                    balance += amountUnit;
                    break;
                case SHORT:
                    result += bp.openPrice - bp.closePrice;
                    balance += amountUnit;
                    break;
                default:
                    break;
                }
            }
        }
        result = result / tickSize;
        result += balance;
        for (BacktestPosition bp : toRemove) {
            this.positions.remove(bp);
        }
        return result;
    }
}
