package cz.warewolf.etoreador.img;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Image manipulation
 * 
 * @author Denis
 * 
 */
public class Image {

    private BufferedImage img;
    private String path;

    public Image(BufferedImage screen) {
        this.img = screen;
        this.path = null;
    }

    public Image(BufferedImage screen, String path) {
        this.img = screen;
        this.path = path;
    }

    public Image(String imgPath) {
        this.img = Image.loadFromFile(imgPath);
        this.path = imgPath;
    }

    public Image(String origPath, String path) {
        this.img = loadFromFile(origPath);
        this.path = path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Convert image to grayscale and return result
     * 
     * @return BufferedImage
     */
    public BufferedImage getGrayscale() {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = result.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * Load image from file
     * 
     * @param path
     *            path to image
     * @return BufferedImage
     */
    public static BufferedImage loadFromFile(String path) {
        BufferedImage result = null;
        File f = null;
        try {
            f = new File(path);
            result = ImageIO.read(f);
        } catch (IOException e) {
            System.err.println("Input file path: " + path);
            if (f != null) System.err.println("Input file absolute path: " + f.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }
    
    public boolean markPoint(Point p) {
        return this.markPoint(p, Color.red);
    }

    public boolean markPoint(Point p, Color c) {
        boolean result = false;
        if (p != null && this.img != null) {
            Graphics2D g2d = this.img.createGraphics();

            g2d.setColor(c);
            g2d.fillOval(p.x, p.y, 10, 10);
            g2d.dispose();
            System.out.println("Made mark at x: " + p.x + ", y:" + p.y);
            result = true;
        }
        return result;
    }
    
    

    public boolean convertToRGB() {
        boolean result = false;
        if (this.img != null) {
            int width = this.img.getWidth();
            int height = this.img.getHeight();

            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics g = newImage.getGraphics();
            g.drawImage(this.img, 0, 0, null);
            g.dispose();

            this.img = newImage;
            System.out.println("Converted image to RGB");
            result = true;
        }
        return result;
    }

    public boolean save() {
        boolean result = false;
        if (this.path != null && this.img != null) {
            try {
                File outputfile = new File(this.path);
                ImageIO.write(this.img, "png", outputfile);
                result = true;
                System.out.println("Saved image to file " + outputfile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public BufferedImage crop(Point p1, Point p2) {
        System.out.println("Image.crop() p1: " + p1 + ", p2: " + p2);
        BufferedImage result = null;
        if (p1.x < p2.x && p1.y < p2.y) {
            result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics g = result.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            result = result.getSubimage(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
            this.img = result;
        }
        return result;
    }

    public void scale(double d) {
        if (this.img != null) {
            int newWidth = new Double(this.img.getWidth() * d).intValue();
            int newHeight = new Double(this.img.getHeight() * d).intValue();
            System.out.println("Image.scale(): newWidth: " + newWidth + ", newHeight: " + newHeight);
            BufferedImage resized = new BufferedImage(newWidth, newHeight, this.img.getType());
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(this.img, 0, 0, newWidth, newHeight, 0, 0, this.img.getWidth(), this.img.getHeight(), null);
            g.dispose();
            this.img = resized;
        }
    }

    public void mask(Point p1, Point p2) {
        if (this.img != null && p1.x < p2.x && p1.y < p2.y) {
            System.out.println("Image.mask(): p1: " + p1 + ", p2: " + p2);
            Graphics2D g = this.img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
    }

    public BufferedImage getImage() {
        return this.img;
    }

    public BufferedImage threshold(int threshold) {
        BufferedImage result = new BufferedImage(this.img.getWidth(), this.img.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY);
        result.getGraphics().drawImage(this.img, 0, 0, null);
        WritableRaster raster = result.getRaster();
        int[] pixels = new int[this.img.getWidth()];
        for (int y = 0; y < this.img.getHeight(); y++) {
            raster.getPixels(0, y, this.img.getWidth(), 1, pixels);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] < threshold)
                    pixels[i] = 0;
                else
                    pixels[i] = 255;
            }
            raster.setPixels(0, y, this.img.getWidth(), 1, pixels);
            this.img = result;
        }
        return result;
    }
}
