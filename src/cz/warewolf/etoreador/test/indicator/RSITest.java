/**
 * 
 */
package cz.warewolf.etoreador.test.indicator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cz.warewolf.etoreador.indicator.RSI;

/**
 * @author Denis
 *
 */
public class RSITest {

    private ArrayList<Double> values;
    private RSI rsi;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.rsi = new RSI(14);
        this.values = new ArrayList<Double>();
        this.values.add(44.3389);
        this.values.add(44.0902);
        this.values.add(44.1497);
        this.values.add(43.6124);
        this.values.add(44.3278);
        this.values.add(44.8264);
        this.values.add(45.0955);
        this.values.add(45.4245);
        this.values.add(45.8433);
        this.values.add(46.0826);
        this.values.add(45.8931);
        this.values.add(46.0328);
        this.values.add(45.6140);
        this.values.add(46.2820);
        this.values.add(46.2820);
        this.values.add(46.0028);
        this.values.add(46.0328);
        this.values.add(46.4116);
        this.values.add(46.2222);
        this.values.add(45.6439);
        this.values.add(46.2122);
        this.values.add(46.2521);
        this.values.add(45.7137);
        this.values.add(46.4515);
        this.values.add(45.7835);
        this.values.add(45.3548);
        this.values.add(44.0288);
        this.values.add(44.1783);
        this.values.add(44.2181);
        this.values.add(44.5672);
        this.values.add(43.4205);
        this.values.add(42.6628);
        this.values.add(43.1314);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link cz.warewolf.etoreador.indicator.RSI#calculate(java.util.List)}.
     */
    @Test
    public void testCalculateListOfDouble() {
        double rsiVal = this.rsi.calculate(this.values);
        assertEquals(37.77, rsiVal, 0.005);
    }
    
    /**
     * Test method for {@link cz.warewolf.etoreador.indicator.RSI#getChange(double)}.
     */
    @Test
    public void testGetChange() {
        int i = 0;
        assertEquals(0.0, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.25, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.06, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.54, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.72, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.50, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.27, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.33, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.42, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.24, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.19, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.14, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.42, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.67, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.28, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.03, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.38, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.19, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.58, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.57, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.04, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.54, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.74, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.67, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.43, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-1.33, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.15, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.04, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.35, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-1.15, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(-0.76, this.rsi.getChange(this.values.get(i)), 0.005);i++;
        assertEquals(0.47, this.rsi.getChange(this.values.get(i)), 0.005);i++;
    }

    /**
     * Test method for {@link cz.warewolf.etoreador.indicator.RSI#calculate(double)}.
     */
    @Test
    public void testCalculate() {
        int i = 0;
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(-1.0, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.0, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.0, this.rsi.getAvgLoss(), 0.005);
        assertEquals(70.53, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.24, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.10, this.rsi.getAvgLoss(), 0.005);
        assertEquals(66.32, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.22, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.11, this.rsi.getAvgLoss(), 0.005);
        assertEquals(66.55, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(69.41, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(66.36, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(57.97, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(62.93, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.22, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.13, this.rsi.getAvgLoss(), 0.005);
        assertEquals(63.26, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(56.06, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(62.38, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(54.71, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(50.42, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(39.99, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(41.46, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.18, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.26, this.rsi.getAvgLoss(), 0.005);
        assertEquals(41.87, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(45.46, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(37.30, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(33.08, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(37.77, this.rsi.calculate(this.values.get(i)), 0.005);i++;
        assertEquals(0.18, this.rsi.getAvgGain(), 0.005);
        assertEquals(0.30, this.rsi.getAvgLoss(), 0.005);
    }
}
