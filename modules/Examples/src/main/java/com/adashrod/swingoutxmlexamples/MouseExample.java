package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.PostSetup;
import com.adashrod.swingoutxml.annotation.Listener;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * example using a mouse listener
 */
@SwingOutContainer(template = "/template/mouse.xml")
public class MouseExample extends JFrame implements PostSetup {
    @UiComponent("label") private JLabel label;

    @Listener("panel") private final MouseListener mouseListener = new MouseAdapter() {
        public void mouseEntered(final MouseEvent e) {
            label.setText("on");
        }
        public void mouseExited(final MouseEvent e) {
            label.setText("off");
        }
    };

    @Override
    public void afterCreate() {
        label.setText("-");
    }
}
