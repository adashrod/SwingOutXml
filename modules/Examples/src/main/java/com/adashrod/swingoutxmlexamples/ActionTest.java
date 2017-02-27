package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.annotation.ComponentAction;
import com.adashrod.swingoutxml.annotation.Listener;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Example with 2 buttons, label, action on the buttons, and another button to enable/disable the action.
 */
@SwingOutContainer(template = "/template/actionTest.xml")
public class ActionTest extends JFrame {
    @UiComponent("button1") JButton button1;
    @UiComponent("button2") JButton button2;
    @UiComponent("button3") JButton button3;
    @UiComponent("actionHelloPanel") JLabel hello;

    @ComponentAction({"button1", "button2"}) private final Action buttonAction = new AbstractAction() {
        {
            putValue(Action.NAME, "click me");
            putValue(Action.SHORT_DESCRIPTION, "clkm");
            putValue(Action.LONG_DESCRIPTION, "click me to display/hide the message");
        }
        public void actionPerformed(final ActionEvent e) {
            hello.setVisible(!hello.isVisible());
        }
    };

    @Listener({"button3"}) private final ActionListener buttonListener = (final ActionEvent e) -> {
        buttonAction.setEnabled(!buttonAction.isEnabled());
    };
}
