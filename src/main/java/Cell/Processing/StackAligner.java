package Cell.Processing;

import ij.IJ;
import ij.ImagePlus;

public class StackAligner {
    private StackAligner(){}

    public void AlignTemplate(ImagePlus imp){
        if (!imp.hasImageStack()) {
            IJ.log("Only image stacks can be aligned.");
        }


    }
}
