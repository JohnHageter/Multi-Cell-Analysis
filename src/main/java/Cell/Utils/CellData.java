package Cell.Utils;

import ij.gui.Roi;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CellData {
    private Roi cellRoi;
    private Roi groupRoi;
    private String name;
    private ArrayList<String> groups = new ArrayList<>();
    private List<Roi> groupRois = new ArrayList<>();  // Store group ROIs here
    private int centerX;
    private int centerY;
    private double fnot;
    private double[] signal;

    public static CellData previousCellData;
    private int[] spikeTrain;

    public CellData(Roi cell) {
        this.cellRoi = cell;
        this.name = cell.getName();
        this.centerX = (int) cell.getBounds().getCenterX();
        this.centerY = (int) cell.getBounds().getCenterY();
    }

    public int getCenterX() {
        return centerX;
    }
    public int getCenterY() {
        return centerY;
    }
    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public Roi getCellRoi() {
        return cellRoi;
    }

    public void setRoi(Roi cellRoi) {
        this.cellRoi = cellRoi;
        if (cellRoi != null) {
            cellRoi.setStrokeColor(Color.RED);
        }
    }

    public void setSignal(double[] signal) {
        this.signal = signal;
    }

    public double[] getSignal() {
        return this.signal;
    }

    public void setSpikeTrain(List<Integer> spikeTrain) {
        int[] ret = new int[spikeTrain.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = spikeTrain.get(i);
        }
        this.spikeTrain = ret;
    }

    public int[] getSpikeTrain() {
        return this.spikeTrain;
    }

}
