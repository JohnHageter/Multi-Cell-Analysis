package Cell.Analysis;

import com.github.psambit9791.jdsp.filter.*;

public class SignalFilter {
    private int samplingRate;
    private int order;
    private int cutOff;

    private int lowCutOff;
    private int highCutoff;

    public SignalFilter(){}

    public SignalFilter(int samplingRate, int order, int cutOff) {
        this.samplingRate = samplingRate;
        this.order = order;
        this.cutOff = cutOff;
    }

    public SignalFilter(int samplingRate, int order, int lowCutOff, int highCutoff){
        this.samplingRate = samplingRate;
        this.order = order;
        this.lowCutOff = lowCutOff;
        this.highCutoff = highCutoff;
    }

    public static double[] gaussianFilter(double[] signal, double sigma){
        int kernelSize = (int) Math.ceil(6 * sigma);
        if (kernelSize % 2 == 0) {
            kernelSize += 1;
        }

        double[] kernel = new double[kernelSize];
        int halfKernel = kernelSize /2;
        double sum = 0.0;

        //Generate
        for (int i = -halfKernel; i <= halfKernel; i++){
            double val = Math.exp(-(i*i)/(2*sigma*sigma));
            kernel[i + halfKernel] = val;
            sum += val;
        }

        //Normalize
        for (int i = 0; i < kernel.length; i++){
            kernel[i] /= sum;
        }

        double[] result = new double[signal.length];

        for (int i = 0; i < signal.length; i++){
            sum = 0.0;
            for (int j = -halfKernel; j<=halfKernel; j++){
                int index = i+j;
                if (index < 0){
                    index = -index;
                } else if (index >= signal.length) {
                    index = 2 * signal.length - index - 1;
                }

                sum += signal[index] * kernel[j +halfKernel];
            }

            result[i] = sum;
        }
        return result;
    }

    /*
     * Copyright (c) 2019 - 2023  Sambit Paul
     * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
     * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
     * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
     * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
     *
     * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
     * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
     * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
     * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
     */

    public double[] lowPassButterworth(double[] signal) {
        Butterworth filter = new Butterworth(this.samplingRate);
        return filter.lowPassFilter(signal, this.order, this.cutOff);
    }

    public double[] highPassButterwoth(double[] signal){
        Butterworth filter = new Butterworth(this.samplingRate);
        return filter.highPassFilter(signal, this.order, this.cutOff);
    }

    public double[] bandPassButterworth(double[] signal){
        Butterworth filter = new Butterworth(this.samplingRate);
        return filter.bandPassFilter(signal, this.order, this.lowCutOff, this.highCutoff);
    }

    public double[] bandStopButterworth(double[] signal){
        Butterworth filter = new Butterworth(this.samplingRate);
        return filter.bandStopFilter(signal, this.order, this.lowCutOff, this.highCutoff);
    }



}
