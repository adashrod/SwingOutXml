package com.aaron.swingoutxml;

/**
 * This is an optional interface to be implemented by classes annotated by \@{@link com.aaron.swingoutxml.annotation.SwingOutContainer}
 * and created using {@link SwingOutXml#create(Class)}. It provides an API for one function: afterCreate(),
 * which is run after instantiation and creation of the GUI components. It can be used to attach event listeners to GUI
 * components or otherwise modifying components as an alternative to doing so in the XML.
 *
 * Complete Example:
 * \@SwingToolkit(template = "path/to/templateFile.xml")
 * public class MyGui extends JFrame implements PostSetup {
 *     JPanel panel;
 *     JButton button;
 *     JLabel label;
 *
 *     final ActionListener buttonListener = (final ActionEvent e) -> {
 *         label.setVisible(!label.isVisible());
 *     }
 *
 *     \@Override
 *     public void afterCreate() {
 *         // afterCreate() is called after components have been instantiated, laid out, and displayed
 *         label.setVisible(false);
 *         button.addActionListener(buttonListener);
 *     }
 * }
 *
 * templateFile.xml
 * <j-frame>
 *     <j-panel layout="java.awt.FlowLayout">
 *         <j-button field="button">click me!</j-button>
 *         <j-label field="label">Hello World!</j-label>
 *     </j-panel>
 * </j-frame>
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public interface PostSetup {
    void afterCreate();
}
