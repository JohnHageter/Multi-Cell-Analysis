package Cell.Utils;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.ShortPointer;

import static org.bytedeco.opencv.global.opencv_core.*;

public class ImageCV {
    public static Mat toMat(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        int width = ip.getWidth();
        int height = ip.getHeight();

        if (ip.getBitDepth() == 8) {
            byte[] pixels = (byte[]) ip.getPixels();
            return new Mat(height, width, CV_8U, new BytePointer(pixels));
        } else if (ip.getBitDepth() == 16) {
            //TODO: Find a way for 16 bit images to be processed
            IJ.error("Motion correction cannot be applied to 16-bit images.");
        } else if (ip.getBitDepth() == 32){
            float[] pixels = (float[]) ip.getPixels();
            return new Mat(height, width, CV_32F, new FloatPointer(pixels));
        } else if (ip.getBitDepth() == 24) {
            Mat mat = new Mat(height, width, CV_8UC3);
            byte[] pixels = (byte[]) ip.getPixels();
            byte[] bgrPixels = new byte[pixels.length];
            for (int i = 0; i < pixels.length; i += 3) {
                bgrPixels[i] = pixels[i + 2];      // B
                bgrPixels[i + 1] = pixels[i + 1];  // G
                bgrPixels[i + 2] = pixels[i];      // R
            }
            mat.data().put(bgrPixels);
        } else {
            throw new IllegalArgumentException("Unsupported image type");
        }
        return null;
    }

    public static Mat toMat(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        if (ip.getBitDepth() == 8) {
            byte[] pixels = (byte[]) ip.getPixels();
            return new Mat(height, width, CV_8UC1, new BytePointer(pixels));
        } else if (ip.getBitDepth() == 16) {
            //TODO: Find a way for 16 bit images to be processed
            IJ.error("Motion correction cannot be applied to 16-bit images");
        } else if (ip.getBitDepth() == 32){
            float[] pixels = (float[]) ip.getPixels();
            return new Mat(height, width, CV_32F, new FloatPointer(pixels));
        } else if (ip.getBitDepth() == 24) {
            Mat mat = new Mat(height, width, CV_8UC3);
            byte[] pixels = (byte[]) ip.getPixels();
            byte[] bgrPixels = new byte[pixels.length];
            for (int i = 0; i < pixels.length; i += 3) {
                bgrPixels[i] = pixels[i + 2];      // B
                bgrPixels[i + 1] = pixels[i + 1];  // G
                bgrPixels[i + 2] = pixels[i];      // R
            }
            mat.data().put(bgrPixels);
        } else {
            throw new IllegalArgumentException("Unsupported image type");
        }
        return null;
    }
}
