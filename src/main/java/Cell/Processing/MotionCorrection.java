package Cell.Processing;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.*;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

public class MotionCorrection {
    ImageProcessor reference, target;
    ImageStack stack;
    Rectangle rect;
    Roi template;

    int refSlice, sArea = 0;
    int width, height;
    double disX, disY;
    int itpMethod = 0;

    boolean subPixel = true;

    FloatProcessor result;

    public MotionCorrection(){}

    public void normXCorr(ImagePlus imp) {

        stack = imp.getStack();
        int slices = stack.getSize();
        width = imp.getWidth();
        height = imp.getHeight();


        refSlice = imp.getCurrentSlice();
        template = imp.getRoi();

        if (template != null && template.isArea()) {
            rect = template.getBounds();
        } else {
                IJ.showMessage("Error", "rectangular template ROI needed");
        }

        reference = imp.getProcessor().crop();

        for (int i = 1; i <= slices; i++) {
            alignSlices(i);
        }

        imp.updateAndDraw();
        ImagePlus ret = new ImagePlus(imp.getTitle() + "_ALIGNED", target);

        ret.show();
    }

    private void alignSlices(int slice) {
        int[] dxdy = new int[2];
        boolean edge = false;
        target = stack.getProcessor(slice);
        target.resetRoi();

        result = TemplateMatching.doMatch(target.crop(), reference, edge);
        dxdy = findMax(result, 0);


        if (subPixel) {
            double[] dxdyG = new double[2];

            dxdyG = gaussianPeakFit(result, dxdy[0], dxdy[1]);
            if(sArea==0){
                disX = rect.x - dxdyG[0];
                disY = rect.y - dxdyG[1];
            }else{
                disX = sArea - dxdyG[0];
                disY = sArea - dxdyG[1];
            }
            target.setInterpolationMethod(itpMethod);
        } else {
            if(sArea==0){
                disX = rect.x - dxdy[0];
                disY = rect.y - dxdy[1];
            }else{
                disX = sArea - dxdy[0];
                disY = sArea - dxdy[1];
            }
        }

        target.resetRoi();
        target.translate(disX, disY);

    }

    public static int[] findMax(ImageProcessor ip, int sW) {
        int[] coord = new int[2];
        float max = ip.getPixel(0, 0);
        int sWh, sWw;

        if (sW == 0) {
            sWh = ip.getHeight();
            sWw = ip.getWidth();
        } else {
            sWh = sW;
            sWw = sW;
        }

        for (int j = (int) (ip.getHeight() - sWh) / 2; j < (ip.getHeight() + sWh) / 2; j++) {
            for (int i = (ip.getWidth() - sWw) / 2; i < (ip.getWidth() + sWw) / 2; i++) {
                if (ip.getPixel(i, j) > max) {
                    max = ip.getPixel(i, j);
                    coord[0] = i;
                    coord[1] = j;
                }
            }
        }
        return (coord);
    }

    private double[] gaussianPeakFit(ImageProcessor ip, int x, int y) {
        double[] coord = new double[2];
        if (x == 0
                || x == ip.getWidth() - 1
                || y == 0
                || y == ip.getHeight() - 1) {
            coord[0] = x;
            coord[1] = y;
        } else {
            coord[0] = x
                    + (Math.log(ip.getPixel(x - 1, y))
                    - Math.log(ip.getPixel(x + 1, y)))
                    / (2 * Math.log(ip.getPixel(x - 1, y))
                    - 4 * Math.log(ip.getPixel(x, y))
                    + 2 * Math.log(ip.getPixel(x + 1, y)));
            coord[1] = y
                    + (Math.log(ip.getPixel(x, y - 1))
                    - Math.log(ip.getPixel(x, y + 1)))
                    / (2 * Math.log(ip.getPixel(x, y - 1))
                    - 4 * Math.log(ip.getPixel(x, y))
                    + 2 * Math.log(ip.getPixel(x, y + 1)));
        }
        return (coord);
    }

}
