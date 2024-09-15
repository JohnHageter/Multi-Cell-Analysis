package Cell.Frame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;

public class Plot extends JFrame {
    public void plotBinarySpikeTrain(List spikeTrain){
        XYSeries spikeSeries = new XYSeries("Spike Train");
        for (int i = 0; i < spikeTrain.getItemCount(); i++) {
            if (spikeTrain.getItem(i) == 1) {
                spikeSeries.add(i, 1);
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(spikeSeries);

        JFreeChart chart = ChartFactory.createScatterPlot(
                "Binary Spike Train", // Chart title
                "Time", // X-axis label
                "Spike", // Y-axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL,
                false, // Include legend
                false, // Tooltips
                false // URLs
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        renderer.setSeriesShape(0, new Rectangle(-2, -2, 4, 4));
        renderer.setSeriesPaint(0, Color.BLACK);
        plot.setRenderer(renderer);

        plot.getDomainAxis().setRange(0, spikeTrain.length);
        plot.getRangeAxis().setRange(0, 1.5);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        setContentPane(chartPanel);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }
}
