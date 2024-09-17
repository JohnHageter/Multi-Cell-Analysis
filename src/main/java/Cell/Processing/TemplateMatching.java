package Cell.Processing;

import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;

import static ijopencv.ij.ImagePlusMatConverter.toMat;
import static org.bytedeco.javacpp.opencv_imgproc.CV_TM_CCOEFF;
import static org.bytedeco.javacpp.opencv_imgproc.matchTemplate;

public class TemplateMatching {

    public static FloatProcessor doMatch(ImageProcessor src, ImageProcessor tpl, boolean showR) {
        //Loader.load(opencv_core.class);

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

        opencv_core.Mat matSrc = toMat(src);
        opencv_core.Mat matTpl = toMat(tpl);
        Mat res = new Mat(new opencv_core.Size(srcW - tplW + 1, srcH - tplH + 1));

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
}
