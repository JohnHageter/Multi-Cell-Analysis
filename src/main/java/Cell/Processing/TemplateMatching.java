package Cell.Processing;

import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.opencv_core.CvMat;
import org.bytedeco.opencv.opencv_core.IplImage;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.cvMatchTemplate;

public class TemplateMatching {

    public static FloatProcessor doMatch(ImageProcessor src, ImageProcessor tpl, boolean showR) {
        if (src == null || tpl == null) {
            IJ.error("Source or template image is null.");
            return null;
        }

        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int tplW = tpl.getWidth();
        int tplH = tpl.getHeight();

        if (tplW > srcW || tplH > srcH) {
            IJ.error("Template size must be smaller than source image size.");
            return null;
        }

        IplImage iplSrc = null;
        IplImage iplTpl = null;
        IplImage res = cvCreateImage(cvSize(srcW - tplW + 1, srcH - tplH + 1), IPL_DEPTH_32F, 1);
        FloatProcessor resultFp = null;

        switch (src.getBitDepth()) {
            case 32:
                iplSrc = convertToIplImage(src);
                iplTpl = convertToIplImage(tpl);
                break;
            case 16:
                iplSrc = convert16BitTo32Bit(src);
                iplTpl = convert16BitTo32Bit(tpl);
                break;
            case 8:
                iplSrc = convertBufferedImageToIplImage(src.getBufferedImage());
                iplTpl = convertBufferedImageToIplImage(tpl.getBufferedImage());
                break;
            default:
                IJ.error("Unsupported image type");
                return null;
        }

        int method = 4;
        cvMatchTemplate(iplSrc, iplTpl, res, method);
        FloatBuffer fb = res.getFloatBuffer();
        float[] f = new float[res.width() * res.height()];
        fb.get(f, 0, f.length);
        resultFp = new FloatProcessor(res.width(), res.height(), f, null);

        // Release resources
        cvReleaseImage(res);
        releaseIplImage(iplSrc);
        releaseIplImage(iplTpl);

        return resultFp;
    }

    private static IplImage convertToIplImage(ImageProcessor ip) {
        CvMat mat = CvMat.create(ip.getHeight(), ip.getWidth(), CV_32FC1);
        double[] arr = float2DtoDouble1DArray(ip.getFloatArray(), ip.getWidth(), ip.getHeight());
        mat.put(0, arr, 0, arr.length);
        return mat.asIplImage();
    }

    private static IplImage convert16BitTo32Bit(ImageProcessor src) {
        BufferedImage bi = ((ShortProcessor) src).get16BitBufferedImage();
        IplImage iplImage = cvCreateImage(cvSize(src.getWidth(), src.getHeight()), IPL_DEPTH_32F, 1);
        IplImage temp = convertBufferedImageToIplImage(bi);
        cvConvertScale(temp, iplImage, 1.0 / 65535.0, 0);
        temp.release();
        return iplImage;
    }

    private static IplImage convertBufferedImageToIplImage(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        IplImage iplImage = cvCreateImage(cvSize(width, height), IPL_DEPTH_8U, 3);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = bi.getRGB(x, y);
                byte[] bgr = new byte[3];
                bgr[0] = (byte) (rgb & 0xFF);          // B
                bgr[1] = (byte) ((rgb >> 8) & 0xFF);   // G
                bgr[2] = (byte) ((rgb >> 16) & 0xFF);  // R
                iplImage.imageData().position((long) y * iplImage.widthStep() + x * 3L).put(bgr); // Set pixel in IplImage
            }
        }

        return iplImage;
    }

    private static void releaseIplImage(IplImage img) {
        if (img != null) {
            cvReleaseImage(img);
        }
    }

    private static double[] float2DtoDouble1DArray(float[][] arr2d, int column, int row) {
        double[] arr1d = new double[column * row];
        for (int y = 0; y < row; y++) {
            for (int x = 0; x < column; x++) {
                arr1d[y * column + x] = (double) arr2d[y][x]; // Fix the pixel access order
            }
        }
        return arr1d;
    }
}
