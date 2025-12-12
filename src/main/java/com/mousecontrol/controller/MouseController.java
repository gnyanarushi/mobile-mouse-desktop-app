package com.mousecontrol.controller;


import java.awt.*;
import java.awt.event.InputEvent;

public class MouseController {

    private  final Robot robot ;

    public MouseController() {
            try {
                    this.robot = new Robot();
                    this.robot.setAutoDelay(0);
            }
            catch (AWTException awtException ) {
                throw new RuntimeException("Failed to create Robot instance", awtException);
            }
    }



    public void moveBy(int dx, int dy) {
        // Get current mouse position
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;

        Point currentPos = pointerInfo.getLocation();

        int newX = currentPos.x + dx;
        int newY = currentPos.y + dy;

        robot.mouseMove(newX, newY);
    }


    public void leftClick() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }




    public void rightClick() {
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
    }

}
