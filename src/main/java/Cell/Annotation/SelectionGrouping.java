package Cell.Annotation;

import Cell.Utils.CellData;
import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectionGrouping implements DialogListener {
    private Map<String, Roi> roiMap = new HashMap<>();

    public void applyGroup(ArrayList<CellData> cells, Roi groupingRoi, String groupName) {
        for (CellData cell : cells) {
            int centerX = cell.getCenterX();
            int centerY = cell.getCenterY();
            if(groupingRoi.contains(centerX, centerY)) {
                cell.addGroup(groupName);
                cell.setGroupRoi(groupingRoi);
                roiMap.put(groupName, groupingRoi);
            }
        }

        if (roiMap.isEmpty()){
            IJ.log("Empty groups will not be added to the cell manager.");
        }
    }

    public Roi getRoiForGroup(String group) {
        return roiMap.get(group);
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return true;
    }


}
