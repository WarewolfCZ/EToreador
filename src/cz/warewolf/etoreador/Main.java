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
import cz.warewolf.etoreador.config.Configuration;
import cz.warewolf.etoreador.exception.EToreadorException;
import cz.warewolf.etoreador.file.FileManager;
import cz.warewolf.etoreador.img.Image;
import cz.warewolf.etoreador.img.Recognition;

/**
 * 
 * @author Denis
 * 
 */
public class Main {

    private static boolean dryRun = false;
    private static Boolean skipLogin;

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
            ERobot robot = new ERobot();
            String screenPath = null;
            Automation au = new Automation(robot, config);

            if (!skipLogin) {
                au.login(dryRun);
            } else {
                if (!dryRun) {
                    System.out.println("Waiting 5 seconds");
                    robot.delay(5000, 0);
                }
            }

            Recognition re = new Recognition();
            FileManager fm = new FileManager();
            InstrumentAutomation ia = new InstrumentAutomation(robot);
            double tWidth = 0.0, tHeight = 0.0;
            // Get instrument prices
            for (int j = 0; j < 1; j++) {

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
                    System.out.println("Oil sell price: " + prices.get(0) + ", buy price: " + prices.get(1));
                    fm.appendToTxtFile(config.getValue("instrument.oil.log"),
                                    "OIL;" + prices.get(0) + ";" + prices.get(1) + ";"
                                                    + Calendar.getInstance().getTimeInMillis() + "\n");
                } else {
                    throw new EToreadorException("Oil not found");
                }

                double balance = au.getAccountBalance(screenPath);
                System.out.println("Balance: " + balance);

                double equity = au.getEquity(screenPath);
                System.out.println("Equity: " + equity);

                double profit = au.getNetProfit(screenPath);
                System.out.println("Net profit: " + profit);

                if (ia.getPosition() != null) {
                    Point p = ia.getPosition();
                    Point sellPos = new Point((int) (p.x + (tWidth / 1.5)), p.y);
                    Point buyPos = new Point((int) (p.x + (tWidth * 1.2)), p.y);
                    Image i = new Image(config.getValue("test.match.instrument.oil.result"));
                    i.convertToRGB();
                    i.markPoint(p);
                    i.markPoint(sellPos, Color.GREEN);
                    i.markPoint(buyPos, Color.BLUE);
                    i.save();
                    ia.openLong(buyPos, re, dryRun, config, 85.4, 97.5);
                    ia.openShort(sellPos, re, dryRun, config, 99.1, 87.2);
                }
                
                // robot.delay(30000, 0);
            }
            // TODO: load prices from graph
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

}
