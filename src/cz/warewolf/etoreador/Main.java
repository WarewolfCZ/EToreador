package cz.warewolf.etoreador;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.openqa.selenium.WebDriverException;

import cz.warewolf.etoreador.automation.Automation;
import cz.warewolf.etoreador.automation.InstrumentAutomation;
import cz.warewolf.etoreador.backtest.Backtest;
import cz.warewolf.etoreador.config.Configuration;
import cz.warewolf.etoreador.exception.EToreadorException;
import cz.warewolf.etoreador.file.FileManager;
import cz.warewolf.etoreador.img.Image;
import cz.warewolf.etoreador.img.Recognition;
import cz.warewolf.etoreador.strategy.Order;
import cz.warewolf.etoreador.strategy.Strategy;

/**
 * 
 * @author Denis
 * 
 */
public class Main {

    private static boolean dryRun = false;
    private static Boolean skipLogin;
    private static Boolean captureData;
    private static Boolean backtest;
    private static Boolean findSLAndPT;

    public static void main(String[] args) {
        try {
            System.loadLibrary("opencv_java248");
            // Load configuration from file
            Configuration config = new Configuration("config.properties");
            if (config.loadConfig()) {
                System.out.println("Configuration loaded ");
                List<String> keys = config.getKeys();
                for (String key : keys) {
                    System.out.println("Key: " + key + ", value: " + config.getValue(key));
                }
            }
            dryRun = Boolean.valueOf(config.getValue("dry.run"));
            skipLogin = Boolean.valueOf(config.getValue("skip.login"));
            captureData = Boolean.valueOf(config.getValue("capture"));
            backtest = Boolean.valueOf(config.getValue("backtest"));
            findSLAndPT = Boolean.valueOf(config.getValue("find_sl_pt"));
            ERobot robot = new ERobot();
            Automation au = new Automation(robot, config);

            if (!dryRun && !backtest && skipLogin) {
                System.out.println("Waiting 5 seconds");
                robot.delay(5000, 0);
            }
            
            if (captureData) {
                au.captureHistoricalData(dryRun);
            } else if (findSLAndPT) {
                Backtest bt = new Backtest(config.getValue("backtest.data"), config);
                List<Double> slpt = bt.findOptimalSLAndPT(68, 10, 0.1);
                if (!slpt.isEmpty()) {
                    double stopLoss = slpt.get(0);
                    double profitTarget = slpt.get(1);
                    double balance = slpt.get(2);
                    System.out.println("Optimal Stop loss: " + stopLoss + ", Profit target: " + profitTarget + " Final balance: " + balance);
                }
            } else if (backtest) {
                Backtest bt = new Backtest(config.getValue("backtest.data"), config);
                bt.runBacktest(68, 10, 0.1, Strategy.DEFAULT_STOPLOSS, Strategy.DEFAULT_PROFIT_TARGET);
            } else {
                trade(au, robot, config);
            }
            // TODO: get open trades
            // TODO: close long position
            // TODO: close short position
            // TODO: select real or practice mode

        } catch (EToreadorException e) {
            System.err.println(e.getMessage());
            Automation.takeScreenshotAndExit();
        } catch (AWTException e) {
            e.printStackTrace();
            Automation.takeScreenshotAndExit();
        } catch (WebDriverException e) {
            e.printStackTrace();
            Automation.takeScreenshotAndExit();
        } catch (IOException e) {
            e.printStackTrace();
            Automation.takeScreenshotAndExit();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Automation.takeScreenshotAndExit();
        } catch (Exception e) {
            e.printStackTrace();
            Automation.takeScreenshotAndExit();
        }
    }

    private static void trade(Automation au, ERobot robot, Configuration config) throws NumberFormatException, IOException, InterruptedException, EToreadorException {
        String screenPath = null;
        if (!skipLogin) {
            au.login(dryRun);
        }

        Recognition re = new Recognition();
        FileManager fm = new FileManager();
        InstrumentAutomation ia = new InstrumentAutomation(robot);
        Strategy st = new Strategy("OIL", 10);
        double tWidth = 0.0, tHeight = 0.0, sellPrice, buyPrice;
        long timestamp;
        boolean loop = true;
        // Get instrument prices
        while (loop) {

            if (!dryRun) {
                System.out.println("Taking main screen screenshot");
                screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                                config.getValue("screenshot.type"));
            } else {
                screenPath = config.getValue("test.mainscreen.path");
                System.out.println("Loading main screen screenshot from file " + screenPath);
            }

            Vector<Double> prices = ia.getPrices(
                            re,
                            screenPath,
                            config.getValue("pattern.instrument.oil.path"),
                            Double.valueOf(config.getValue("pattern.instrument.oil.threshold")),
                            config,
                            config.getValue("test.match.instrument.oil.result"),
                            tWidth,
                            tHeight
                            );
            if (ia.getTemplateHeight() > 0 && ia.getTemplateWidth() > 0) {
                tWidth = ia.getTemplateWidth();
                tHeight = ia.getTemplateHeight();
            }
            if (prices != null && prices.size() > 0) {
                sellPrice = prices.get(0);
                buyPrice = prices.get(1);
                timestamp = Calendar.getInstance().getTimeInMillis();
                System.out.println("Oil sell openPrice: " + sellPrice + ", buy openPrice: " + buyPrice);
                fm.appendToTxtFile(config.getValue("instrument.oil.log"), "OIL;" + sellPrice + ";" + buyPrice + ";"
                                                + timestamp + "\n");
            } else {
                throw new EToreadorException("Oil not found");
            }

            double balance = au.getAccountBalance(screenPath);
            System.out.println("Balance: " + balance);

            double equity = au.getEquity(screenPath);
            System.out.println("Equity: " + equity);

            double profit = au.getNetProfit(screenPath);
            System.out.println("Net profit: " + profit);

            if (!ia.isLongOpen(re, config, dryRun)) {
                st.markLongClosed();
            }
            if (!ia.isShortOpen(re, config, dryRun)) {
                st.markShortClosed();
            }
            st.update(sellPrice, buyPrice, balance, equity, timestamp);
            Order o = st.getOrder();
            if (o != null && ia.getPosition() != null) {
                Point p = ia.getPosition();
                Point sellPos = new Point((int) (p.x + (tWidth / 1.5)), p.y);
                Point buyPos = new Point((int) (p.x + (tWidth * 1.2)), p.y);
                Image i = new Image(config.getValue("test.match.instrument.oil.result"));
                i.convertToRGB();
                i.markPoint(p);
                i.markPoint(sellPos, Color.GREEN);
                i.markPoint(buyPos, Color.BLUE);
                i.save();
                switch (o.type) {
                case OPEN_LONG:
                    ia.openLong(buyPos, re, dryRun, config, o.stoploss, o.profitTarget);
                    st.markLongOpened();
                    break;
                case OPEN_SHORT:
                    ia.openShort(sellPos, re, dryRun, config, o.stoploss, o.profitTarget);
                    st.markShortOpened();
                    break;
                default:
                    break;
                
                }
            }
            
            robot.delay(30000, 0);
        }
    }
}
