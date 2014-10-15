/**
 * 
 */
package cz.warewolf.etoreador.automation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.openqa.selenium.firefox.FirefoxDriver;

import cz.warewolf.etoreador.ERobot;
import cz.warewolf.etoreador.config.Configuration;
import cz.warewolf.etoreador.exception.EToreadorException;
import cz.warewolf.etoreador.file.FileManager;
import cz.warewolf.etoreador.img.Image;
import cz.warewolf.etoreador.img.Recognition;
import cz.warewolf.etoreador.img.Screenshot;

/**
 * @author Denis
 * 
 */
public class Automation {
    private static Point loginButtonPosition;
    private static Point usernameFieldPosition;
    private static Point passwordFieldPosition;
    private static Point welcomeMessageOkButtonPosition;
    private Configuration config;
    private ERobot robot;
    private Recognition re;
    private Point balancePosition;
    private Point equityPosition;
    private Point profitPosition;

    public Automation(ERobot robot, Configuration config) {
        this.robot = robot;
        this.config = config;
        this.re = new Recognition();
    }

    public void login(boolean dryRun) throws EToreadorException {
        FirefoxDriver driver = null;
        String screenPath = null;

        // Switch to trading platform
        if (!dryRun) {
            System.out.println("Initializing WebDriver");
            driver = new FirefoxDriver();
            this.switchToPlatform(driver, robot);
        }

        // Get first screenshot
        if (!dryRun) {
            System.out.println("Taking screenshot");
            screenPath = takeScreenshot(config.getValue("screenshot.path"), config.getValue("screenshot.type"));
        } else {
            screenPath = config.getValue("test.screenshot.path");
            System.out.println("Loading test screenshot from file " + screenPath);
        }
        // Get screen resolution
        BufferedImage screen = Image.loadFromFile(screenPath);
        double ratioY = (double) screen.getHeight() / 768;
        double ratioX = (double) screen.getWidth() / 1366;
        saveGrayscale(screenPath, config.getValue("grayscale.path"), config.getValue("grayscale.type"));

        // Find loging form
        if (this.findFormFields(screenPath, ratioX, ratioY)) {

            if (!dryRun) {
                // Fill in username and password
                this.fillInLoginForm(config, robot);
                this.submitLoginForm(robot);
                System.out.println("Taking welcome message screenshot");
                screenPath = takeScreenshot(config.getValue("screenshot.path"), config.getValue("screenshot.type"));
            } else {
                screenPath = config.getValue("test.welcomescreen.path");
                System.out.println("Loading welcome message screenshot from file " + screenPath);
            }
            // Dismiss possible welcome screen
            if (this.findWelcomeMessage(screenPath)) {
                if (!dryRun) {
                    this.dismissWelcomeMessage(robot);
                }
            }
        } else {
            throw new EToreadorException("Login form fields not found");
        }

        if (driver != null) {
            // driver.close();
        }
    }

    private void fillInLoginForm(Configuration config, ERobot robot) {
        System.out.println("Filling login form");
        robot.doubleClick(usernameFieldPosition.x, usernameFieldPosition.y);
        robot.delay(250, 20);
        robot.write(config.getValue("login"));
        robot.doubleClick(passwordFieldPosition.x, passwordFieldPosition.y);
        robot.delay(380, 20);
        robot.write(config.getValue("password"));
        takeScreenshot();
        robot.delay(520, 200);
    }

    private void submitLoginForm(ERobot robot) {
        System.out.println("Logging in");
        System.out.println("Clicking login button");
        robot.click(loginButtonPosition.x, loginButtonPosition.y);
        System.out.println("Waiting 10±2 seconds");
        robot.delay(10000, 2000);
    }

    private void dismissWelcomeMessage(ERobot robot) {
        System.out.println("Dismissing welcome message");
        robot.delay(250, 20);
        robot.click(welcomeMessageOkButtonPosition.x, welcomeMessageOkButtonPosition.y);
        robot.delay(250, 20);
    }

    private boolean findWelcomeMessage(String screenPath) {
        boolean result = false;
        // Check if there is welcome message
        System.out.println("Searching for welcome message");
        Vector<Point> vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.welcome.path"),
                        Double.valueOf(config.getValue("pattern.welcome.threshold")),
                        config.getValue("test.match.welcome.result")
                        );
        System.out.println("Welcome screen search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Welcome screen found");
            Point p1 = vec.get(0);
            Point p2 = vec.get(1);
            Point middle = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);

            welcomeMessageOkButtonPosition = middle;
            welcomeMessageOkButtonPosition.y += ((p1.y + p2.y) / 5);
            Image i = new Image(config.getValue("test.match.welcome.result"));
            i.convertToRGB();
            i.markPoint(welcomeMessageOkButtonPosition);
            i.save();
            result = true;
        }
        return result;
    }

    public static void saveGrayscale(String screenPath, String grayscalePath, String grayscaleType)
                    throws EToreadorException {
        // Convert screenshot to grayscale and save it
        Image img = new Image(screenPath);
        BufferedImage gray = img.getGrayscale();
        if (gray != null) {
            System.out.println("Screenshot converted to grayscale");
            FileManager fm = new FileManager();
            if (fm.saveImage(gray, grayscalePath, grayscaleType)) {
                System.out.println("Grayscale image saved to file ");
            } else {
                throw new EToreadorException("Error while saving grayscale image");
            }
        } else {
            throw new EToreadorException("Error converting to grayscale");
        }
    }

    private void switchToPlatform(FirefoxDriver driver, ERobot robot) throws EToreadorException {
        System.out.println("Opening trading platform");
        driver.get("https://www.etoro.com/webtrader/");
        robot.delay(500, 10);
        driver.manage().window().maximize();
        String pageTitle = driver.getTitle();
        if (pageTitle.equals("WebTrader 2.0 - the leading online trading platform | eToro")) {
            System.out.println("Opened WebTrader window");
        } else {
            driver.close();
            throw new EToreadorException("Error: page title is: " + pageTitle);
        }
        System.out.println("Waiting for page to load completely");
        robot.delay(5000, 535);
    }

    private boolean findFormFields(String screenPath, double ratioX, double ratioY)
                    throws EToreadorException {
        boolean result = false;
        Vector<Point> vec = null;

        // Verify that there is login form on the page
        System.out.println("Searching for login form");

        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.loginbutton.path"),
                        Double.valueOf(config.getValue("pattern.loginbutton.threshold")),
                        config.getValue("test.match.login.result")
                        );
        System.out.println("Login button search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Login form found");
            // login form found
            loginButtonPosition = vec.get(0);
            loginButtonPosition.x += (10 * ratioX);
            loginButtonPosition.y += (10 * ratioY);
            Image i = new Image(config.getValue("grayscale.path"));
            i.markPoint(loginButtonPosition);
            i.save();
        } else {
            throw new EToreadorException("Login form not found");
        }

        // Find username field
        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.username.path"),
                        Double.valueOf(config.getValue("pattern.username.threshold")),
                        config.getValue("test.match.username.result")
                        );
        System.out.println("Username field search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Username form field found");
            usernameFieldPosition = vec.get(0);
            usernameFieldPosition.x += (30 * ratioX);
            usernameFieldPosition.y += (90 * ratioY);
            Image i = new Image(config.getValue("grayscale.path"));
            i.markPoint(usernameFieldPosition);
            i.save();
        } else {
            throw new EToreadorException("Username form field not found");
        }

        // Find password field
        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.password.path"),
                        Double.valueOf(config.getValue("pattern.password.threshold")),
                        config.getValue("test.match.password.result")
                        );
        System.out.println("Password field search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Password form field found");
            passwordFieldPosition = vec.get(0);
            passwordFieldPosition.x += (40 * ratioX);
            passwordFieldPosition.y += (45 * ratioY);
            Image i = new Image(config.getValue("grayscale.path"));
            i.markPoint(passwordFieldPosition);
            i.save();
            result = true;
        } else {
            throw new EToreadorException("Password form field not found");
        }
        return result;
    }

    public static String takeScreenshot() {
        String type = "png";
        String name = "screenshot" + (Calendar.getInstance().getTimeInMillis()) + "." + type;
        return takeScreenshot(name, type);
    }

    public static String takeScreenshot(String name, String type) {
        Screenshot sc;
        String result = null;
        try {
            sc = new Screenshot(new Robot());
            BufferedImage screen = sc.takeScreenshot();
            FileManager fm = new FileManager();

            fm.saveImage(screen, name, type);
            System.out.println("Screenshot " + name + " taken");
            result = name;
        } catch (AWTException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void takeScreenshotAndExit() {
        Screenshot sc;
        try {
            sc = new Screenshot(new Robot());
            BufferedImage screen = sc.takeScreenshot();
            FileManager fm = new FileManager();
            fm.saveImage(screen, "error_screen" + (Calendar.getInstance().getTimeInMillis() / 1000) + ".png", "png");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File("scream_male.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            Robot r = new Robot();
            r.delay(1500);
        } catch (AWTException | UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            System.exit(1);
        }
    }

    public double getAccountBalance(String screenPath) throws EToreadorException, IOException, InterruptedException {
        double result = 0.0;
        Vector<Point> vec = null;

        System.out.println("Searching for account balance");
        // Get screen resolution
        BufferedImage screen = Image.loadFromFile(screenPath);
        double ratioY = (double) screen.getHeight() / 768;
        double ratioX = (double) screen.getWidth() / 1366;

        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.balance.path"),
                        Double.valueOf(config.getValue("pattern.balance.threshold")),
                        config.getValue("test.match.balance.result")
                        );
        System.out.println("Balance search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Balance found");
            balancePosition = vec.get(0);
            balancePosition.x += (85 * ratioX);
            balancePosition.y -= (15 * ratioY);

            Point p3 = new Point((int) (balancePosition.x - (50 * ratioX)), (int) (balancePosition.y - (11 * ratioY)));
            Point p4 = new Point((int) (balancePosition.x + (55 * ratioX)), (int) (balancePosition.y + (15 * ratioY)));
            Image i = new Image(config.getValue("test.match.balance.result"));
            i.convertToRGB();
            i.markPoint(balancePosition);
            i.save();

            System.out.println("Preparing OCR");
            Image img = new Image(screenPath);
            BufferedImage ocrImg = img.crop(p3, p4);
            Image i2 = new Image(ocrImg, config.getValue("instrument.ocr.image"));
            // i2.mask(new Point(65, 0), new Point(130, 25));
            i2.scale(2.0);
            i2.threshold(135);
            if (i2.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image"));
                String balanceStr = re.ocr(i2.getImage());
                System.out.println("OCR result: " + balanceStr);
                balanceStr = balanceStr.replace("$", "").replace(",", "").replace("O", "0");
                if (balanceStr.length() <= 7) {
                    balanceStr = balanceStr.replace(".", "");
                }
                result = Double.valueOf(balanceStr);
            } else {
                throw new EToreadorException("Cannot save ocr temp image: " + config.getValue("instrument.ocr.image"));
            }
        } else {
            throw new EToreadorException("Balance information not found");
        }

        return result;
    }

    public double getEquity(String screenPath) throws IOException, InterruptedException, EToreadorException {
        double result = 0.0;
        Vector<Point> vec = null;

        System.out.println("Searching for equity");
        // Get screen resolution
        BufferedImage screen = Image.loadFromFile(screenPath);
        double ratioY = (double) screen.getHeight() / 768;
        double ratioX = (double) screen.getWidth() / 1366;

        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.equity.path"),
                        Double.valueOf(config.getValue("pattern.equity.threshold")),
                        config.getValue("test.match.equity.result")
                        );
        System.out.println("Equity search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Equity found");
            equityPosition = vec.get(0);
            equityPosition.x += (165 * ratioX);
            equityPosition.y -= (15 * ratioY);

            Point p3 = new Point((int) (equityPosition.x - (28 * ratioX)), (int) (equityPosition.y - (5 * ratioY)));
            Point p4 = new Point((int) (equityPosition.x + (160 * ratioX)), (int) (equityPosition.y + (20 * ratioY)));
            Image i = new Image(config.getValue("test.match.equity.result"));
            i.convertToRGB();
            i.markPoint(equityPosition);
            i.save();

            System.out.println("Preparing OCR");
            Image img = new Image(screenPath);
            BufferedImage ocrImg = img.crop(p3, p4);
            Image i2 = new Image(ocrImg, config.getValue("instrument.ocr.image"));
            i2.scale(2.0);
            i2.threshold(158);
            if (i2.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image"));
                String equityStr = re.ocr(i2.getImage());
                System.out.println("OCR result: " + equityStr);
                equityStr = equityStr.replace("$", "").replace(",", "").replace("O", "0");

                result = Double.valueOf(equityStr);
            } else {
                throw new EToreadorException("Cannot save ocr temp image: " + config.getValue("instrument.ocr.image"));
            }
        } else {
            throw new EToreadorException("Equity information not found");
        }

        return result;
    }

    public double getNetProfit(String screenPath) throws IOException, InterruptedException, EToreadorException {
        double result = 0.0;
        Vector<Point> vec = null;

        System.out.println("Searching for net profit");
        // Get screen resolution
        BufferedImage screen = Image.loadFromFile(screenPath);
        double ratioY = (double) screen.getHeight() / 768;
        double ratioX = (double) screen.getWidth() / 1366;

        vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.profit.path"),
                        Double.valueOf(config.getValue("pattern.profit.threshold")),
                        config.getValue("test.match.profit.result")
                        );
        System.out.println("Net profit search result count: " + vec.size());

        if (vec.size() > 0) {
            System.out.println("Net profit found");
            profitPosition = vec.get(0);
            profitPosition.x += (85 * ratioX);
            profitPosition.y -= (15 * ratioY);

            Point p3 = new Point((int) (profitPosition.x - (20 * ratioX)), (int) (profitPosition.y - (5 * ratioY)));
            Point p4 = new Point((int) (profitPosition.x + (85 * ratioX)), (int) (profitPosition.y + (20 * ratioY)));
            Image i = new Image(config.getValue("test.match.profit.result"));
            i.convertToRGB();
            i.markPoint(profitPosition);
            i.save();

            System.out.println("Preparing OCR");
            Image img = new Image(screenPath);
            BufferedImage ocrImg = img.crop(p3, p4);
            Image i2 = new Image(ocrImg, config.getValue("instrument.ocr.image"));
            i2.scale(2.0);
            i2.threshold(158);
            if (i2.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image"));
                String profitStr = re.ocr(i2.getImage());
                System.out.println("OCR result: " + profitStr);
                profitStr = profitStr.replace("$", "").replace(",", "").replace("O", "0");

                result = Double.valueOf(profitStr);
            } else {
                throw new EToreadorException("Cannot save ocr temp image: " + config.getValue("instrument.ocr.image"));
            }
        } else {
            throw new EToreadorException("Net profit information not found");
        }

        return result;
    }

    public void captureHistoricalData(boolean dryRun) throws IOException, InterruptedException, EToreadorException,
                    ParseException {
        boolean loop = true;
        Date previousTime = null;
        FileManager fm = new FileManager();
        double scale = 4.2;
        int threshold = 149;
        String screenPath;
        String ocrOrig;
        Point p3 = null, p4 = null;
        if (!dryRun) {
            System.out.println("Taking screenshot!!");
            screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                            config.getValue("screenshot.type"));
        } else {
            screenPath = config.getValue("test.capture.path");
            System.out.println("Loading screenshot from file " + screenPath);
        }

        System.out.println("Searching for share button with pattern " + config.getValue("pattern.capture.path"));
        Vector<Point> vec = re.matchTemplate(
                        screenPath,
                        config.getValue("pattern.capture.path"),
                        Double.valueOf(config.getValue("pattern.capture.threshold")),
                        config.getValue("test.match.capture.result")
                        );
        System.out.println("Share button search result count: " + vec.size() + ", scale X: "
                        + re.getTemplateWidth()
                        + ", scale Y: " + re.getTemplateHeight());

        if (vec.size() > 0) {
            p3 = vec.get(0);
            p4 = vec.get(1);
        }

        while (loop) {

            if (!dryRun) {
                System.out.println("Taking screenshot");
                screenPath = Automation.takeScreenshot(config.getValue("screenshot.path"),
                                config.getValue("screenshot.type"));
            } else {
                screenPath = config.getValue("test.capture.path");
                System.out.println("Loading screenshot from file " + screenPath);
            }

            double open, close, high, low;
            Date time;
            Point p1 = new Point((int) (p3.x - ((p4.x - p3.x) * 2.5)), p3.y);
            Point p2 = new Point((int) (p3.x + ((p4.x - p3.x) * 0.1)), p4.y - 30);
            System.out.println("Preparing OCR");
            Image i = new Image(screenPath, config.getValue("instrument.ocr.image"));
            i.crop(p1, p2);
            i.scale(scale);
            i.threshold(threshold);
            if (i.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image"));
                String ocrResult = re.ocr(i.getImage());
                System.out.println("OCR result: '" + ocrResult + "'");
                ocrOrig = ocrResult;
                ocrResult = ocrResult.trim().replace(", ", ".").replace(",", ".").replace("'", "").replace("-", "").replace("“", "")
                                .replace("_", "").trim();
                ocrResult = ocrResult.replace("  ", " ").replace("O", "0").replace("0ct", "Oct").replace(" :", ":")
                                .replace(": ", ":").replace("ö", "6");
                ocrResult = ocrResult.replace("0Ct", "Oct").replace("0CT", "Oct").replace("0cT", "Oct");
                System.out.println("OCR result modified: '" + ocrResult + "'");
                String[] tmp = ocrResult.split(" ", 3);
                open = Double.valueOf(tmp[0].replace(" ", ""));
                high = Double.valueOf(tmp[1].replace(" ", ""));
                tmp[2] = tmp[2].substring(0, tmp[2].length() - 5) + " "
                                + tmp[2].substring(tmp[2].length() - 5, tmp[2].length() - 3) + ":"
                                + tmp[2].substring(tmp[2].length() - 2);
                tmp[2] = tmp[2].replace("  ", " ").replace(" :", ":").replace(": ", ":").replace("I", "1");
                System.out.println("date string modified: '" + tmp[2] + "'");
                try {
                    SimpleDateFormat parserSDF = new SimpleDateFormat("yyyy MMM dd HH:mm", Locale.US);
                    time = parserSDF.parse("2014 " + tmp[2]);
                } catch (ParseException e) {
                    try {
                        SimpleDateFormat parserSDF = new SimpleDateFormat("yyyy MMM ddHH:mm", Locale.US);
                        time = parserSDF.parse("2014 " + tmp[2]);
                    } catch (ParseException e2) {
                        try {
                            SimpleDateFormat parserSDF = new SimpleDateFormat("yyyy MMMdd HH:mm", Locale.US);
                            time = parserSDF.parse("2014 " + tmp[2]);
                        } catch (ParseException e3) {
                            try {
                                SimpleDateFormat parserSDF = new SimpleDateFormat("yyyy MMMddHH:mm", Locale.US);
                                time = parserSDF.parse("2014 " + tmp[2]);
                            } catch (ParseException e4) {
                                throw e4;
                            }
                        }
                    }
                }

            } else {
                throw new EToreadorException("Cannot save ocr temp image: "
                                + config.getValue("instrument.ocr.image"));
            }

            p1 = new Point((int) (p3.x - ((p4.x - p3.x) * 2.5)), p3.y + 35);
            p2 = new Point((int) (p3.x - ((p4.x - p3.x) * 1.2)), p4.y);
            System.out.println("Preparing OCR");
            i = new Image(screenPath, config.getValue("instrument.ocr.image2"));
            i.crop(p1, p2);
            i.scale(scale);
            i.threshold(threshold);
            if (i.save()) {
                System.out.println("Starting OCR on image " + config.getValue("instrument.ocr.image2"));
                String ocrResult = re.ocr(i.getImage());
                System.out.println("OCR result: '" + ocrResult + "'");
                ocrOrig += " " + ocrResult;
                ocrResult = ocrResult.trim().replace(", ", ".").replace(",", ".").replace("'", "").replace("-", "")
                                .replace("_", "").trim();
                ocrResult = ocrResult.replace("  ", " ").replace("O", "0").replace("0ct", "Oct").replace("ö", "6");
                System.out.println("OCR result modified: '" + ocrResult + "'");
                String[] tmp = ocrResult.split(" ", 2);
                close = Double.valueOf(tmp[0].replace(" ", ""));
                low = Double.valueOf(tmp[1].replace(" ", ""));
            } else {
                throw new EToreadorException("Cannot save ocr temp image: "
                                + config.getValue("instrument.ocr.image2"));
            }
            System.out.println("Open: " + open + ", close: " + close + ", high: " + high + ", low: " + low
                            + ", time: " + time);
            fm.appendToTxtFile(config.getValue("capture.log"), open + ";" + close + ";" + high + ";" + low + ";"
                            + time.getTime() + ";" + time + ";" + ocrOrig + "\n");
            if (previousTime != null && previousTime.equals(time)) {
                System.out.println("Time is the same, exiting data capturing");
                loop = false;
            } else {
                System.out.println("Pressing key Right");
//                robot.click();
                robot.keyPress(KeyEvent.VK_RIGHT);
//                robot.delay(100, 0);
                previousTime = time;
            }

        }
    }
}
