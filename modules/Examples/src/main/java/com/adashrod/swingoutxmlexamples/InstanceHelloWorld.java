package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@SwingOutContainer(template = "/template/instanceHelloWorld.xml")
public class InstanceHelloWorld {
    @UiComponent JFrame theFrame;
    @UiComponent JButton button;
    @UiComponent JLabel hello;

    private final ActionListener buttonListener = (final ActionEvent e) -> {
        hello.setVisible(!hello.isVisible());
    };

    public InstanceHelloWorld() {
        System.out.println(String.format("HW constructed without args"));
    }

    public InstanceHelloWorld(final int a) {
        System.out.println(String.format("HW constructed with %s", a));
    }
}
