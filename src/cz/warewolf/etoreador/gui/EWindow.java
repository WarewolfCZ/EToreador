/**
 * 
 */
package cz.warewolf.etoreador.gui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

/**
 * @author Denis
 * 
 */
public class EWindow {

    private static JFrame frame;
    private static JLabel label;

    public static void setText(String text) {
        if (label == null) label = new JLabel();
        label.setText(text);
    }
    
    public static void show() {
        if (frame == null) frame = new JFrame("EToreador");
        if (label == null) label = new JLabel("              EToreador running          ");
        // Set's the window to be "always on top"
        frame.setAlwaysOnTop(true);

        frame.setLocationByPlatform(true);
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static void hide() {
        frame.dispose();
    }
}
