package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.PostSetup;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

@SwingOutContainer(template = "/template/fancyPanel.xml")
public class FancyPanel extends JPanel implements PostSetup {
    @UiComponent JButton button;
    @UiComponent JLabel label;
    private CoolListener coolListener;

    private final ActionListener buttonListener = (final ActionEvent e) -> {
        label.setText(label.getText() + "!");
        if (coolListener != null) {
            System.out.println("firing cool event");
            coolListener.onStuff();
        }
    };

    public void addListener(final CoolListener coolListener) {
        System.out.println("adding cool listener");
        this.coolListener = coolListener;
    }

    public void addListener(final MouseListener mouseListener) {
        System.out.println("adding mouse listener");
    }

    @Override
    public void afterCreate() {
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    }
}
