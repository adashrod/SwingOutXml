package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.annotation.SwingOutContainer;

import javax.swing.JDialog;
import javax.swing.JLabel;
import java.awt.Frame;

@SwingOutContainer(template = "/template/dialogChildWindow.xml")
public class DialogChild extends JDialog {
    private JLabel label1;
    private JLabel label2;
    private JLabel label3;

    public DialogChild(final Frame parent) {
        super(parent);
    }
}
