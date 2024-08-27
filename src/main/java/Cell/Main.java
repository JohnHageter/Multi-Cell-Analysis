package Cell;

import Cell.Frame.CellManager;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class Main implements PlugIn {

    public static void main(String[] args){
        new CellManager();
        new ImageJ();
    }

    @Override
    public void run(String arg) {
        new CellManager();
    }
}
