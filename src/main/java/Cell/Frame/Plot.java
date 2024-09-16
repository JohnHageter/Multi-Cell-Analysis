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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import Cell.Analysis.SignalDetector;

public class Plot extends JFrame {
    private final SignalDetector sd = new SignalDetector();
    private HashMap<String, List> responseList;
    private List<Double> signal;
    private JSlider lagSlider, thresholdSlider, influenceSlider;
    private JLabel lagLabel, thresholdLabel, influenceLabel;
    private ChartPanel chartPanel;

    public Plot(List<Double> signal) {
        this.signal = signal;
    }

    public void plotSpikeTrain() {
        setLayout(new BorderLayout());

        // Initialize sliders
        lagSlider = createSlider(1, 100, 30, "Lag");
        thresholdSlider = createSlider(0, 100, 35, "Threshold (x 0.1)", 0.1);
        influenceSlider = createSlider(0, 100, 50, "Influence (x 0.01)", 0.01);

        // Initialize labels
        lagLabel = new JLabel("Lag: 30");
        thresholdLabel = new JLabel("Threshold: 3.5");
        influenceLabel = new JLabel("Influence: 0.50");

        // Panel for sliders and labels
        JPanel sliderPanel = new JPanel(new GridLayout(3, 2));
        sliderPanel.add(lagSlider);
        sliderPanel.add(lagLabel);
        sliderPanel.add(thresholdSlider);
        sliderPanel.add(thresholdLabel);
        sliderPanel.add(influenceSlider);
        sliderPanel.add(influenceLabel);

        // Add change listener to sliders
        ChangeListener sliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLabels();  // Update labels when sliders change
                updatePlot();    // Re-plot the graph with new slider values
            }
        };

        lagSlider.addChangeListener(sliderListener);
        thresholdSlider.addChangeListener(sliderListener);
        influenceSlider.addChangeListener(sliderListener);

        // Add slider panel to the frame
        add(sliderPanel, BorderLayout.SOUTH);

        // Initial plot
        updatePlot();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setVisible(true);
    }

    private JSlider createSlider(int min, int max, int initial, String title) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, initial);
        slider.setBorder(BorderFactory.createTitledBorder(title));
        return slider;
    }

    private JSlider createSlider(int min, int max, int initial, String title, double scale) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, initial);
        slider.setBorder(BorderFactory.createTitledBorder(title));
        slider.putClientProperty("scale", scale);
        return slider;
    }

    private void updateLabels() {
        // Update the labels with current slider values
        int lag = lagSlider.getValue();
        double threshold = thresholdSlider.getValue() / 10.0;
        double influence = influenceSlider.getValue() / 100.0;

        lagLabel.setText("Lag: " + lag);
        thresholdLabel.setText(String.format("Threshold: %.1f", threshold));
        influenceLabel.setText(String.format("Influence: %.2f", influence));
    }

    private void updatePlot() {
        // Get slider values
        int lag = lagSlider.getValue();
        double threshold = thresholdSlider.getValue() / 10.0;
        double influence = influenceSlider.getValue() / 100.0;

        // Analyze signal
        responseList = sd.analyzeDataForSignals(signal, lag, threshold, influence);
        List<Integer> binary = responseList.get("signals");

        // Create series for spike train
        XYSeries spikeSeries = new XYSeries("Spike Train");
        for (int i = 0; i < binary.size(); i++) {
                spikeSeries.add(i, binary.get(i));
        }

        // Add the series to dataset
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(spikeSeries);

        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
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
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLACK);
        plot.setRenderer(renderer);

        // Update the chart panel
        if (chartPanel != null) {
            chartPanel.setChart(chart);  // Update chart in existing panel
        } else {
            chartPanel = new ChartPanel(chart);  // Create new chart panel if it doesn't exist
            add(chartPanel, BorderLayout.CENTER);  // Add chart panel to the frame
        }

        revalidate();
        repaint();
    }
}
