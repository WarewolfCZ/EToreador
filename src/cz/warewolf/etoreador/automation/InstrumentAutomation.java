/**
 * 
 */
package cz.warewolf.etoreador.automation;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Vector;

import cz.warewolf.etoreador.ERobot;
import cz.warewolf.etoreador.config.Configuration;
import cz.warewolf.etoreador.exception.EToreadorException;
import cz.warewolf.etoreador.img.Image;
import cz.warewolf.etoreador.img.Recognition;

/**
 * @author Denis
 * 
 */
public class InstrumentAutomation {
    private Point instrumentPosition;
    private double matchedTemplWidth;
    private double matchedTemplHeight;
    private ERobot robot;

    public InstrumentAutomation(ERobot robot) {
        this.matchedTemplWidth = 0.0;
        this.matchedTemplHeight = 0.0;
        this.robot = robot;
    }

    public double getTemplateWidth() {
        return this.matchedTemplWidth;
    }

    public double getTemplateHeight() {
        return this.matchedTemplHeight;
    }

    public Point getPosition() {
        return this.instrumentPosition;
    }

    public Vector<Double> getPrices(Recognition re, String screenPath, String patternPath, double patternThreshold,
                    Configuration config, String matchResultPath) throws IOException,
                    InterruptedException, EToreadorException {
        return this.getPrices(re, screenPath, patternPath, patternThreshold, config, matchResultPath, 0.0, 0.0);
    }

    public Vector<Double> getPrices(Recognition re, String screenPath, String patternPath, double patternThreshold,
                    Configuration config, String matchResultPath, double templSizeX, double templSizeY)
                    throws IOException,
                    InterruptedException, EToreadorException {
        Vector<Double> result = new Vector<Double>();
        Vector<Point> vec = null;
        // Find oil position
        System.out.println("Searching for instrument with pattern " + patternPath);
        vec = re.matchTemplate(
                        screenPath,
                        patternPath,
                        patternThreshold,
                        matchResultPath,
                        templSizeX,
                        templSizeY
                        );
        System.out.println("Instrument search result count: " + vec.size() + ", scale X: " + re.getTemplateWidth()
                        + ", scale Y: " + re.getTemplateHeight());

        if (vec.size() > 0) {
            this.matchedTemplWidth = re.getTemplateWidth();
            this.matchedTemplHeight = re.getTemplateHeight();
            System.out.println("Instrument found");
            Point p1 = vec.get(0);
            Point p2 = vec.get(1);
            Point middle = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
            Point p3 = new Point(p2.x + 30, p1.y);
            Point p4 = new Point(p2.x + (p2.x - p1.x) - 5, p2.y);
            instrumentPosition = middle;
            /*
             * i = new Image(config.getValue("grayscale3.path"));
             * i.markPoint(instrumentPosition);
             * i.save();
             */

            System.out.println("Preparing OCR");
            Image img = new Image(screenPath);
            BufferedImage oilImg = img.crop(p3, p4);
            Image i2 = new Image(oilImg, config.getValue("instrument.ocr.image"));
            i2.mask(new Point(65, 0), new Point(130, 25));
            i2.scale(2.0);
            i2.threshold(135);
            if (i2.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image"));
                String pricesStr = re.ocr(i2.getImage());
                System.out.println("OCR result: " + pricesStr);
                String[] prices = pricesStr.split(" ");
                result.add(Double.valueOf(prices[0]));
                result.add(Double.valueOf(prices[1]));
            } else {
                throw new EToreadorException("Cannot save ocr temp image: " + config.getValue("instrument.ocr.image"));
            }
        }
        return result;
    }

    public void openLong(Point pos, Recognition re, boolean dryRun, Configuration config, double stopLoss, double takeProfit)
                    throws EToreadorException, IOException, InterruptedException {
        this.open(pos, re, dryRun, config, stopLoss, takeProfit);
    }
    
    public void openShort(Point pos, Recognition re, boolean dryRun, Configuration config, double stopLoss, double takeProfit)
                    throws EToreadorException, IOException, InterruptedException {
        this.open(pos, re, dryRun, config, stopLoss, takeProfit);
    }
    
    private void open(Point pos, Recognition re, boolean dryRun, Configuration config, double stopLoss, double takeProfit)
                    throws EToreadorException, IOException, InterruptedException {
        robot.click(pos.x, pos.y);
        robot.delay(550, 50);
        Vector<Point> vec = null;
        System.out.println("Opening position");
        String screenPath;
        if (!dryRun) {
            // Fill in username and password
            System.out.println("Taking screenshot");
            screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                            config.getValue("screenshot.type"));
        } else {
            screenPath = config.getValue("test.tradedialog.path");
            System.out.println("Loading screenshot from file " + screenPath);
        }

        System.out.println("Searching for trade dialog with pattern " + config.getValue("pattern.trade.dialog.path"));
        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.trade.dialog.path"),
                        Double.valueOf(config.getValue("pattern.trade.dialog.threshold")),
                        config.getValue("test.match.trade.dialog.result")
                        );
        System.out.println("Trade dialog search result count: " + vec.size() + ", scale X: " + re.getTemplateWidth()
                        + ", scale Y: " + re.getTemplateHeight());

        if (vec.size() > 0) {
            
            Point p3 = vec.get(0);
            Point p4 = vec.get(1);
            
            Point riskLevelPlus = new Point(p4.x + ((p4.x - p3.x) / 3), p3.y + ((p4.y - p3.y) / 4));
            Image i = new Image(config.getValue("test.match.trade.dialog.result"));
            i.convertToRGB();
            i.markPoint(riskLevelPlus);
            i.save();
            robot.click(riskLevelPlus.x, riskLevelPlus.y);
            robot.delay(200, 50);
            robot.click(riskLevelPlus.x, riskLevelPlus.y);
            robot.delay(200, 50);
            //Verify risk rate
            if (!dryRun) {
                System.out.println("Taking screenshot");
                screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                                config.getValue("screenshot.type"));
            } else {
                screenPath = config.getValue("test.tradedialogx100.path");
                System.out.println("Loading screenshot from file " + screenPath);
            }
            
            System.out.println("Preparing OCR");
            i = new Image(screenPath, config.getValue("instrument.ocr.image"));
            i.crop(new Point(riskLevelPlus.x - ((p4.x - p3.x) / 3), riskLevelPlus.y - 10), new Point(riskLevelPlus.x, riskLevelPlus.y + 20));
            i.scale(2.0);
            i.threshold(135);
            if (i.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image"));
                String riskRateStr = re.ocr(i.getImage());
                System.out.println("OCR result: " + riskRateStr);
                if (!riskRateStr.equals("X100")) {
                    throw new EToreadorException("Wrong risk rate");
                }
            } else {
                throw new EToreadorException("Cannot save ocr temp image: " + config.getValue("instrument.ocr.image"));
            }
            
            Point stopLossPoint = new Point(p4.x + ((p4.x - p3.x) / 5), (int) (p3.y + ((p4.y - p3.y) / 1.4)));
            i = new Image(config.getValue("test.match.trade.dialog.result"));
            i.convertToRGB();
            i.markPoint(stopLossPoint);
            i.save();
            robot.click(stopLossPoint.x, stopLossPoint.y);
            robot.delay(200, 50);
            robot.doubleClick(stopLossPoint.x, stopLossPoint.y);
            robot.write(stopLoss + "");
            
            Point takeProfitPoint = new Point(p4.x + ((p4.x - p3.x) / 5), p4.y);
            i = new Image(config.getValue("test.match.trade.dialog.result"));
            i.convertToRGB();
            i.markPoint(takeProfitPoint);
            i.save();
            robot.click(takeProfitPoint.x, takeProfitPoint.y);
            takeProfitPoint.y -= ((p4.y - p3.y) / 10);
            robot.delay(200, 50);
            robot.doubleClick(takeProfitPoint.x, takeProfitPoint.y);
            robot.write(takeProfit + "");
            
            if (!dryRun) {
                System.out.println("Taking screenshot");
                screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                                config.getValue("screenshot.type"));
            } else {
                screenPath = config.getValue("test.tradedialogx100.path");
                System.out.println("Loading screenshot from file " + screenPath);
            }
            
            System.out.println("Searching for dialog close button with pattern "
                            + config.getValue("pattern.trade.closedialog.path"));
            vec = re.matchTemplate(
                            screenPath,
                            config.getValue("pattern.trade.closedialog.path"),
                            Double.valueOf(config.getValue("pattern.trade.closedialog.threshold")),
                            config.getValue("test.match.trade.closedialog.result")
                            );
            System.out.println("Close button search result count: " + vec.size() + ", scale X: "
                            + re.getTemplateWidth() + ", scale Y: " + re.getTemplateHeight());
            if (vec.size() > 0) {
                Point p1 = vec.get(0);
                Point p2 = vec.get(1);
                Point middle = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
                System.out.println("Closing trade dialog");
                robot.click(middle.x, middle.y);
                robot.delay(550, 22);
            } else {
                throw new EToreadorException("Closing button not found");
            }
        } else {
            throw new EToreadorException("Trade dialog not found");
        }
    }
}
