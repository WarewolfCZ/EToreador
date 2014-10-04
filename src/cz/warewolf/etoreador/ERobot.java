/**
 * 
 */
package cz.warewolf.etoreador;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * @author Denis
 *
 */
public class ERobot {
    
    private Robot robot;

    public ERobot() throws AWTException {
            this.robot = new Robot();
            this.robot.setAutoDelay(50);
    }
    
    public void write(String text) {
            int i = 0; 
            boolean shift;
//            System.out.println("ERobot.write(): writing: " + text) ;
            for (char ch: text.toUpperCase().toCharArray()) {
                    shift = false;
                    if (isUpperCase(text.charAt(i))) {
                            shift = true;
                    }
                    writeLetter(ch, shift, false);
                    i++;
            }
//            System.out.println("ERobot.write(): done writing");
    }

    private void writeLetter(char ch, boolean shift, boolean ctrl) {
            int keycode = ch;
            if (ch == 'Ì') keycode = 50;
            else if (ch == '0') keycode = KeyEvent.VK_NUMPAD0;
            else if (ch == '1') keycode = KeyEvent.VK_NUMPAD1;
            else if (ch == '2') keycode = KeyEvent.VK_NUMPAD2;
            else if (ch == '3') keycode = KeyEvent.VK_NUMPAD3;
            else if (ch == '4') keycode = KeyEvent.VK_NUMPAD4;
            else if (ch == '5') keycode = KeyEvent.VK_NUMPAD5;
            else if (ch == '6') keycode = KeyEvent.VK_NUMPAD6;
            else if (ch == '7') keycode = KeyEvent.VK_NUMPAD7;
            else if (ch == '8') keycode = KeyEvent.VK_NUMPAD8;
            else if (ch == '9') keycode = KeyEvent.VK_NUMPAD9;
            else if (ch == 'Š') keycode = 51;
            else if (ch == 'È') keycode = 52;
            else if (ch == 'Ø') keycode = 53;
            else if (ch == 'Ž') keycode = 54;
            else if (ch == 'Ý') keycode = 55;
            else if (ch == 'Á') keycode = 56;
            else if (ch == 'Í') keycode = 57;
            else if (ch == 'É') keycode = 58;
//            System.out.println(ch + ": " + keycode);
            if (ctrl) {
                    robot.keyPress(KeyEvent.VK_CONTROL);
            }
            if (shift) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
            }
            robot.keyPress(keycode);
            robot.delay(10 + getRandomInt(0, 45));
            robot.keyRelease(keycode);
            if (shift) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
            }
            if (ctrl) {
                    robot.keyRelease(KeyEvent.VK_CONTROL);
            }
            robot.delay(50 + getRandomInt(0, 150));
    }
    
    public void enter() {
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.delay(78 + getRandomInt(0, 125));
            robot.keyRelease(KeyEvent.VK_ENTER);
    }
    
    public void click(int x, int y) {
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(75 + getRandomInt(0, 8));
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
    
    public void doubleClick(int x, int y) {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(25 + getRandomInt(0, 8));
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(75 + getRandomInt(0, 8));
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(25 + getRandomInt(0, 8));
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
}
    
    public BufferedImage takeScrenshot() {
            return robot.createScreenCapture(new Rectangle(437, 218, 1130 - 437, 414 - 218));
    }

    public void delay(int delayMs, int rand) {
            robot.delay(delayMs + getRandomInt(0, rand));
    }
    
    @SuppressWarnings("static-method")
    private boolean isUpperCase(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }
    
    public static int getRandomInt(int min, int max) {
        Random ran = new Random();
        int result = ran.nextInt(max - min + 1) + min;
        return result;
}

    public Robot getRobot() {
        return this.robot;
    }

    public void mouseMove(int x, int y) {
        robot.mouseMove(x, y);
    }
}
