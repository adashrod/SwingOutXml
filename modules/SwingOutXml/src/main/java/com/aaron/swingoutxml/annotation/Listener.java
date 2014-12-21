package com.aaron.swingoutxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Listener is used on fields in GUI classes, e.g. on a MouseListener that is a field in a JFrame, to cause that listener
 * to be registered on a component.
 *
 * It has an optional value that is an array of XML element IDs. If the value(s) is/are omitted, then this is effectively
 * a marker interface
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
 * The listeners attribute in the XML has higher precedence than the value of the annotation. Since using both methods
 * of binding is redundant, if both are specified the value of the \@Listener will be ignored.
 * @see com.aaron.swingoutxml.annotation.UiComponent
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {
    String[] value() default {};
}
