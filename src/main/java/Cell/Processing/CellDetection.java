package Cell.Processing;

import ij.IJ;

public class CellDetection {

    public static void runStarDist(){
        new Thread(() -> {
            IJ.run("StarDist 2D", "");
        }).start();
    }

    public static void runCellpose() {
        new Thread(() -> {
            Cellpose c = new Cellpose();
            c.run();
        }).start();
    }
}
