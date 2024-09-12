package Cell.Analysis;

import Cell.UI.Popup;
import Cell.Utils.CellData;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class Exporter {
    private static final String PREF_SAVE = "Save";
    private static final String PREF_ITERATIONS = "Iterations";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMULUS_NAMES = "Stimpoint";
    private static final String PREF_METHOD = "Method";

    private String stimulusNamesInput = "";
    private String stimulusPointsInput = "";
    private int method = 0;
    private int nIterations = 2;

    ArrayList<ImagePlus> iterations = new ArrayList<ImagePlus>();

    private ResultsTable rt_raw;
    private ResultsTable rt_stim;

    public Exporter(ArrayList<CellData> cells){}

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

        getResultsTable(averageImp);
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

    public void getResultsTable(ImagePlus imp){

    }

    public void setParameters() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(Exporter.class);
        this.nIterations = prefs.getInt(PREF_ITERATIONS, this.nIterations);
        this.stimulusPointsInput = prefs.get(PREF_STIMULUS, this.stimulusPointsInput);
        this.stimulusNamesInput = prefs.get(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
        this.method = prefs.getInt(PREF_METHOD, this.method);

        GenericDialog gd = getGenericDialog();

        if (gd.wasOKed()) {
            this.nIterations = (int) gd.getNextNumber();
            this.stimulusNamesInput = gd.getNextString();
            this.stimulusPointsInput = gd.getNextString();
            this.method = gd.getNextChoiceIndex();

            prefs.clear();
            prefs.putInt(PREF_ITERATIONS, this.nIterations);
            prefs.put(PREF_STIMULUS, this.stimulusPointsInput);
            prefs.put(PREF_STIMULUS_NAMES, this.stimulusNamesInput);
            prefs.putInt(PREF_METHOD, this.method);
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
        String[] method = new String[]{">3σ", "Peak Detection", "none"};

        GenericDialog gd = new GenericDialog("Exporter Settings");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addNumericField("Iterations:", this.nIterations);
        gd.addMessage("Input stimulus as frames points with a duration to search separated by a comma (ex. 60-63,120-123)\nThis will average relative intensity change from frame 60-63 and 120-123");
        gd.addStringField("Stimulus time(s)", this.stimulusNamesInput);
        gd.addStringField("Stimulus Names", this.stimulusPointsInput);
        gd.addChoice("Response call method", method, method[this.method]);
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
