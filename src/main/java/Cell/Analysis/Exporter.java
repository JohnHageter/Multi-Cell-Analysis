package Cell.Analysis;

import Cell.Utils.CellData;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;

import java.util.ArrayList;


public class Exporter {
    private static final String PREF_SAVE = "Save";
    private static final String PREF_ITERATIONS = "Iterations";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMPOINT = "Stimpoint";

    private String stimulusNamesInput;
    private String stimulusPointsInput;
    private int method = 0;

    private int nIterations;
    private int iterationSlices;

    private ResultsTable rt_raw;
    private ResultsTable rt_stim;

    public Exporter(ImagePlus imp, ArrayList<CellData> cells){
        this.iterationSlices = imp.getNSlices();
        boolean convertedFormat = imp.getTitle().contains("_DELTAF");
    }

    public void setParameters(){
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

        if (gd.wasOKed()) {
            this.nIterations = (int) gd.getNextNumber();
            //this.stimulusNamesInput = ;
        }
    }

    public double convertToTime(int slice, double framerate) {
        if (framerate <= 0) {
            framerate = 1;
        }
        return slice / framerate;
    }



    
}
