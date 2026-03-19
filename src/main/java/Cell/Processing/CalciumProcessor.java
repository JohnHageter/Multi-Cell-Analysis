package Cell.Processing;

import ij.ImagePlus;
import Cell.Utils.Utils;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;
import ij.IJ;

public class CalciumProcessor {
    public CalciumProcessor() {}

    public void convertToDF(ImagePlus imp, int baselineBegin, int baselineEnd) {

        if (!Utils.isStack(imp)) {
            throw new IllegalArgumentException("Input image must be a stack.");
        }

        ImageStack deltaFStack = new ImageStack();
        ImageStack stack = imp.getImageStack();

        int height = stack.getHeight();
        int width = stack.getWidth();
        int depth = stack.getSize();
        ImagePlus baselineImp = getBaseline(imp, baselineBegin, baselineEnd);
        baselineImp.show();

        for (int slice = 1; slice <= depth; slice++) {
            ImageProcessor ip = stack.getProcessor(slice).duplicate();
            FloatProcessor deltaFProcessor = new FloatProcessor(width, height);
            ImageProcessor baselineIp = baselineImp.getProcessor();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double pixelValue = ip.getPixelValue(x, y);
                    double baselineValue = baselineIp.getPixelValue(x, y);

                    double dfValue = (pixelValue - baselineValue) / baselineValue;
                    deltaFProcessor.putPixelValue(x, y, dfValue);
                }
            }

            deltaFStack.addSlice(deltaFProcessor);
        }

        ImagePlus ret = new ImagePlus(imp.getTitle() + "_DELTAF", deltaFStack);
        ret.show();
    }

    private ImagePlus getBaseline(ImagePlus imp, int baselineBegin, int baselineEnd) {
        ImageStack stack = imp.getStack();
        int width = stack.getWidth();
        int height = stack.getHeight();

        float[] sumPixels = new float[width * height];
        double baselineDuration = baselineEnd - baselineBegin;

        for (int z = baselineBegin; z <= baselineEnd; z++) {
            ImageProcessor ip = stack.getProcessor(z);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width) + x;
                    sumPixels[index] += ip.getPixelValue(x, y);
                }
            }
        }

        FloatProcessor baselineIp = new FloatProcessor(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                baselineIp.putPixelValue(x, y, sumPixels[index] / baselineDuration);
            }
        }

        return new ImagePlus(imp.getTitle() + "_BASELINE", baselineIp);
    }


    /*
FUNCTION stackConvert8Bit(image):
    SAVE calibration from image
    REMOVE calibration from image
    
    stack = image.getStack()
    width = stack.width
    height = stack.height
    size = number of slices in stack
    
    globalMin = +infinity
    globalMax = -infinity
    
    // Find minimum and maximum pixel value across every slice in the stack.
    FOR each slice i from 1 to size:
        processor = duplicate of stack.getProcessor(i)
    
        FOR each pixel (x, y) in processor:
            value = processor.getPixelValue(x, y)
    
            IF value < globalMin:
                globalMin ← value
            IF value > globalMax:
                globalMax ← value
    
    newStack ← empty stack with same width and height
    
    // Use the global min and max to scale 16-bit values to 8-bit
    FOR each slice i from 1 to size:
        processor = duplicate of stack.getProcessor(i)
    
        CREATE byte array dst of size (width * height)
        index = 0
    
        FOR each pixel (x, y) in processor:
            value = processor.getPixelValue(x, y)
    
            // scale value to 0–255
            scaled = (value - globalMin) * 255 / range
    
            // safety: if by chance a pixel in the raw image is NaN or infinity
            // make sure it's clamped to the 8-bit range
            IF scaled < 0:
                scaled = 0
            IF scaled > 255:
                scaled = 255
    
            //put the scaled pixel in the new stack
            dst[index] = round(scaled)
            index = index + 1
    
        ADD dst as new slice to newStack
    
    // replace the 16-bit stack with the new 8-bit stack
    image.setStack(newStack)
    
    RESTORE original calibration to image
    UPDATE display
    */

    public static void stackConvert8Bit(ImagePlus imp) {
        if (imp == null) {
            IJ.noImage();
            return;
        }

        Calibration cal = imp.getCalibration();
        imp.setCalibration(null);

        ImageStack stack = imp.getStack();
        int width = stack.getWidth();
        int height = stack.getHeight();
        int size = stack.getSize();

        double globalMin = Double.POSITIVE_INFINITY;
        double globalMax = Double.NEGATIVE_INFINITY;

        for (int i = 1; i <= size; i++) {
            ImageProcessor ip = stack.getProcessor(i).duplicate();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float v = ip.getf(x, y);
                    if (v < globalMin) globalMin = v;
                    if (v > globalMax) globalMax = v;
                }
            }
        }

        double range = globalMax - globalMin;
        if (range == 0) range = 1.0;

        ImageStack newStack = new ImageStack(width, height);

        for (int i = 1; i <= size; i++) {
            ImageProcessor ip = stack.getProcessor(i).duplicate();
            byte[] dst = new byte[width * height];
            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++, idx++) {
                    float v = ip.getf(x, y);
                    double scaled = (v - globalMin) * 255.0 / range;
                    if (scaled < 0) scaled = 0;
                    if (scaled > 255) scaled = 255;
                    dst[idx] = (byte) (int) (scaled + 0.5);
                }
            }
            newStack.addSlice(null, dst);
        }

        imp.setStack(newStack);
        imp.setCalibration(cal);
        imp.updateAndDraw();
    }
}
