package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.PostSetup;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

@SwingOutContainer(template = "/template/aBunchOfStuff.xml")
public class ABunchOfStuff extends JFrame implements PostSetup {
    @UiComponent("p1") JPanel p1;
    @UiComponent("p2") JPanel p2;
    @UiComponent("p3") JPanel p3;
    @UiComponent("p4") JPanel p4;

    @Override
    public void afterCreate() {
        p1.setBorder(new BevelBorder(BevelBorder.LOWERED));
        p2.setBorder(new BevelBorder(BevelBorder.LOWERED));
        p3.setBorder(new BevelBorder(BevelBorder.LOWERED));
        p4.setBorder(new BevelBorder(BevelBorder.LOWERED));
    }
}
