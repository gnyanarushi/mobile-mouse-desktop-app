package com.mousecontrol.models;
public class MotionData {
    public final double gyroX;
    public final double gyroY;

    public final boolean leftClick;
    public final boolean rightClick;

    public MotionData(double gyroX, double gyroY, boolean leftClick, boolean rightClick) {
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }


    // toString method to display the data
    @Override
    public String toString() {
        return "MotionData{" +
                "gyroX=" + gyroX +
                ", gyroY=" + gyroY +
                ", leftClick=" + leftClick +
                ", rightClick=" + rightClick +
                '}';
    }
}
