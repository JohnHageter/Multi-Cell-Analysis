package Cell.Processing;

import ij.IJ;
import ij.ImagePlus;
import Cell.Utils.Utils;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class CalciumProcessor {
    public CalciumProcessor() {}

    public void convertToDF(ImagePlus imp, int baselineBegin, int baselineEnd) {
        if (!Utils.isStack(imp)) {
            throw new IllegalArgumentException("Input image must be a stack.");
        }

        ImageStack deltaFStack = new ImageStack();
        ImageStack stack = imp.getStack();

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

        ImagePlus ret = new ImagePlus(imp.getTitle() + "_DeltaF/F", deltaFStack);
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
}
