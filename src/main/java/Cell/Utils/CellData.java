package Cell.Utils;

import ij.gui.Roi;

public class CellData {
    public Roi roi;
    private String name;
    private String group;
    private int centerX;
    private int centerY;
    private double fnot;
    private double[] df;

    public CellData(){
    }

    public CellData(Roi cell){
        this.roi = cell;
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
