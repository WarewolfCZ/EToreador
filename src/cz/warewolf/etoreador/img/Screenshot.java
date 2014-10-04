/**
 * 
 */
package cz.warewolf.etoreador.img;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

/**
 * @author Denis
 * 
 */
public class Screenshot {
    private Robot robot;

    public Screenshot(Robot robot) {
        this.robot = robot;
    }

    /**
     * Take screenshot of whole screen
     * @return BufferedImage <br> 
     * Null on failure
     */
    public BufferedImage takeScreenshot() {
        BufferedImage result = null;
        Rectangle screenRectangle = this.getScreenRectangle();
        if (screenRectangle != null) {
            result = robot.createScreenCapture(screenRectangle);
        }
        return result;
    }

    /**
     * Get screen size
     * 
     * @return Rectangle
     */
    public Rectangle getScreenRectangle() {
        Rectangle result = null;
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            int width = gd.getDisplayMode().getWidth();
            int height = gd.getDisplayMode().getHeight();
            result = new Rectangle(width, height);
        } catch (HeadlessException e) {
            System.err.println("Cannot take screenshot because running in headless mode!");
        }

        return result;
    }
}
