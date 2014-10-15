/**
 * 
 */
package cz.warewolf.etoreador.indicator;

import java.util.Iterator;
import java.util.List;

/**
 * @author Denis
 * 
 */
public class RSI {
    private int period;
    private int cyclesToStart;
    private double avgLoss;
    private double avgGain;
    private double previousPrice;
    private double sumGain;
    private double sumLoss;
    private boolean previousPriceInitialized;

    public RSI(int period) {
        this.period = period;
        this.cyclesToStart = period + 1;
        this.avgGain = 0.0;
        this.avgLoss = 0.0;
        this.sumGain = 0.0;
        this.sumLoss = 0.0;
        this.previousPriceInitialized = false;
    }

    public void reset() {
        this.avgGain = 0.0;
        this.avgLoss = 0.0;
        this.sumGain = 0.0;
        this.sumLoss = 0.0;
        this.cyclesToStart = this.period + 1;
        this.previousPriceInitialized = false;
    }

    public double getChange(double price) {
        double result = 0.0;
        if (this.previousPriceInitialized) {
            result = price - this.previousPrice;
        }
        this.previousPrice = price;
        this.previousPriceInitialized = true;
        return result;
    }
    
    
    public double getAvgGain() {
        return this.avgGain;
    }
    
    public double getAvgLoss() {
        return this.avgLoss;
    }
    
    public double calculate(double price) {
        double rsi = -1.0;
        this.cyclesToStart--;
        double change = this.getChange(price);
        if (this.cyclesToStart == 0) {
            // compute averages for the first time
            avgLoss = sumLoss / this.period;
            avgGain = sumGain / this.period;
            rsi = this.getRsi();
        } else if (this.cyclesToStart < 0) {
            this.cyclesToStart = 0;

            if (change < 0) {
                avgLoss = ((avgLoss * (this.period - 1)) - change) / this.period;
                avgGain = ((avgGain * (this.period - 1)) + 0) / this.period;
            } else {
                avgLoss = ((avgLoss * (this.period - 1)) + 0) / this.period;
                avgGain = ((avgGain * (this.period - 1)) + change) / this.period;
            }
            rsi = this.getRsi();

        } else {
            if (change < 0) {
                sumLoss -= change; // change has negative value => incrementing
                                   // sumLoss value
            } else {
                sumGain += change;
            }
        }
        return rsi;
    }

    public double calculate(List<Double> prices) {
        double result = -1.0;
        if (prices != null && prices.size() > this.period) {
            Iterator<Double> it = prices.iterator();
            while (it.hasNext()) {
                result = this.calculate(it.next());
            }
        }
        return result;
    }

    public double getRsi() {
        double rsi = 0.0;
        if (avgLoss == 0)
            rsi = 100.0;
        else if (avgGain == 0)
            rsi = 0.0;
        else {
            double rs = avgGain / avgLoss;
            rsi = 100 - (100 / (1 + rs));
        }
        return rsi;
    }
}
