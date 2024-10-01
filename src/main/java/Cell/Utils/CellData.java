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

    public ArrayList<String> getGroups() {
        return groups;
    }

    public void addGroup(String group) {
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
    }

    public void removeGroup(String group) {
        this.groups.remove(group);
        this.groupRois.remove(group);
    }

    public void addGroupRoi(Roi groupRoi) {
        if (!this.groupRois.contains(groupRoi)) {
            this.groupRois.add(groupRoi);
        }
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

    public List<Roi> getGroupRois() {
        return groupRois;
    }

    public List<Roi> getGroupRois(String group) {
        Map<String, List<Roi>> groupRoiMap = new HashMap<>();
        return groupRoiMap.getOrDefault(group, Collections.emptyList());
    }

    public String[] breakName(String delimiter) {
        return this.getName().split(delimiter);
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

    public void setSpikeTrain(int[] spikeTrain) {
        this.spikeTrain = spikeTrain;
    }
}
