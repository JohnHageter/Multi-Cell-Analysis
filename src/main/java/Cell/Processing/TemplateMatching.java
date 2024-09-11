package Cell.Processing;

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.presets.javacpp;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import static org.bytedeco.opencv.global.opencv_core.CV_32F; // Use this constant for 32-bit float
import static org.bytedeco.opencv.global.opencv_imgproc.matchTemplate;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_TM_CCOEFF;

public class TemplateMatching {

    public static FloatProcessor doMatch(ImageProcessor src, ImageProcessor tpl, boolean showR) {
        Loader.load(javacpp.class);
        Loader.load(opencv_core.class);

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

        Mat matSrc = convertToMat(src);
        Mat matTpl = convertToMat(tpl);
        Mat res = new Mat(new Size(srcW - tplW + 1, srcH - tplH + 1), CV_32F);

        matchTemplate(matSrc, matTpl, res, CV_TM_CCOEFF);

        FloatProcessor resultFp = new FloatProcessor(res.cols(), res.rows());
        float[] resultPixels = (float[]) resultFp.getPixels(); // Get the float pixels array

        FloatPointer floatPointer = new FloatPointer(res.data().asByteBuffer().asFloatBuffer());
        floatPointer.get(resultPixels);

        matSrc.release();
        matTpl.release();
        res.release();

        return resultFp;
    }


    private static Mat convertToMat(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        if (ip instanceof FloatProcessor) {
            return new Mat(height, width, CV_32F, new FloatPointer((float[]) ip.getPixels()));
        } else if (ip instanceof ByteProcessor) {
            byte[] pixels = (byte[]) ip.getPixels();
            float[] floatPixels = new float[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                floatPixels[i] = (float) (pixels[i] & 0xFF);
            }
            return new Mat(height, width, CV_32F, new FloatPointer(floatPixels));
        } else {
            throw new IllegalArgumentException("Unsupported ImageProcessor type: " + ip.getClass().getName());
        }
    }
}
