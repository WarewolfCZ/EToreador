/**
 * 
 */
package cz.warewolf.etoreador.img;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

/**
 * @author Denis
 * 
 */
public class Recognition {
    private static final int MAX_RESIZE_COUNT = 10;
    private boolean scaledUp;
    private double matchedTemplWidth;
    private double matchedTemplHeight;

    public Recognition() {
        this.matchedTemplWidth = 0.0;
        this.matchedTemplHeight = 0.0;
    }
    
    public double getTemplateWidth() {
        return this.matchedTemplWidth;
    }
    
    public double getTemplateHeight() {
        return this.matchedTemplHeight;
    }
    
    public Vector<java.awt.Point> matchTemplate(String screenshotFilename, String templateName, double threshold,
                    String resultPath) {
        return this.matchTemplate(screenshotFilename, templateName, threshold, resultPath, 0.0, 0.0);
    }
    
    public Vector<java.awt.Point> matchTemplate(String screenshotFilename, String templateName, double threshold,
                    String resultPath, double templSizeX, double templSizeY) {
        System.out.println("Recognition.matchTemplate(): using template " + templateName + ", threshold: " + threshold);
        Mat imageMat = Highgui.imread(screenshotFilename, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        if (imageMat.height() < 700 || imageMat.width() < 1300) {
            System.out.println("Recognition.matchTemplate(): scaling up input image");
            Imgproc.resize(imageMat, imageMat, new Size(imageMat.width() * 2, imageMat.height() * 2));
            scaledUp = true;
        }
        Vector<java.awt.Point> result = new Vector<java.awt.Point>();

        int match_method = Imgproc.TM_SQDIFF_NORMED;

        Mat resultMat = new Mat();
        Mat templMat = Highgui.imread(templateName, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        if (templSizeX > 0.0 && templSizeY > 0.0) {
            System.out.println("Recognition.matchTemplate(): Scaling template to size " + Math.ceil(templSizeX) + "x" + Math.ceil(templSizeY));
            Imgproc.resize(templMat, templMat, new Size(Math.ceil(templSizeX), Math.ceil(templSizeY)));
//            Highgui.imwrite("template_scaled_" + templSizeX + "x" + templSizeY + ".png", templMat);
        }
        
        Imgproc.threshold(templMat, templMat, 200, 0.0, Imgproc.THRESH_TOZERO);
        // Highgui.imwrite("templMat.png", templMat);
        Imgproc.threshold(imageMat, imageMat, 200, 0.0, Imgproc.THRESH_TOZERO);
        // Highgui.imwrite("imageMat.png", imageMat);
        // Localizing the best match with minMaxLoc
        double minVal;
        Point minLoc;
        Point maxLoc;
        Point matchLoc;

        for (int i = 0; i < MAX_RESIZE_COUNT; i++) {

            // Do the Matching and Normalize
            Imgproc.matchTemplate(imageMat, templMat, resultMat, match_method);

            for (int j = 0; j < 6; j++) {

                MinMaxLocResult minMaxResult = Core.minMaxLoc(resultMat);
                minVal = minMaxResult.minVal;
                minLoc = minMaxResult.minLoc;
                maxLoc = minMaxResult.maxLoc;
                System.out.println("minVal: " + minVal + ", threshold: " +
                                threshold);
                if (minVal < threshold) {
                    // System.out.println("minVal < threshold: " + minVal);
                    // For SQDIFF and SQDIFF_NORMED, the best matches are lower
                    // values. For all the other methods, the higher the better
                    if (match_method == Imgproc.TM_SQDIFF
                                    || match_method == Imgproc.TM_SQDIFF_NORMED) {
                        matchLoc = minLoc;
                    } else {
                        matchLoc = maxLoc;
                    }

                    // / Show me what you got
                    Core.rectangle(
                                    imageMat,
                                    matchLoc,
                                    new Point(matchLoc.x + templMat.cols(), matchLoc.y
                                                    + templMat.rows()), Scalar.all(100), 1, 8, 0);

                    Imgproc.floodFill(resultMat, new Mat(), matchLoc, Scalar.all(100));
                    java.awt.Point resultPoint;
                    java.awt.Point resultPoint2;
                    if (scaledUp) {
                        resultPoint = new java.awt.Point((int) matchLoc.x / 2, (int) matchLoc.y / 2);
                        resultPoint2 = new java.awt.Point((int) (matchLoc.x + templMat.cols()) / 2, (int) (matchLoc.y
                                        + templMat.rows()) / 2);
                    } else {
                        resultPoint = new java.awt.Point((int) matchLoc.x, (int) matchLoc.y);
                        resultPoint2 = new java.awt.Point((int) (matchLoc.x + templMat.cols()), (int) (matchLoc.y
                                        + templMat.rows()));
                    }

                    result.add(resultPoint);
                    result.add(resultPoint2);
                    this.matchedTemplWidth = templSizeX;
                    this.matchedTemplHeight = templSizeY;
                    // System.out.println("resultPoint: " + resultPoint);
                } else {
                    break;
                }

            }

            if (result.size() > 0) break;
            System.out.println("Template not found, scaling down template and trying again...");
            templSizeX = templMat.width() * 0.9;
            templSizeY = templMat.height() * 0.9;
            Imgproc.resize(templMat, templMat, new Size(templSizeX, templSizeY));
            // Highgui.imwrite("template_resized_" + i + ".png", templMat);
        }

        System.out.println(String.format("Writing %s", resultPath));
        Highgui.imwrite(resultPath, imageMat);
        return result;

    }

    public Vector<java.awt.Point> matchFeatures(String screenshotFilename, String templateName) {

        System.out.println("Loading screenshot " + screenshotFilename);
        Mat imageMat = Highgui.imread(screenshotFilename, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Mat templMat = Highgui.imread(templateName, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Mat resultMat = new Mat();

        Vector<java.awt.Point> result = new Vector<java.awt.Point>();

        // detecting keypoints
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.GFTT);
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        detector.detect(templMat, keypoints1);

        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        detector.detect(imageMat, keypoints2);

        // computing descriptors
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.BRIEF);
        Mat descriptors1 = new Mat();
        extractor.compute(templMat, keypoints1, descriptors1);

        Mat descriptors2 = new Mat();
        extractor.compute(imageMat, keypoints2, descriptors2);

        // matching descriptors
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);

        System.out.println("Matches: " + matches.size());

        // drawing the results
        Features2d.drawMatches(templMat, keypoints1, imageMat, keypoints2, matches, resultMat);

        // Mat H = Features2d.findHomography(Mat(points1), Mat(points2),
        // CV_RANSAC, ransacReprojThreshold);

        System.out.println(String.format("Writing %s", "feature_match_result.png"));
        Highgui.imwrite("feature_match_result.png", resultMat);
        return result;
    }

    public static void showResult(Mat img, String title) {
        Imgproc.resize(img, img, new Size(640, 480));
        MatOfByte matOfByte = new MatOfByte();
        Highgui.imencode(".jpg", img, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
            JFrame frame = new JFrame(title);
            frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String ocr(BufferedImage slice) throws IOException, InterruptedException {
        //TODO: perform ocr without saving to files
        String result = null;
        //save image so we can process it
        File ocrInputfile = new File("ocr_input.png");
        String ocrFile = "ocr_output";
        ImageIO.write(slice, "png", ocrInputfile);
        // perform ocr on file
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec("tesseract " + ocrInputfile + " " + ocrFile + " -l ces -psm 7");
        proc.waitFor();
        // read result from txt file
        List<String> lines = Files.readAllLines(Paths.get(ocrFile + ".txt"), StandardCharsets.UTF_8);
//        Files.delete(Paths.get(ocrFile+".txt"));
        Files.delete(ocrInputfile.toPath());
        if (lines.size() > 0) {
                result = lines.get(0);
        }
        return result;
}
}
