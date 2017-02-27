package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple example with a button, label, and action listener on the button
 */
@SwingOutContainer(template = "/template/helloWorld.xml")
public class HelloWorld extends JFrame {
    @UiComponent JButton button;
    @UiComponent JLabel hello;

    final private ActionListener buttonListener = (final ActionEvent e) -> {
        hello.setVisible(!hello.isVisible());
    };

    public HelloWorld() {
        System.out.println(String.format("HW constructed without args"));
    }

    public HelloWorld(final int a) {
        System.out.println(String.format("HW constructed with %s", a));
    }
}
