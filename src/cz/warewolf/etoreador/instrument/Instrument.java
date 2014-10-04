/**
 * 
 */
package cz.warewolf.etoreador.instrument;

/**
 * @author Denis
 *
 */
public class Instrument {
    public double buyPrice;
    public double sellPrice;
    public String name;
    
    public Instrument(String name, double bPrice, double sPrice) {
        this.name = name;
        this.buyPrice = bPrice;
        this.sellPrice = sPrice;
    }
}
