package cz.warewolf.etoreador.file;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;

/**
 * 
 * @author Denis
 * 
 */
public class FileManager {

    public enum ImageType {
        PNG("png"), JPG("jpg");

        private String value;

        private ImageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Save image to specified path
     * 
     * @param img
     *            BufferedImage
     * @param path
     *            path to output file
     * @return <b>true</b> if image saved
     */
    public boolean saveImage(BufferedImage img, String path, String type) {
        boolean result = false;
        if (img != null && path != null && type != null) {
            File file = new File(path);
            try {
                result = ImageIO.write(img, type, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean appendToTxtFile(String path, String text) throws IOException {
        boolean result = false;
        if (path != null && text != null) {
            File file = new File(path);
            file.createNewFile();
            Files.write(file.toPath(), text.getBytes(), StandardOpenOption.APPEND);
            result = true;
        }
        return result;
    }
}
