package cz.warewolf.etoreador;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.openqa.selenium.WebDriverException;

import cz.warewolf.etoreador.automation.Automation;
import cz.warewolf.etoreador.automation.InstrumentAutomation;
import cz.warewolf.etoreador.backtest.Backtest;
import cz.warewolf.etoreador.config.Configuration;
import cz.warewolf.etoreador.exception.EToreadorException;
import cz.warewolf.etoreador.file.FileManager;
import cz.warewolf.etoreador.gui.EWindow;
import cz.warewolf.etoreador.img.Image;
import cz.warewolf.etoreador.img.Recognition;
import cz.warewolf.etoreador.strategy.Order;
import cz.warewolf.etoreador.strategy.StrategyInterface;
import cz.warewolf.etoreador.strategy.TrendStrategy;

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
    private static Double profitTarget;
    private static Double stoploss;

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
            stoploss = Double.valueOf(config.getValue("stoploss.trade"));
            profitTarget = Double.valueOf(config.getValue("profit.target.trade"));
            ERobot robot = new ERobot();
            Automation au = new Automation(robot, config);
            EWindow.show();
            if (!dryRun && !backtest && skipLogin) {
                System.out.println("Waiting 5 seconds");
                for (int i = 0; i < 5; i++) {
                    String text = "Waiting " + (5-i) + " seconds";
                    EWindow.setText(text);
                    robot.delay(1000, 0);
                }
            }
            
            if (captureData) {
                EWindow.setText("Capturing data");
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
                EWindow.setText("Backtesting");
                Backtest bt = new Backtest(config.getValue("backtest.data"), config);
                String startTimestampStr = config.getValue("backtest.start", "0");
                String endTimestampStr = config.getValue("backtest.end", "0");
                String backtestDateStr = config.getValue("backtest.date");
                Date backtestDate = null;
                if (!backtestDateStr.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                    backtestDate = sdf.parse(backtestDateStr);
                }
                if (startTimestampStr.isEmpty()) startTimestampStr = "0";
                if (endTimestampStr.isEmpty()) endTimestampStr = "0";
                long startTimestamp = Long.valueOf(startTimestampStr);
                long endTimestamp = Long.valueOf(endTimestampStr);
                if (backtestDate != null) {
                    bt.runBacktest(68, 10, 0.1, stoploss, profitTarget, backtestDate);
                } else if (startTimestamp > 0 || endTimestamp > 0) {
                    bt.runBacktest(68, 10, 0.1, stoploss, profitTarget, startTimestamp, endTimestamp);
                } else {
                    bt.runBacktest(68, 10, 0.1, stoploss, profitTarget);
                }
                EWindow.hide();
            } else {
                EWindow.setText("Trading");
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
            EWindow.setText("Logging in");
            au.login(dryRun);
        }

        Recognition re = new Recognition();
        FileManager fm = new FileManager();
        InstrumentAutomation ia = new InstrumentAutomation(robot);
        StrategyInterface st = new TrendStrategy("OIL", 10);
        double tWidth = 0.0, tHeight = 0.0, sellPrice, buyPrice;
        double dailyStopLoss = Double.valueOf(config.getValue("stoploss.daily"));
        double dailyProfitTarget = Double.valueOf(config.getValue("profit.target.daily"));
        long timestamp;
        boolean loop = true;
        
        EWindow.setText("Taking screenshot");
        if (!dryRun) {
            System.out.println("Taking main screen screenshot");
            screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                            config.getValue("screenshot.type"));
        } else {
            screenPath = config.getValue("test.mainscreen.path");
            System.out.println("Loading main screen screenshot from file " + screenPath);
        }
        EWindow.setText("Getting initial balance");
        double initialBalance = au.getAccountBalance(screenPath);
        System.out.println("Initial balance: " + initialBalance);
        
        // Get instrument prices
        while (loop) {
            EWindow.setText("Taking screenshot");
            if (!dryRun) {
                System.out.println("Taking main screen screenshot");
                screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                                config.getValue("screenshot.type"));
            } else {
                screenPath = config.getValue("test.mainscreen.path");
                System.out.println("Loading main screen screenshot from file " + screenPath);
            }
            EWindow.setText("Getting prices");
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

            EWindow.setText("Getting balance value");
            double balance = au.getAccountBalance(screenPath);
            System.out.println("Balance: " + balance);

            /*EWindow.setText("Getting equity value");
            double equity = au.getEquity(screenPath);
            System.out.println("Equity: " + equity);
*/
            if (initialBalance - balance >= dailyStopLoss) {
                throw new EToreadorException("Daily stoploss reached: balance: " + balance);
            } else if (balance - initialBalance >= dailyProfitTarget) {
                EWindow.setText("Daily profit target reached: " + (balance - initialBalance));
                System.out.println("Daily profit target reached: " + (balance - initialBalance));
                break;
            }
            
            /*
            EWindow.setText("Getting profit value");
            double profit = au.getNetProfit(screenPath);
            System.out.println("Net profit: " + profit);
             */
            if (!ia.isLongOpen(re, config, dryRun)) {
                EWindow.setText("Long position not opened");
                st.markLongClosed();
            } else {
                st.markLongOpened();
            }
            
            if (!ia.isShortOpen(re, config, dryRun)) {
                EWindow.setText("Short position not opened");
                st.markShortClosed();
            } else {
                st.markShortOpened();
            }
            
            st.update(sellPrice, buyPrice, balance, 0, timestamp);
            Order o = st.getOrder();
            if (o != null) {
                EWindow.setText("Signal: " + o.type + "");
                robot.delay(400, 0);
            } else {
                EWindow.setText("No signal");
                robot.delay(200, 0);
            }
            
            if (o != null && ia.getPosition() != null) {
                EWindow.setText("Opening trade");
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
//                    loop = false; ///////////////////////////////////////
                    break;
                case OPEN_SHORT:
                    ia.openShort(sellPos, re, dryRun, config, o.stoploss, o.profitTarget);
                    st.markShortOpened();
//                    loop = false; ///////////////////////////////////////
                    break;
                default:
                    break;
                
                }
            }
            robot.mouseMove(0, 0);
            int delay = 300;
            for (int i = 0; i < delay; i++) {
                String text = "Waiting " + (delay-i) + " seconds (B:" + balance + ",L:" + st.isLongOpened() + ",S:" + st.isShortOpened()+",I:" + st.getRemainingIdleCycles() + ")";
                if (o != null) text += ",O:" + o.type;
                EWindow.setText(text);
                robot.delay(1000, 0);
            }
        }
    }
}
