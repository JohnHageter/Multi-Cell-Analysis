package Cell.Utils;

import ij.IJ;

public class Math {
    public double sd(double[] arr) {
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

    public int[] seq(int begin, int end, int by) {
        int size = (end - begin) / by + 1;
        int[] sequence = new int[size];
        int index = 0;
        for (int i = begin; i <= end; i += by) {
            sequence[index++] = i;
        }
        return sequence;
    }

    public Integer strToInt(String str){
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            IJ.showMessage("Error", "Input must be numeric. Error in: " + str);
            return null;
        }
    }
}
