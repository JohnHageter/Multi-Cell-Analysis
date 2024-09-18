package Cell.Processing;


import Cell.Utils.CellData;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ijopencv.ij.ImagePlusMatVectorConverter;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;

import java.awt.*;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc.*;
import static ijopencv.ij.ImagePlusMatConverter.*;
import static ijopencv.opencv.MatImagePlusConverter.*;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import de.csbdresden.stardist.*;

public class CellDetection {
    private ImagePlus imp;
    private Mat imp_mat;

    public CellDetection(ImagePlus imp) {
        this.imp = imp;
        //Loader.load(opencv_core.class);
        //Loader.load(ImagePlusMatVectorConverter.class);
        this.imp_mat = toMat(imp);
    }

    public CellDetection(){}

    public void runStarDist(){
        new Thread(() -> {
            IJ.run("StarDist 2D", "");
        }).start();
    }

//    public ArrayList<CellData> detectContourCells() {
//        Mat blurred = blurImage(this.imp_mat);
//        ImagePlus blur = new ImagePlus("Blurred", toImageProcessor(blurred));
//        blur.show();
//
//        Mat threshold = new Mat(otsuThreshold(this.imp_mat));
//        ImagePlus thresh = new ImagePlus("Blurred", toImageProcessor(threshold));
//        thresh.show();
//        getContours(threshold);
//        return null;

//    }

//    private ArrayList<Roi> getContours(Mat mat){
//        opencv_core.MatVector contours = new opencv_core.MatVector();
//        Mat hierarchy = new Mat();
//        findContours(mat, contours ,RETR_TREE, CHAIN_APPROX_SIMPLE);
//        Mat colorImage = new Mat();
//        cvtColor(mat, colorImage, COLOR_GRAY2BGR);
//
//        for (int i = 0; i < contours.size(); i++) {
//            Scalar color = new Scalar(255, 0, 0, 0); // Red color for the contour
//            drawContours(colorImage, contours, i, color, 1, LINE_8, hierarchy, 0, new opencv_core.Point());
//        }
//
//        RoiManager rm = new RoiManager();
//        ArrayList<Roi> drawnRois = matVectorToRoi(contours);
//        for (Roi roi : drawnRois) {
//            rm.addRoi(roi);
//        }
//        rm.setVisible(true);
//
//        new ImagePlus("Contours", toImageProcessor(colorImage)).show();
//        return null;
//    }

//    private Mat otsuThreshold(Mat mat) {
//        Mat threshold = new Mat();
//        opencv_imgproc.threshold(mat, threshold, 0d, 255d, opencv_imgproc.THRESH_BINARY+opencv_imgproc.THRESH_OTSU);
//        return threshold;
//    }
//
//    private Mat blurImage(Mat mat) {
//        Mat blur = new Mat();
//        int kernel = 15;
//        opencv_imgproc.GaussianBlur(mat, blur, new Size(kernel,kernel), 0);
//
//        return blur;
//    }
//
//    public static ArrayList<Roi> matVectorToRoi(MatVector contours) {
//        ArrayList<Roi> rois = new ArrayList<>();
//        for (int i = 0; i < contours.size(); i++) {
//            Mat contour = contours.get(i);
//
//            int numPoints = contour.rows();
//            float[] xPoints = new float[numPoints];
//            float[] yPoints = new float[numPoints];
//
//            for (int j = 0; j < numPoints; j++) {
//                IntPointer point = new IntPointer(contour.ptr(j));
//                xPoints[j] = point.get(0); // X-coordinate
//                yPoints[j] = point.get(1); // Y-coordinate
//            }
//
//            FloatPolygon polygon = new FloatPolygon(xPoints, yPoints, numPoints);
//            PolygonRoi roi = new PolygonRoi(polygon, Roi.POLYGON);
//            rois.add(roi);
//        }
//
//        return rois;
//    }

}
