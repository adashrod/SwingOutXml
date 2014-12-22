package com.aaron.swingoutxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes annotated with \@SwingOutXml can be instantiated using SwingOutXml, and have their layouts done exclusively in XML
 * instead of programmatically adding components in Java. Classes that use this annotation should also extend one of the
 * top-level container classes, such as JFrame or JDialog. todo: remove that sentence if applicable in the future
 * The annotation must specify a template, which is a path to the XML template file
 *
 * Complete Example:
 * \@SwingOutXml(template = "path/to/templateFile.xml")
 * public class MyGui extends JFrame {
 *     JPanel panel;
 *     JButton button;
 *     JLabel label;
 *
 *     final ActionListener buttonListener = (final ActionEvent e) -> {
 *         label.setVisible(!label.isVisible());
 *     }
 * }
 *
 * templateFile.xml
 * <j-frame>
 *     <j-panel layout="java.awt.FlowLayout">
 *         <j-button field="button" action-listeners="buttonListener">click me!</j-button>
 *         <j-label field="label" visible="false">Hello World!</j-label>
 *     </j-panel>
 * </j-frame>
 *
 * This example creates a JFrame with one child: a JPanel. The JPanel has a FlowLayout and two children: a JButton and a
 * JLabel. The field buttonListener in MyGui is attached to the button using addActionListener.
 *
 * Equivalent Java-only :
 * public class MyGui extends JFrame {
 *     JPanel panel;
 *     JButton button;
 *     JLabel label;
 *
 *     final ActionListener buttonListener = (final ActionEvent e) -> {
 *         label.setVisible(!label.isVisible());
 *     }
 *
 *     public MyGui() {
 *         panel.setLayout(new FlowLayout());
 *         button.setText("click me!")
 *         button.addActionListener(buttonListener);
 *         panel.add(button);
 *         label.setText("Hello World!");
 *         label.setVisible(false);
 *         panel.add(label);
 *         add(panel);
 *         pack();
 *         setVisible(true);
 *     }
 * }
 *
 * For details on configuring XML/annotations:
 * @see com.aaron.swingoutxml.annotation.UiComponent
 * @see com.aaron.swingoutxml.annotation.Listener
 * @see com.aaron.swingoutxml.annotation.ComponentAction
 * @see com.aaron.swingoutxml.PostSetup
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwingOutContainer {
    String template();
}
