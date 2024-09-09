package Cell;

import ij.ImageJ;
import ij.plugin.PlugIn;
import Cell.Frame.CellManager;

public class Main implements PlugIn {

    public static void main(String[] args){
        new CellManager().setVisible(true);
        new ImageJ();
    }

    @Override
    public void run(String arg) {
        new CellManager().setVisible(true);

    }
}
