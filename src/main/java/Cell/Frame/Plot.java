package Cell.Frame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class Plot {
    private String title;
    private String xAxisLabel;
    private String yAxisLabel;
    private Map<String, XYSeries> seriesMap;

    public Plot(String title, String xAxis, String yAxis) {
        this.title = title;
        this.xAxisLabel = xAxis;
        this.yAxisLabel = yAxis;
        this.seriesMap = new HashMap<>();
    }

    public void addSeries(String seriesName) {
        seriesMap.put(seriesName, new XYSeries(seriesName));
    }

    public void addData(String seriesName, double x, double y) {
        XYSeries series = seriesMap.get(seriesName);
        if (series != null) {
            series.add(x, y);
        } else {
            System.out.println("Series '" + seriesName + "' not found.");
        }
    }

    public void display() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries series : seriesMap.values()) {
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        Plot plot = new Plot("Sample Multi-Line Plot", "X-Axis", "Y-Axis");

        // Adding multiple series
        plot.addSeries("Series 1");
        plot.addSeries("Series 2");

        // Adding data to Series 1
        plot.addData("Series 1", 1, 1);
        plot.addData("Series 1", 2, 4);
        plot.addData("Series 1", 3, 9);

        // Adding data to Series 2
        plot.addData("Series 2", 1, 2);
        plot.addData("Series 2", 2, 3);
        plot.addData("Series 2", 3, 5);
        plot.addData("Series 2", 4, 7);

        // Display the plot
        plot.display();
    }
}
