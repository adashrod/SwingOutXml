package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.PostSetup;
import com.adashrod.swingoutxml.annotation.CellRenderer;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;

/**
 * Created by aaron on 2015-10-07.
 */
@SwingOutContainer(template = "/template/cellRenderers.xml")
public class CellRenderers extends JFrame implements PostSetup {
    JList<String> list;
    DefaultListModel<String> listModel = new DefaultListModel<>();

    @SuppressWarnings("unchecked")
    @CellRenderer("myList") ListCellRenderer<String> cellRenderer = (ListCellRenderer) new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
            final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final String s = (String) value;
            setText(s.substring(0, s.length() > 1 ? 2 : s.length()));
            return component;
        }
    };

    @Override
    public void afterCreate() {
        listModel.addElement("blah");
        listModel.addElement("bleh");
        listModel.addElement("blurgh");
    }
}
