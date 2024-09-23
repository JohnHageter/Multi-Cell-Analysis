package Cell.Utils;

import Cell.Analysis.SignalFilter;
import Cell.Frame.Plot;
import ij.IJ;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static Cell.Processing.CellDetection.runStarDist;
import static Cell.Utils.Math.seq;

public class Test {
    public static double[] signal = {
            0.18757, 0.11173, 0.13787, 0.08408, 0.12447, 0.08744, 0.10210, 0.05646, 0.05786, 0.07088,
            0.07744, 0.07466, 0.05582, 0.00087, -0.01450, -0.04066, -0.04238, -0.05659, -0.03443, -0.04382,
            -0.01740, -0.02471, -0.04113, -0.02753, -0.06895, -0.00993, -0.05348, -0.04850, -0.04561, -0.04012,
            -0.03377, -0.06928, -0.05254, -0.00062, -0.02591, -0.07876, -0.00131, -0.03007, -0.00664, -0.02611,
            -0.02313, -0.04106, -0.05148, -0.06245, -0.09985, -0.04148, -0.06760, -0.05855, -0.07935, -0.05282,
            -0.07347, -0.04800, -0.08867, -0.04511, -0.06514, -0.08167, -0.09223, -0.04474, -0.07329, -0.10442,
            0.32957, 1.58295, 2.39901, 2.08912, 2.00694, 1.84354, 1.70579, 1.63543, 1.44127, 1.34177,
            1.04718, 0.99487, 1.08932, 1.13172, 0.98967, 0.80217, 0.75346, 0.66489, 0.49420, 0.56073,
            0.67784, 0.54193, 0.47762, 0.46143, 0.50912, 0.45810, 0.65955, 0.66279, 0.55344, 0.58897,
            0.46287, 0.45961, 0.57576, 0.49830, 0.50052, 0.49372, 0.50680, 0.41530, 0.44219, 0.47799,
            0.35841, 0.40836, 0.44540, 0.44043, 0.41725, 0.38338, 0.62491, 0.43666, 0.34259, 0.32221,
            0.35744, 0.29878, 0.36440, 0.25377, 0.18450, 0.11067, 0.07689, 0.11102, 0.08220, 0.14760,
            0.15877, 0.11890, 0.26388, 0.22573, 0.17206, 0.11949, 0.21267, 0.22173, 0.10064, 0.19112,
            0.15765, 0.10074, 0.07303, -0.01029, -0.03341, -0.02926, -0.06289, -0.06774, -0.07462, -0.11586,
            -0.11865, -0.10787, -0.00227, -0.02026, 0.15742, 0.17200, 0.12643, 0.05927, 0.10420, 0.05561,
            -0.02033, -0.03897, -0.07126, -0.13808, -0.16306, -0.17193, -0.18537, -0.16053, -0.13779, -0.12473,
            -0.15480, -0.15841, -0.14854, -0.16434, -0.14843, -0.17332, -0.17586, -0.13233, -0.14651, -0.16322,
            -0.12211, -0.17036, -0.20800, -0.22152, -0.20856, -0.18127, -0.19803, -0.20137, -0.18853, -0.19098
    };

    public static void testGaussian(){
        IJ.log("Testing filter");
        double[] result2 = SignalFilter.gaussianFilter(signal, 2.0);
        double[] result1 = SignalFilter.gaussianFilter(signal, 1.0);
        double[] result05 = SignalFilter.gaussianFilter(signal, 0.5);
        double[] result01 = SignalFilter.gaussianFilter(signal, 0.1);

        ij.gui.Plot plt = new ij.gui.Plot("Gaussian filter", "Time", "Df/f");
        double[] time = seq(1.0,180.0,1.0);

        plt.setLineWidth(2);
        plt.setColor(Color.BLUE);
        plt.addPoints(time, signal, ij.gui.Plot.LINE);
        plt.setColor(Color.ORANGE);
        plt.addPoints(time, result2, ij.gui.Plot.LINE);
        plt.setColor(Color.RED);
        plt.addPoints(time, result1, ij.gui.Plot.LINE);
        plt.setColor(Color.GREEN);
        plt.addPoints(time, result05, ij.gui.Plot.LINE);
        plt.setColor(Color.PINK);
        plt.addPoints(time, result01, ij.gui.Plot.LINE);
        plt.addLegend("Original\nGaussian 2.0\nGaussian 1.0\nGaussian 0.5\n Gaussian 0.1");
        plt.show();
    }

    public static void testSpikeDetection(){
        //List<Double> data, int lag, Double threshold, Double influence
        List<Double> signalList = Arrays.stream(signal).boxed().collect(Collectors.toList());
        Plot plt = new Plot(signalList);
        plt.plotSpikeTrain();

    }

    public static void testBlur() {
        runStarDist();
    }

}
