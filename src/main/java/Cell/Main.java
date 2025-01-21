package Cell;

import ij.ImageJ;
import ij.plugin.PlugIn;
import Cell.Frame.CellManager;
import javax.swing.*;

public class Main implements PlugIn {

    public static void main(String[] args){
        new ImageJ();
        new CellManager();
    }

    @Override
    public void run(String arg) {
        SwingUtilities.invokeLater(CellManager::getInstance);}
}
