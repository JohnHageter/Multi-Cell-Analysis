package Cell.Frame;

import ij.IJ;
import ij.plugin.PlugIn;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CellManagerHotkey implements PlugIn {

    @Override
    public void run(String arg) {
        IJ.getInstance().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_M) && ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    CellManager.showCellManager();
                }
            }
        });
    }
}
