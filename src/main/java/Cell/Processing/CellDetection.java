package Cell.Processing;

import ij.IJ;

public class CellDetection {

    public static void runStarDist(){
        new Thread(() -> {
            try {
                IJ.run("StarDist 2D", "");
            } catch (Exception e) {
                IJ.showMessage("Update sites required", "To use StarDist2D please enable the\nStardist, CSBDeep, and Tensorflow update sites.");
            }
        }).start();
    }

    public static void runCellpose() {
        new Thread(() -> {
            Cellpose c = new Cellpose();
            c.run();
        }).start();
    }
}
