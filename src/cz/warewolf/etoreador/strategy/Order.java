/**
 * 
 */
package cz.warewolf.etoreador.strategy;

/**
 * @author Denis
 *
 */
public class Order {
    public enum OrderType {
        OPEN_LONG, OPEN_SHORT
    }
    
    public OrderType type;
    public double price;
    public double stoploss;
    public double profitTarget;
    
    public Order(OrderType type, double price, double stoploss, double profitTarget) {
        this.type = type;
        this.price = price;
        this.stoploss = stoploss;
        this.profitTarget = profitTarget;
    }
}
