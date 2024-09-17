package Cell.Analysis;

import java.util.*;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

//Based on peak detection algorithm from https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data/56174275#56174275
public class SignalDetector {
    public static final int PEAK_3SD = 0;
    public static final int PEAK_LAG_WINDOW = 1;
    public static final int PEAK_NONE = 2;

    public HashMap<String, List> peakLaggingWindow(List<Double> data, int lag, Double threshold, Double influence) {

        SummaryStatistics stats = new SummaryStatistics();
        List<Integer> signals = new ArrayList<Integer>(Collections.nCopies(data.size(), 0));
        List<Double> filteredData = new ArrayList<Double>(data);
        List<Double> avgFilter = new ArrayList<Double>(Collections.nCopies(data.size(), 0.0d));
        List<Double> stdFilter = new ArrayList<Double>(Collections.nCopies(data.size(), 0.0d));

        for (int i = 0; i < lag; i++) {
            stats.addValue(data.get(i));
        }
        avgFilter.set(lag - 1, stats.getMean());
        stdFilter.set(lag - 1, Math.sqrt(stats.getPopulationVariance()));
        stats.clear();

        for (int i = lag; i < data.size(); i++) {

            if (Math.abs((data.get(i) - avgFilter.get(i - 1))) > threshold * stdFilter.get(i - 1)) {
                if (data.get(i) > avgFilter.get(i - 1)) {
                    signals.set(i, 1);
                } else {
                    signals.set(i, -1);
                }

                filteredData.set(i, (influence * data.get(i)) + ((1 - influence) * filteredData.get(i - 1)));
            } else {
                signals.set(i, 0);
                filteredData.set(i, data.get(i));
            }

            for (int j = i - lag; j < i; j++) {
                stats.addValue(filteredData.get(j));
            }
            avgFilter.set(i, stats.getMean());
            stdFilter.set(i, Math.sqrt(stats.getPopulationVariance()));
            stats.clear();
        }


        HashMap<String, List> returnMap = new HashMap<>();
        returnMap.put("signals", signals);
        returnMap.put("raw", data);
        returnMap.put("filteredData", filteredData);
        returnMap.put("avgFilter", avgFilter);
        returnMap.put("stdFilter", stdFilter);

        return returnMap;
    }
}
