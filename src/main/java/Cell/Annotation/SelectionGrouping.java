package Cell.Annotation;

import Cell.Utils.CellData;
import Cell.Utils.GroupData;
import ij.IJ;
import ij.gui.Roi;
import java.util.ArrayList;


public class SelectionGrouping {
    public GroupData applyGroup(ArrayList<CellData> cells, Roi groupingRoi, String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            IJ.log("Group name is empty; no groups will be added.");
            return null;
        }

        boolean groupAdded = false;
        GroupData group = new GroupData(groupName);
        group.setRoi((Roi) groupingRoi.clone());

        for (CellData cell : cells) {
            int centerX = cell.getCenterX();
            int centerY = cell.getCenterY();

            if (groupingRoi.contains(centerX, centerY)) {
                group.addCell(cell);
                groupAdded = true;
            }
        }

        if (!groupAdded) {
            IJ.log("No groups were added as no cells fell within the grouping ROI.");
        } else if (group.cells.isEmpty()) {
            IJ.log("Empty groups will not be added to the cell manager.");
        }

        return group;
    }
}
