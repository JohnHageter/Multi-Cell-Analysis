package Cell.Annotation;

import Cell.Utils.CellData;
import Cell.Utils.GroupData;
import ij.IJ;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static Cell.Annotation.SelectMultiple.calcDistance;


public class SelectionGrouping {
    public static final int METHOD_POLYGON = 1;
    public static final int METHOD_POINT = 2;

    public GroupData applyGroup(ArrayList<CellData> cells, Roi groupingRoi, String groupName, int method) {
        if (groupName == null || groupName.isEmpty()) {
            IJ.log("Group name is empty; no groups will be added.");
            return null;
        }

        boolean groupAdded = false;
        GroupData group = new GroupData(groupName);
        switch (method) {
            case METHOD_POLYGON:
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
            case METHOD_POINT:
                FloatPolygon points = ((PolygonRoi) groupingRoi.clone()).getFloatPolygon();
                group.setRoi(groupingRoi);

                double[][] distances = new double[cells.size()][points.npoints];
                for (int cell = 0; cell < cells.size(); cell++) {
                    for (int point = 0; point < points.npoints; point++) {
                        double cellX = cells.get(cell).getCellRoi().getBounds().getCenterX();
                        double cellY = cells.get(cell).getCellRoi().getBounds().getCenterY();

                        distances[cell][point] = calcDistance(points.xpoints[point],
                                points.ypoints[point],
                                cellX,
                                cellY);
                    }
                }

                Set<Integer> addedCells = new HashSet<>();

                for (int point = 0; point < points.npoints; point++) {
                    double minDistance = Double.MAX_VALUE;
                    int minCellIndex = -1;
                    for (int cell = 0; cell < cells.size(); cell++) {
                        if (distances[cell][point] < minDistance && !addedCells.contains(cell)) {
                            minDistance = distances[cell][point];
                            minCellIndex = cell;
                        }
                    }

                    if (minCellIndex != -1) {
                        group.addCell(cells.get(minCellIndex));
                        IJ.log("Added cell: " + cells.get(minCellIndex).getName());
                        addedCells.add(minCellIndex);
                        groupAdded = true;
                    } else {
                        IJ.log("No Cell within range found.");
                    }
                }

                if (!groupAdded) {
                    IJ.log("No groups were added as no cells fell within the grouping ROI.");
                } else if (group.cells.isEmpty()) {
                    IJ.log("Empty groups will not be added to the cell manager.");
                }
                IJ.run("Select None");
                return group;
            default:
                IJ.log("No grouping method selected");
                break;
        }

        return group;
    }
}
