package Cell.Analysis;

import Cell.UI.Popup;
import Cell.Utils.CellData;
import Cell.Utils.GroupData;
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
import java.util.stream.IntStream;


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
    private int detectionMethod = 0;
    private int filter = 0;
    private int lag = 30;
    private double threshold = 3.0;
    private double influence = 0.25;

    ArrayList<ImagePlus> iterations = new ArrayList<ImagePlus>();
    ArrayList<CellData> cells;
    ArrayList<GroupData> groups;
    ImagePlus imp;
    ImagePlus averageImp;


    private final ResultsTable rt_raw = new ResultsTable();
    private final ResultsTable rt_stim = new ResultsTable();

    private final String[] detectionMethods = new String[]{"Peak Detection", "None"};
    private final String[] filters = new String[]{"Gaussian", "None"};

    public static final int FILTER_GAUSSIAN = 0;
    public static final int FILTER_NONE = -1;
    public static final int PEAK_LAGGING_WINDOW = 1;

    public Exporter(ArrayList<CellData> cells, ArrayList<GroupData> groups){
        this.cells = cells;
        this.groups = groups;
    }

    public void exportData() throws BackingStoreException {
        this.imp = IJ.getImage();
        setParameters();
        getIterations();

        if(this.cells.isEmpty()){
            return;
        }

        int stackSize = iterations.get(0).getStackSize();

        new Thread(() -> {
            ImageStack imp = new ImageStack();
            for (int i = 1; i <= stackSize; i++){
                ImageProcessor ip = averageIterations(i);
                imp.addSlice(ip);
            }
            averageImp = new ImagePlus(iterations.get(0).getTitle() + "_AVG", imp);
            averageImp.show();
        }).start();

        filterSignal();
        detectPeaks();
        new Thread(this::getResultsTable).start();
    }

    public ImageProcessor averageIterations(int sliceIndex) {
        int width = iterations.get(0).getWidth();
        int height = iterations.get(0).getHeight();

        ImageProcessor rp = iterations.get(0).getStack().getProcessor(sliceIndex).duplicate();
        rp.multiply(0);

        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                float currentValue = rp.getf(x, y);
                for (ImagePlus imp : iterations) {
                    ImageProcessor ip = imp.getStack().getProcessor(sliceIndex);
                    float newValue = ip.getf(x, y);
                    currentValue += newValue;
                }
                rp.setf(x, y, currentValue);
            }
        });

        rp.multiply(1.0 / iterations.size());

        return rp;
    }

    public void filterSignal(){
        int nSlices = imp.getNSlices();
        IJ.showStatus("Filtering signal...");
        IJ.showProgress(0, (int) cells.size()*nSlices);
        int cellIndex = 0;
        for (CellData cell : cells) {
            double[] signal = new double[nSlices];
            //IJ.log("Slices "+ nSlices);
            for (int i = 1; i <= nSlices; i++) {
                //IJ.log(Integer.toString(i));
                imp.setSlice(i);
                ImageProcessor ip = imp.getProcessor();
                ip.setRoi(cell.getCellRoi());
                ImageStatistics stats = ip.getStatistics();
                signal[i-1] = stats.mean;
            }
            IJ.showProgress(cellIndex,cells.size()*nSlices);

            if(this.filter == FILTER_GAUSSIAN) {
                double[] fsignal = SignalFilter.gaussianFilter(signal, 0.3);
                cell.setSignal(fsignal);
            } else {
                cell.setSignal(signal);
            }

            cellIndex++;
        }
    }

    public void detectPeaks() {
        int nSlices = imp.getNSlices();;
        IJ.showStatus("Detecting peaks...");
        IJ.showProgress(0, (int) cells.size()*nSlices);
        for (CellData cell : cells){
            List<Double> signal = Arrays.stream(cell.getSignal())
                    .boxed()
                    .collect(Collectors.toList());

            SignalDetector sd = new SignalDetector();
            HashMap<String, List> map = sd.peakLaggingWindow(signal, lag, threshold, influence);
            cell.setSpikeTrain(map.get("signals"));
        }

    }

    public void getResultsTable(){
        if(imp.getTitle().contains("_DELTAF")){
            boolean convertedFormat = true;
        } else {
            IJ.log("WARNING: Image series may not be in converted Delta F/F format");
        }

        IJ.showStatus("Generating results...");
        IJ.showProgress(0, imp.getNSlices()*cells.size());
        int progress = 0;

        String name = imp.getTitle().trim();

        for (CellData cell : cells){
            String group = getCellGroupName(cell);
            IJ.showProgress(progress, imp.getNSlices()*cells.size());

            rt_raw.incrementCounter();
            rt_stim.incrementCounter();

            rt_raw.addValue("Name", name);
            rt_stim.addValue("Name", name);

            rt_raw.addValue("ROI", cell.getName());
            rt_stim.addValue("ROI", cell.getName());

            rt_stim.addValue("Group",group);
            rt_raw.addValue("Group", group);

            rt_raw.addValue("X", cell.getCenterX());
            rt_stim.addValue("X", cell.getCenterX());

            rt_raw.addValue("Y", cell.getCenterY());
            rt_stim.addValue("Y", cell.getCenterY());

            rt_raw.addValue("Filter", filters[this.filter]);
            rt_stim.addValue("Filter", filters[this.filter]);

            rt_raw.addValue("Detection.Method", detectionMethods[this.detectionMethod]);
            rt_stim.addValue("Detection.Method", detectionMethods[this.detectionMethod]);

            for (int i = 1; i <= imp.getNSlices(); i++){
                rt_raw.addValue("Slice_" + i, cell.getSignal()[i-1]);
                rt_stim.addValue("Slice_" + i, cell.getSpikeTrain()[i-1]);
            }

            progress++;
        }

        rt_raw.show("Signal results");
        rt_stim.show("Peak detection results");
    }

    private String getCellGroupName(CellData cell) {
        StringBuilder groupName = new StringBuilder();

        for (GroupData group: groups) {
            for (CellData groupCells : group.getCellsInGroup()) {
                if (groupCells.getCellRoi() == cell.getCellRoi()) {
                    groupName.append(group.name).append(":");
                }
            }
        }

        return groupName.toString();
    }

    public void setParameters() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(Exporter.class);
        this.nIterations = prefs.getInt(PREF_ITERATIONS, this.nIterations);
        this.stimulusPointsInput = prefs.get(PREF_STIMULUS, this.stimulusPointsInput);
        this.stimulusNamesInput = prefs.get(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
        this.detectionMethod = prefs.getInt(PREF_METHOD, this.detectionMethod);
        this.filter = prefs.getInt(PREF_FILTER, this.filter);

        GenericDialog gd = getGenericDialog();

        if (gd.wasOKed()) {
            this.nIterations = (int) gd.getNextNumber();
            this.stimulusNamesInput = gd.getNextString();
            this.stimulusPointsInput = gd.getNextString();
            this.detectionMethod = gd.getNextChoiceIndex();
            this.filter = gd.getNextChoiceIndex();

            if(this.detectionMethod == PEAK_LAGGING_WINDOW) {
                this.lag = prefs.getInt(PREF_LAG, this.lag);
                this.threshold = prefs.getDouble(PREF_THRESHOLD, this.threshold);
                this.influence = prefs.getDouble(PREF_INFLUENCE, this.influence);

                GenericDialog gd_peak = new GenericDialog("Peak detection");
                gd_peak.addMessage("Input parameters for peak detection");
                gd_peak.addNumericField("Lag: ", this.lag);
                gd_peak.addNumericField("Threshold: ", this.threshold);
                gd_peak.addNumericField("Influence: ", this.influence);
                gd_peak.showDialog();

                if(gd_peak.wasOKed()) {
                    this.lag = (int)gd_peak.getNextNumber();
                    this.threshold = gd_peak.getNextNumber();
                    this.influence = gd_peak.getNextNumber();

                    prefs.putInt(PREF_LAG, this.lag);
                    prefs.putDouble(PREF_THRESHOLD, this.threshold);
                    prefs.putDouble(PREF_INFLUENCE, this.influence);
                }
            }

            prefs.clear();
            prefs.putInt(PREF_ITERATIONS, this.nIterations);
            prefs.put(PREF_STIMULUS, this.stimulusPointsInput);
            prefs.put(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
            prefs.putInt(PREF_METHOD, this.detectionMethod);
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
        gd.addChoice("Response call method", detectionMethods, detectionMethods[this.detectionMethod]);
        gd.addChoice("Filtering method", filters, filters[0]);
        gd.showDialog();
        return gd;
    }
}
