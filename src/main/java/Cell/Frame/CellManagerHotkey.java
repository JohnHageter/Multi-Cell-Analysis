package Cell.Frame;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.PlugIn;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CellManagerHotkey implements PlugIn {

    @Override
    public void run(String arg) {
        IJ.getInstance().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                if ((e.getKeyCode() == KeyEvent.VK_M) && ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    CellManager.showCellManager();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
                    CellManager cm = CellManager.getInstance();
                    ImagePlus imp = WindowManager.getCurrentImage();

                    if (cm == null) {cm = new CellManager(); cm.setVisible(true); return;}
                    if (imp == null) { IJ.noImage(); return;}

                    Roi selection =  imp.getRoi();
                    if (selection == null) {
                        IJ.log("No active selection.");
                        return;
                    }
                    cm.addCell(selection, false, null, -1);
                    IJ.log("Added Cell:" + selection.getName());
                }
            }
        });
    }
}
