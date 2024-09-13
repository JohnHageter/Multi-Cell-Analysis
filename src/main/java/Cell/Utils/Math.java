package Cell.Utils;

import ij.IJ;

public class Math {
    public static double sd(double[] arr) {
        double sum = 0.0;
        double mean;
        double variance = 0.0;
        double count = 0.0;

        for (int i = 0; i<=arr.length; i++){
            sum += arr[i];
            count++;
        }
        mean = sum / count;

        for (int i = 0; i <= arr.length; i++){
            variance += java.lang.Math.pow(arr[i] - mean, 2);
        }
        variance = variance / count;

        return java.lang.Math.sqrt(variance);
    }

    public static int[] seq(int begin, int end, int by) {
        int size = (end - begin) / by + 1;
        int[] sequence = new int[size];
        int index = 0;
        for (int i = begin; i <= end; i += by) {
            sequence[index++] = i;
        }
        return sequence;
    }

    public static double[] seq(double begin, double end, double by) {
        int size = (int) ((end - begin) / by + 1);
        double[] sequence = new double[size];
        int index = 0;
        for (double i = begin; i <= end; i += by) {
            sequence[index++] = i;
        }
        return sequence;
    }

    public static long[] seq(long begin, long end, long by) {
        int size = (int) ((end - begin) / by + 1);
        long[] sequence = new long[size];
        int index = 0;
        for (long i = begin; i <= end; i += by) {
            sequence[index++] = i;
        }
        return sequence;
    }

    public static float[] seq(float begin, float end, float by) {
        int size = (int) ((end - begin) / by + 1);
        float[] sequence = new float[size];
        int index = 0;
        for (float i = begin; i <= end; i += by) {
            sequence[index++] = i;
        }
        return sequence;
    }

    public static Integer strToInt(String str){
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            IJ.showMessage("Error", "Input must be numeric. Error in: " + str);
            return null;
        }
    }
}
