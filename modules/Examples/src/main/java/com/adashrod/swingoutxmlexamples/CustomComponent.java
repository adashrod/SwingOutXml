package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.annotation.Listener;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;

import javax.swing.JFrame;

/**
 * This example uses elements that are custom JComponents, i.e. user-defined classes that extend an existing JComponent
 * subclass.
 */
@SwingOutContainer(template = "/template/customComponent.xml")
public class CustomComponent extends JFrame {
    @UiComponent private FancyPanel fancyPanel1;
    @Listener(addFunction = "addListener", value = {"fancyPanel1", "fancyPanel2"}) private final CoolListener myCoolListener = () -> {
        System.out.println("clicked the cool button");
    };

    private final Object object = new Object() {
        public final String cent = "Center";
    };
}
