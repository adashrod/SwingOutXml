package com.aaron.swingoutxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Listener is used on fields in GUI classes, e.g. on a MouseListener that is a field in a JFrame, to cause that listener
 * to be registered on a component.
 *
 * It has two optional properties: value is an array of XML element IDs. If the value(s) is/are omitted, then this is
 * effectively a marker interface.
 * The property addFunction can be used to specify the name of the function that is used to add the listener to a
 * component when using user-defined EventListener and JComponent classes. I.e. SwingOutXml can figure out how to add
 * the standard AWT and Swing listeners, like a MouseListener by calling {@link java.awt.Component#addMouseListener(java.awt.event.MouseListener)},
 * but doesn't know how to add custom EventListeners, so addFunction needs to be specified to tell it how to add the
 * listener.
 * using custom listeners with addFunction has the following requirements:
 * - the listener class must extend{@link java.util.EventListener}
 * - the add function must take one parameter
 *
 * Examples:
 * XML listeners only:
 * ...
 * <j-label id="label0" listeners="myMouseListener">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     MouseListener myMouseListener = new MouseListener() {
 *         public void mouseEntered(final MouseEvent e) {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * XML listeners only, with \@Listener (marker annotation, has no effect)
 * ...
 * <j-label id="label0" listeners="myMouseListener">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     \@Listener MouseListener myMouseListener = new MouseListener() {
 *         public void mouseEntered(final MouseEvent e) {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * annotation only:
 * ...
 * <j-label id="label0">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     \@Listener({"label0"}) MouseListener myMouseListener = new MouseListener() {
 *         public void mouseEntered(final MouseEvent e) {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * both bindings (redundant):
 * ...
 * <j-label id="label0" listeners="myMouseListener">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     \@Listener({"label0"}) MouseListener myMouseListener = new MouseListener() {
 *         public void mouseEntered(final MouseEvent e) {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * addFunction examples for user-defined JComponents and EventListeners:
 *
 * the function named by addFunction should be a public-access function on the class to which the listener is being added;
 * in the following examples: com.package.CustomJComponent. It might look like this:
 * public class CustomJComponent extends JPanel {
 *     ...
 *     public void addCustomListener(final CustomListener customListener) {
 *         this.listeners.add(customListener);
 *     }
 *     ...
 * XML listeners only:
 * ...
 * <com.package.CustomJComponent id="label0" listeners="addCustomListener(myCustomListener)">...</com.package.CustomJComponent>
 * ...
 * public class MyGui extends JFrame {
 *     CustomListener myCustomListener = new CustomListener() {
 *         public void onEvent() {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * XML listeners only, with \@Listener (marker annotation, has no effect)
 * ...
 * <com.package.CustomJComponent id="label0" listeners="addCustomListener(myCustomListener)">...</com.package.CustomJComponent>
 * ...
 * public class MyGui extends JFrame {
 *     \@Listener CustomListener myCustomListener = new CustomListener() {
 *         public void onEvent() {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * annotation only:
 * ...
 * <com.package.CustomJComponent id="label0">...</com.package.CustomJComponent>
 * ...
 * public class MyGui extends JFrame {
 *     \@Listener(addFunction = "addCustomListener", value = {"label0"}) CustomListener myCustomListener = new CustomListener() {
 *         public void onEvent() {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 * both bindings (redundant):
 * ...
 * <com.package.CustomJComponent id="label0" listeners="addCustomListener(myCustomListener)">...</com.package.CustomJComponent>
 * ...
 * public class MyGui extends JFrame {
 *     \@Listener(addFunction = "addCustomListener", value = {"label0"}) CustomListener myCustomListener = new CustomListener() {
 *         public void onEvent() {
 *             ...
 *         }
 *         ...
 *     };
 *     ...
 *
 *
 * The listeners attribute in the XML has higher precedence than the value of the annotation. Since using both methods
 * of binding is redundant, if both are specified the value of the \@Listener will be ignored.
 * @see com.aaron.swingoutxml.annotation.UiComponent
 * @see com.aaron.swingoutxml.annotation.ComponentAction
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {
    /**
     * @return IDs of XML elements to add this listener to
     */
    String[] value() default {};

    /**
     * @return the name of the function to use to add this listener to the specified component(s)
     */
    String addFunction() default "";
}
