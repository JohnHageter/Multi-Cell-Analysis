package Cell.Utils;

import ij.gui.Roi;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public class CellData {
    private Roi cellRoi;
    private Roi groupRoi;
    private String name;
    private ArrayList<String> groups = new ArrayList<>();
    private int centerX;
    private int centerY;
    private double fnot;
    private double[] signal;

    public static CellData previousCellData;
    private int[] spikeTrain;

    public CellData(Roi cell){
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

    public String getName(){
        return name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public ArrayList<String> getGroups(){
        return groups;
    }

    public void addGroup(String group){
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
    }

    public void removeGroup(String group) {
        this.groups.remove(group);
    }

    public void setGroupRoi(Roi groupRoi) {
        this.groupRoi = groupRoi;
    }

    public Roi getGroupRoi() {
        return groupRoi;
    }

    public Roi getCellRoi() {
        return cellRoi;
    }

    public void setRoi(Roi cellRoi){
        this.cellRoi = cellRoi;
        if (cellRoi != null) {
            cellRoi.setStrokeColor(Color.RED);
        }
    }

    public String[] breakName(String delimiter) {
        return this.getName().split(delimiter);
    }

    public void setSignal(double[] signal) {
        this.signal = signal;
    }

    public double[] getSignal(){
        return this.signal;
    }

    public String printData() {
        return "Cell Data{" +
                "Name= " + this.name +
                "Center X= " + this.centerX +
                ", Center Y= " + this.centerY +
                "}";
    }

    public void setSpikeTrain(int[] spikeTrain){
        this.spikeTrain = spikeTrain;
    }

    public int[] getBinary() {
        return this.spikeTrain;
    }
}
