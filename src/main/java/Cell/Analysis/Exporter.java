package Cell.Analysis;

import Cell.UI.Popup;
import Cell.UI.WaitingUI;
import Cell.Utils.CellData;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;


public class Exporter {
    //private static final String PREF_SAVE = "Save";
    private static final String PREF_ITERATIONS = "Iterations";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMULUS_NAMES = "Stimpoint";
    private static final String PREF_METHOD = "Method";
    private static final String PREF_FILTER = "Filter";
    private static final String PREF_LAG = "Lag";
    private static final String PREF_THRESHOLD = "Threshold";
    private static final String PREF_INFLUENCE = "Influence";

    private String stimulusPointsInput = "";
    private String stimulusNamesInput = "";
    private int nIterations = 2;
    private int method = 0;
    private int filter = 0;
    private int lag = 30;
    private double threshold = 3.0;
    private double influence = 0.25;
    private boolean convertedFormat = false;

    ArrayList<ImagePlus> iterations = new ArrayList<ImagePlus>();
    ArrayList<CellData> cells;


    private ResultsTable rt_raw = new ResultsTable();
    private ResultsTable rt_stim = new ResultsTable();

    private final String[] methods = new String[]{">3σ", "Peak Detection", "None"};
    private final String[] filters = new String[]{"Gaussian", "None"};

    public Exporter(ArrayList<CellData> cells){
        this.cells = cells;
    }

    public void exportData() throws BackingStoreException {
        setParameters();
        getIterations();

        int stackSize = iterations.get(0).getStackSize();

        ImageStack imp = new ImageStack();
        for (int i = 1; i <= stackSize; i++){
            ImageProcessor ip = averageIterations();
            imp.addSlice(ip);
        }

        ImagePlus averageImp = new ImagePlus(iterations.get(0).getTitle() + "_AVG", imp);
        averageImp.show();

        filterSignal(averageImp);
    }

    public ImageProcessor averageIterations() {
        int width = iterations.get(0).getWidth();
        int height = iterations.get(0).getHeight();

        ImageProcessor rp = iterations.get(0).getProcessor().duplicate();
        rp.multiply(0);

        for (ImagePlus imp : iterations) {
            ImageProcessor ip = imp.getProcessor();
            if (ip.getWidth() != width || ip.getHeight() != height) {
                new Popup("Error", "Iterations must be the same dimensions").showPopup();
                return null;
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float currentValue = rp.getf(x, y);
                    float newValue = ip.getf(x, y);
                    rp.setf(x, y, currentValue + newValue);
                }
            }
        }
        rp.multiply(1.0 / iterations.size());

        return rp;
    }

    public void filterSignal(ImagePlus imp){
        int nSlices = imp.getNSlices();
        IJ.showStatus("Filtering signal...");
        IJ.showProgress(0, (int) cells.size()*nSlices);
        int cellIndex = 0;
        for (CellData cell : cells) {
            double[] signal = new double[nSlices];
            for (int i = 1; i <= nSlices; i++) {
                imp.setSlice(i);
                ImageProcessor ip = imp.getProcessor();
                ip.setRoi(cell.getCellRoi());
                ImageStatistics stats = ip.getStatistics();
                signal[i] = stats.mean;
                IJ.showProgress(cellIndex*i,cells.size()*nSlices);
            }
            cell.setSignal(signal);
            cellIndex++;
        }
    }

    public void detectPeaks(ImagePlus imp) {
        int nSlices = imp.getNSlices();;
        IJ.showStatus("Detecting peaks...");
        IJ.showProgress(0, (int) cells.size()*nSlices);
        int cellIndex = 0;
        for (CellData cell : cells){
            List<Double> signal = Arrays.stream(cell.getSignal())
                    .boxed()
                    .collect(Collectors.toList());

            SignalDetector sd = new SignalDetector();
            HashMap<String, List> map = sd.analyzeDataForSignals(signal, lag, threshold, influence);
            cell.setSpikeTrain(map.get("signals"));
        }

    }

    public void getResultsTable(ImagePlus imp){
        if(imp.getTitle().contains("_DELTAF")){
            convertedFormat = true;
        } else {
            IJ.log("WARNING: Image series may not be in converted Delta F/F format");
        }

        IJ.showStatus("Generating results...");
        IJ.showProgress(0, imp.getNSlices()*cells.size());
        int progress = 0;

        String name = imp.getTitle().trim();

        for (CellData cell : cells){
            IJ.showProgress(progress, cells.size());

            rt_raw.incrementCounter();
            rt_stim.incrementCounter();

            rt_raw.addValue("Name", name);
            rt_stim.addValue("Name", name);

            rt_raw.addValue("ROI", cell.getName());
            rt_stim.addValue("ROI", cell.getName());

            rt_raw.addValue("X", cell.getCenterX());
            rt_stim.addValue("X", cell.getCenterX());

            rt_raw.addValue("Y", cell.getCenterY());
            rt_stim.addValue("Y", cell.getCenterY());

            rt_raw.addValue("Filter", filters[this.filter]);
            rt_stim.addValue("Filter", filters[this.filter]);

            rt_raw.addValue("Detection.Method", methods[this.method]);
            rt_stim.addValue("Detection.Method", methods[this.method]);

            for (int i = 1; i <= imp.getNSlices(); i++){
                rt_raw.addValue("Slice_" + i, cell.getSignal()[i]);
                rt_stim.addValue("Slice_" + i, cell.getBinary()[i]);
            }


            progress++;
        }

        rt_raw.show("Signal results");
        rt_stim.show("Peak detection results");
    }

    public void setParameters() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(Exporter.class);
        this.nIterations = prefs.getInt(PREF_ITERATIONS, this.nIterations);
        this.stimulusPointsInput = prefs.get(PREF_STIMULUS, this.stimulusPointsInput);
        this.stimulusNamesInput = prefs.get(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
        this.method = prefs.getInt(PREF_METHOD, this.method);
        this.filter = prefs.getInt(PREF_FILTER, this.filter);

        GenericDialog gd = getGenericDialog();

        if (gd.wasOKed()) {
            this.nIterations = (int) gd.getNextNumber();
            this.stimulusNamesInput = gd.getNextString();
            this.stimulusPointsInput = gd.getNextString();
            this.method = gd.getNextChoiceIndex();
            this.filter = gd.getNextChoiceIndex();

            prefs.clear();
            prefs.putInt(PREF_ITERATIONS, this.nIterations);
            prefs.put(PREF_STIMULUS, this.stimulusPointsInput);
            prefs.put(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
            prefs.putInt(PREF_METHOD, this.method);
            prefs.putInt(PREF_FILTER, this.filter);
        }
    }

    public void getIterations() throws BackingStoreException {
        int[] windowList = WindowManager.getIDList();
        if(windowList == null || windowList.length == 0) {
            new Popup("Error", "No images open.").showPopup();
            return;
        }

        String[] impTitles = new String[windowList.length];
        for (int i = 0; i < windowList.length; i ++){
            ImagePlus imp = WindowManager.getImage(windowList[i]);
            impTitles[i] = imp != null ? imp.getTitle() : "N/A";
        }

        GenericDialog gd = new GenericDialog("Select Iterations");
        for (int i = 1; i <= nIterations; i++) {
            gd.addChoice("Iteration " + i + ":", impTitles, impTitles[0]);
        }

        gd.showDialog();

        if(gd.wasOKed()) {
            String[] selectedTitles = new String[this.nIterations];
            for (int i = 0; i < nIterations; i++) {
                selectedTitles[i] = impTitles[gd.getNextChoiceIndex()];
                iterations.add(WindowManager.getImage(selectedTitles[i]));
                //IJ.log("Selected: " + selectedTitles[i]);
            }
        } else if (gd.wasCanceled()) {
            setParameters();
        }
    }

    private GenericDialog getGenericDialog() {
        GenericDialog gd = new GenericDialog("Exporter Settings");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addNumericField("Iterations:", this.nIterations);
        gd.addMessage("Input stimulus as frames points with a duration to search separated by a comma (ex. 60-63,120-123)\nThis will average relative intensity change from frame 60-63 and 120-123");
        gd.addStringField("Stimulus time(s)", this.stimulusNamesInput);
        gd.addStringField("Stimulus Names", this.stimulusPointsInput);
        gd.addChoice("Response call method", methods, methods[this.method]);
        gd.addChoice("Filtering method", filters, filters[0]);
        //gd.addCheckbox("Show group plot", this.plot);
        gd.showDialog();
        return gd;
    }

    public double convertToTime(int slice, double framerate) {
        if (framerate <= 0) {
            framerate = 1;
        }
        return slice / framerate;
    }
}
