package Cell.Analysis;

public class SignalFilter {
    public SignalFilter(){}

    public static double[] gaussianFilter(double[] signal, double sigma){
        int kernelSize = (int) Math.ceil(6 * sigma);
        if (kernelSize % 2 == 0) {
            kernelSize += 1;
        }

        double[] kernel = new double[kernelSize];
        int halfKernel = kernelSize /2;
        double sum = 0.0;

        //Generate kernel
        for (int i = -halfKernel; i <= halfKernel; i++){
            double val = Math.exp(-(i*i)/(2*sigma*sigma));
            kernel[i + halfKernel] = val;
            sum += val;
        }

        //Normalize kernel
        for (int i = 0; i < kernel.length; i++){
            kernel[i] /= sum;
        }

        double[] result = new double[signal.length];

        // Convolution with mirror padding
        for (int i = 0; i < signal.length; i++) {
            sum = 0.0;
            for (int j = -halfKernel; j <= halfKernel; j++) {
                int index = i + j;
                if (index < 0) {
                    index = -index - 1;
                } else if (index >= signal.length) {
                    index = 2 * signal.length - index - 1;
                }
                sum += signal[index] * kernel[j + halfKernel];
            }
            result[i] = sum;
        }
        return result;
    }
}
