package Cell.Analysis;

import Cell.Frame.CellManager;
import Cell.UI.Popup;
import Cell.Utils.CellData;
import Cell.Utils.GroupData;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Optimized Exporter:
 * - Avoids repeated ROI statistics by averaging pixel values directly via precomputed pixel indices.
 * - Averages iteration stacks using raw array math (no per-pixel getf/setf calls).
 * - Writes CSVs quickly and opens them asynchronously as ResultsTables.
 * - Reduces UI/update overhead and unnecessary allocations.
 */
public class Exporter {
    private static final String PREF_ITERATIONS = "Iterations";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMULUS_NAMES = "Stimpoint";
    private static final String PREF_METHOD = "Method";
    private static final String PREF_FILTER = "Filter";
    private static final String PREF_LAG = "Lag";
    private static final String PREF_THRESHOLD = "Threshold";
    private static final String PREF_INFLUENCE = "Influence";
    private static final String PREF_SIGMA = "Sigma";

    private String stimulusPointsInput = "";
    private String stimulusNamesInput = "";
    private String exportName = "";
    private int nIterations = 2;
    private int detectionMethod = 0;
    private int filter = 0;
    private int lag = 30;
    private double threshold = 3.0;
    private double influence = 0.25;
    private double sigma = 0.3;

    private final ArrayList<ImagePlus> iterations = new ArrayList<>();
    private final ArrayList<CellData> cells;
    private final ArrayList<GroupData> groups;
    private ImagePlus imp;
    private ImagePlus averageImp;

    private final String[] detectionMethods = new String[]{"Peak Detection", "None"};
    private final String[] filters = new String[]{"Gaussian", "None"};

    public static final int FILTER_GAUSSIAN = 0;
    public static final int FILTER_NONE = 1;
    public static final int PEAK_LAGGING_WINDOW = 0;
    public static final int DETECT_NONE = 1;

    private final Map<CellData, int[]> pixelIndexCache = new IdentityHashMap<>();
    private final Map<Roi, String> groupNameByRoi = new IdentityHashMap<>();

    public Exporter(ArrayList<CellData> cells, ArrayList<GroupData> groups) {
        this.cells = cells;
        this.groups = groups;
        buildGroupCache();
    }

    public void exportData() throws BackingStoreException {
        this.imp = IJ.getImage();
        setParameters();
        getIterations();

        if (this.cells == null || this.cells.isEmpty()) return;

        generateAverageStack();

        if (this.filter == FILTER_GAUSSIAN) {
            filterSignal();
        }

        if (this.detectionMethod == PEAK_LAGGING_WINDOW){
            detectPeaks();
            generatePeakStack();
        }

        new Thread(this::getResultsTable, "Write-Results").start();
    }

    private void generatePeakStack() {
        ImagePlus sigStack = averageImp;
        sigStack.setTitle(iterations.get(0).getTitle() + "_PEAK");
        int nSlices = sigStack.getNSlices();

        if (cells == null || cells.isEmpty()) {
            IJ.log("No cells found.");
            return;
        }

        CellManager cm = CellManager.getInstance();
        if (cm == null) return;

        Overlay overlay = new Overlay();

        for (CellData cell : cells) {
            int[] spikeTrain = cell.getSpikeTrain();
            Roi baseRoi = cell.getCellRoi();
            if (baseRoi == null) continue;

            for (int s = 0; s < nSlices && s < spikeTrain.length; s++) {
                int val = spikeTrain[s];


                Roi sliceRoi = (Roi) baseRoi.clone();
                sliceRoi.setPosition(s+1);

                if (val == 1) {
                    sliceRoi.setStrokeColor(Color.GREEN);
                } else if (val == -1) {
                    sliceRoi.setStrokeColor(Color.RED);
                } else {
                    sliceRoi.setStrokeColor(Color.BLUE);
                }

                overlay.add(sliceRoi);
            }
        }

        sigStack.setOverlay(overlay);
        sigStack.show();
    }

    private FloatProcessor averageIterations(int sliceIndex) {
        int width = iterations.get(0).getWidth();
        int height = iterations.get(0).getHeight();
        int nPix = width * height;
        int nIter = iterations.size();

        float[] avg = new float[nPix];

        for (ImagePlus im : iterations) {
            Object pixels = im.getStack().getProcessor(sliceIndex).getPixels();

            if (pixels instanceof byte[]) {
                byte[] px = (byte[]) pixels;
                for (int i = 0; i < nPix; i++) {
                    avg[i] += (px[i] & 0xff);
                }
            } else if (pixels instanceof float[]) {
                float[] px = (float[]) pixels;
                for (int i = 0; i < nPix; i++) {
                    avg[i] += px[i];
                }
            } else {
                throw new IllegalArgumentException("Unsupported pixel type: " + pixels.getClass());
            }
        }

        for (int i = 0; i < nPix; i++) {
            avg[i] /= nIter;
        }

        return new FloatProcessor(width, height, avg);
    }

    private void generateAverageStack() {
        int stackSize = iterations.get(0).getStackSize();
        ImageStack outStack = new ImageStack(iterations.get(0).getWidth(), iterations.get(0).getHeight());
        for (int s = 1; s <= stackSize; s++) {
            FloatProcessor fp = averageIterations(s);
            outStack.addSlice(fp);
            if (s % 10 == 0) IJ.showProgress(s, stackSize);
        }
        averageImp = new ImagePlus(iterations.get(0).getTitle() + "_AVG", outStack);
        averageImp.show();
    }

    private void filterSignal() {
        final int nSlices = imp.getNSlices();
        IJ.showStatus("Filtering signal...");
        IJ.showProgress(0, Math.max(1, cells.size()));
        precomputeAllPixelIndices();

        int row = 0;
        for (CellData cell : cells) {
            double[] signal = new double[nSlices];
            final int[] idx = pixelIndexCache.get(cell);
            final int roiCount = idx.length;

            for (int s = 1; s <= nSlices; s++) {
                Object pixels = imp.getStack().getProcessor(s).getPixels();

                double sum = 0;
                if (pixels instanceof byte[]) {
                    byte[] px = (byte[]) pixels;
                    for (int p : idx) sum += (px[p] & 0xff);
                } else if (pixels instanceof float[]) {
                    float[] px = (float[]) pixels;
                    for (int p : idx) sum += px[p];
                } else {
                    throw new IllegalArgumentException("Unsupported pixel type: " + pixels.getClass());
                }

                signal[s - 1] = sum / roiCount;
            }

            if (this.filter == FILTER_GAUSSIAN) {
                double[] fsignal = SignalFilter.gaussianFilter(signal, this.sigma);
                cell.setSignal(fsignal);
            } else {
                cell.setSignal(signal);
            }

            row++;
            if (row % 25 == 0) {
                IJ.showProgress(row, cells.size());
            }
        }
    }

    private void precomputeAllPixelIndices() {
        final int w = imp.getWidth();
        final int h = imp.getHeight();
        for (CellData cell : cells) {
            if (!pixelIndexCache.containsKey(cell)) {
                Roi roi = cell.getCellRoi();
                if (roi == null) {
                    pixelIndexCache.put(cell, new int[0]);
                    continue;
                }
                Rectangle b = roi.getBounds();
                java.util.ArrayList<Integer> list = new java.util.ArrayList<>(b.width * b.height);
                for (int y = Math.max(0, b.y); y < Math.min(h, b.y + b.height); y++) {
                    for (int x = Math.max(0, b.x); x < Math.min(w, b.x + b.width); x++) {
                        if (roi.contains(x, y)) {
                            list.add(y * w + x);
                        }
                    }
                }
                int[] idx = new int[list.size()];
                for (int i = 0; i < list.size(); i++) idx[i] = list.get(i);
                pixelIndexCache.put(cell, idx);
            }
        }
    }

    private void detectPeaks() {
        final int nSlices = imp.getNSlices();
        IJ.showStatus("Detecting peaks...");
        IJ.showProgress(0, Math.max(1, cells.size()));

        int row = 0;
        for (CellData cell : cells) {
            double[] sig = cell.getSignal();
            java.util.ArrayList<Double> asList = new java.util.ArrayList<>(sig.length);
            for (double v : sig) asList.add(v);

            SignalDetector sd = new SignalDetector();
            Map<String, List> map = sd.peakLaggingWindow(asList, lag, threshold, influence);

            List<?> spikesList = map.get("signals");
            int[] spikes = new int[nSlices];
            for (int i = 0; i < spikes.length && i < spikesList.size(); i++) {
                Object o = spikesList.get(i);
                spikes[i] = (o instanceof Number) ? ((Number) o).intValue() : 0;
            }
            cell.setSpikeTrain(spikes);

            row++;
            if (row % 50 == 0) IJ.showProgress(row, cells.size());
        }
    }

    public void getResultsTable() {
        if (!imp.getTitle().contains("_DELTAF")) {
            IJ.log("WARNING: Image series may not be in converted Delta F/F format");
        }

        IJ.showStatus("Generating results...");

        int nSlices = imp.getNSlices();
        int totalCells = cells.size();
        String filterName = filters[Math.max(0, Math.min(this.filter, filters.length - 1))];
        String detectionMethodName = detectionMethods[Math.max(0, Math.min(this.detectionMethod, detectionMethods.length - 1))];

        String[] sliceLabels = new String[nSlices];
        for (int i = 0; i < nSlices; i++) sliceLabels[i] = "Slice_" + (i + 1);

        File tempDir = new File(IJ.getDirectory("temp"));
        if (!tempDir.exists()) tempDir.mkdirs();
        File rawFile = new File(tempDir, "signal_results.csv");
        File stimFile = (this.detectionMethod == PEAK_LAGGING_WINDOW)
                ? new File(tempDir, "spike_results.csv")
                : null;

        try (
                BufferedWriter rawOut = new BufferedWriter(new FileWriter(rawFile));
                BufferedWriter stimOut = (stimFile != null) ? new BufferedWriter(new FileWriter(stimFile)) : null
        ) {
            // headers
            writeHeader(rawOut, sliceLabels);
            if (stimOut != null) writeHeader(stimOut, sliceLabels);

            // rows
            final int updateEvery = 1;
            for (int r = 0; r < totalCells; r++) {
                CellData cell = cells.get(r);
                String roi = cell.getName();
                String group = getCellGroupName(cell);
                double cx = cell.getCenterX();
                double cy = cell.getCenterY();

                // Line buffers
                StringBuilder sbRaw = new StringBuilder(64 + 16 * nSlices);
                appendMeta(sbRaw, this.exportName, roi, group, cx, cy, filterName, detectionMethodName);

                double[] sig = cell.getSignal();
                for (int s = 0; s < nSlices; s++) sbRaw.append(',').append(sig[s]);
                rawOut.write(sbRaw.append('\n').toString());

                if (stimOut != null) {
                    StringBuilder sbStim = new StringBuilder(64 + 16 * nSlices);
                    appendMeta(sbStim, this.exportName, roi, group, cx, cy, filterName, detectionMethodName);

                    int[] spike = cell.getSpikeTrain();
                    for (int s = 0; s < nSlices; s++) sbStim.append(',').append(spike[s]);
                    stimOut.write(sbStim.append('\n').toString());
                }

                IJ.showProgress(r, totalCells);
            }
        } catch (IOException e) {
            IJ.handleException(e);
            return;
        }

        new Thread(() -> {
            try {
                ResultsTable.open(rawFile.getAbsolutePath()).show("Signal Results");
                if (stimFile != null) {
                    ResultsTable.open(stimFile.getAbsolutePath()).show("Peak Detection Results");
                }
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }, "Open-Results").start();
    }

    private static void writeHeader(Writer w, String[] sliceLabels) throws IOException {
        w.write("Name,ROI,Group,X,Y,Filter,Detection.Method");
        for (String lab : sliceLabels) {
            w.write(',');
            w.write(lab);
        }
        w.write('\n');
    }

    private static void appendMeta(StringBuilder sb, String name, String roi, String group,
                                   double cx, double cy, String filterName, String detectionMethod) {
        sb.append(name).append(',')
                .append(roi).append(',')
                .append(group).append(',')
                .append(cx).append(',')
                .append(cy).append(',')
                .append(filterName).append(',')
                .append(detectionMethod);
    }

    private void buildGroupCache() {
        groupNameByRoi.clear();
        if (groups == null) return;
        for (GroupData g : groups) {
            String prefix = g.name + ":";
            for (CellData c : g.getCellsInGroup()) {
                Roi r = c.getCellRoi();
                if (r != null) {
                    String old = groupNameByRoi.get(r);
                    groupNameByRoi.put(r, old == null ? prefix : old + prefix);
                }
            }
        }
    }

    private String getCellGroupName(CellData cell) {
        Roi r = cell.getCellRoi();
        String name = groupNameByRoi.get(r);
        return name == null ? "" : name;
    }

    public void setParameters() {
        Preferences prefs = Preferences.userNodeForPackage(Exporter.class);
        this.nIterations = prefs.getInt(PREF_ITERATIONS, this.nIterations);
        this.stimulusPointsInput = prefs.get(PREF_STIMULUS, this.stimulusPointsInput);
        this.stimulusNamesInput = prefs.get(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
        this.detectionMethod = prefs.getInt(PREF_METHOD, this.detectionMethod);
        this.filter = prefs.getInt(PREF_FILTER, this.filter);

        GenericDialog gd = getGenericDialog();

        if (gd.wasOKed()) {
            this.exportName = gd.getNextString();
            this.nIterations = (int) gd.getNextNumber();
//            this.stimulusNamesInput = gd.getNextString();
//            this.stimulusPointsInput = gd.getNextString();
            this.detectionMethod = gd.getNextChoiceIndex();
            this.filter = gd.getNextChoiceIndex();

            if (this.detectionMethod == PEAK_LAGGING_WINDOW) {
                this.lag = prefs.getInt(PREF_LAG, this.lag);
                this.threshold = prefs.getDouble(PREF_THRESHOLD, this.threshold);
                this.influence = prefs.getDouble(PREF_INFLUENCE, this.influence);

                GenericDialog gd_peak = new GenericDialog("Peak detection");
                gd_peak.addMessage("Input parameters for peak detection");
                gd_peak.addNumericField("Lag: ", this.lag);
                gd_peak.addNumericField("Threshold: ", this.threshold);
                gd_peak.addNumericField("Influence: ", this.influence);
                gd_peak.showDialog();

                if (gd_peak.wasOKed()) {
                    this.lag = (int) gd_peak.getNextNumber();
                    this.threshold = gd_peak.getNextNumber();
                    this.influence = gd_peak.getNextNumber();

                    prefs.putInt(PREF_LAG, this.lag);
                    prefs.putDouble(PREF_THRESHOLD, this.threshold);
                    prefs.putDouble(PREF_INFLUENCE, this.influence);
                } else {
                    return;
                }
            }

            if (this.filter == FILTER_GAUSSIAN) {
                this.sigma = prefs.getDouble(PREF_SIGMA, this.sigma);

                GenericDialog gdFilter = new GenericDialog("Gaussian Filter");
                gdFilter.addMessage("Input parameters for gaussian filter");
                gdFilter.addNumericField("Sigma: ", this.sigma);
                gdFilter.showDialog();

                if (gdFilter.wasOKed()) {
                    prefs.putDouble(PREF_SIGMA, this.sigma);
                } else {
                    return;
                }
            }


            prefs.putInt(PREF_ITERATIONS, this.nIterations);
            prefs.put(PREF_STIMULUS, this.stimulusPointsInput);
            prefs.put(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
            prefs.putInt(PREF_METHOD, this.detectionMethod);
            prefs.putInt(PREF_FILTER, this.filter);
        }
    }

    public void getIterations() {
        int[] windowList = WindowManager.getIDList();
        if (windowList == null || windowList.length == 0) {
            new Popup("Error", "No images open.").showPopup();
            return;
        }

        String[] impTitles = new String[windowList.length];
        for (int i = 0; i < windowList.length; i++) {
            ImagePlus im = WindowManager.getImage(windowList[i]);
            impTitles[i] = im != null ? im.getTitle() : "N/A";
        }

        GenericDialog gd = new GenericDialog("Select Iterations");
        for (int i = 1; i <= nIterations; i++) gd.addChoice("Iteration " + i + ":", impTitles, impTitles[0]);
        gd.showDialog();

        if (gd.wasOKed()) {
            iterations.clear();
            for (int i = 0; i < nIterations; i++) {
                String title = impTitles[gd.getNextChoiceIndex()];
                ImagePlus sel = WindowManager.getImage(title);
                if (sel != null) iterations.add(sel);
            }
        } else if (gd.wasCanceled()) {
            setParameters();
        }
    }

    private GenericDialog getGenericDialog() {
        GenericDialog gd = new GenericDialog("Exporter Settings");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addStringField("Name: ", this.imp.getTitle().trim());
        gd.addNumericField("Num videos:", this.nIterations);
        gd.addChoice("Response call method", detectionMethods, detectionMethods[Math.max(0, Math.min(this.detectionMethod, detectionMethods.length - 1))]);
        gd.addChoice("Filtering method", filters, filters[Math.max(0, Math.min(this.filter, filters.length - 1))]);
        gd.showDialog();
        return gd;
    }

}
