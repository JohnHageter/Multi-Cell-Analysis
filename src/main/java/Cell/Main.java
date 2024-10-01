package Cell;

import Cell.Frame.CellManagerHotkey;
import ij.ImageJ;
import ij.plugin.PlugIn;
import Cell.Frame.CellManager;

public class Main implements PlugIn {

    public static void main(String[] args){
        new CellManager().setVisible(true);
        new ImageJ();
        //new CellManagerHotkey().run("");
    }

    @Override
    public void run(String arg) {
        new CellManager().setVisible(true);
    }
}
