package Cell.Utils;

import ij.gui.Roi;

import java.awt.*;
import java.util.Objects;

public class CellData {
    private Roi cellRoi;
    private Roi groupRoi;
    private String name;
    private String group = null;
    private int centerX;
    private int centerY;
    private double fnot;
    private double[] df;

    public static CellData previousCellData;

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

    public String getGroup(){return !Objects.equals(this.group, "") ?this.group:"";}

    public void setGroup(String group){
        this.group = group;
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
            cellRoi.setStrokeColor(Color.RED); // Set the cell ROI color to red
        }
    }

    public String[] breakName(String delimiter) {
        return this.getName().split(delimiter);
    }

    public String printData() {
        return "Cell Data{" +
                "Name= " + this.name +
                "Center X= " + this.centerX +
                ", Center Y= " + this.centerY +
                "}";
    }

}
