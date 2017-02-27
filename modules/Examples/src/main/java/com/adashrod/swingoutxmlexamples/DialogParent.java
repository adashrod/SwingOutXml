package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.PostSetup;
import com.adashrod.swingoutxml.SwingOutXml;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import org.xml.sax.SAXException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

@SwingOutContainer(template = "/template/dialogParentWindow.xml")
public class DialogParent extends JFrame implements PostSetup {
    private JButton button;
    private JLabel label;

    private DialogChild dialogChild;

    private final ActionListener buttonListener = (final ActionEvent e) -> {
        dialogChild.setVisible(true);
    };

    @Override
    public void afterCreate() {
        try {
            dialogChild = SwingOutXml.create(DialogChild.class);
        } catch (final IOException | SAXException | InvocationTargetException | NoSuchMethodException | ParseException e) {
            e.printStackTrace();
        }
    }
}
