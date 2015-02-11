package com.aaron.swingoutxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ComponentAction is used on Actions in GUI classes, e.g. on an AbstractAction that is a field in a JFrame, to cause that
 * action to be registered on a component.
 *
 * It has an optional value that is an array of XML element IDs. If the value(s) is/are omitted, then this is effectively
 * a marker interface
 *
 * Examples:
 * XML action only:
 * ...
 * <j-button id="button0" action="myAction">text...</j-button>
 * ...
 * public class MyGui extends JFrame {
 *     Action myAction = new AbstractAction() {
 *         {
 *             putValue(Action.NAME, "click me");
 *         }
 *         public void actionPerformed(final ActionEvent e) {
 *             ...
 *         }
 *     };
 *     ...
 *
 * XML action only, with \@ComponentAction (marker annotation, has no effect)
 * ...
 * <j-button id="button0" action="myAction">text...</j-button>
 * ...
 * public class MyGui extends JFrame {
 *     \@ComponentAction Action myAction = new AbstractAction() {
 *         {
 *             putValue(Action.NAME, "click me");
 *         }
 *         public void actionPerformed(final ActionEvent e) {
 *             ...
 *         }
 *     };
 *     ...
 *
 * annotation only:
 * ...
 * <j-button id="button0">text...</j-button>
 * ...
 * public class MyGui extends JFrame {
 *     \@ComponentAction({"button0"}) Action myAction = new AbstractAction() {
 *         {
 *             putValue(Action.NAME, "click me");
 *         }
 *         public void actionPerformed(final ActionEvent e) {
 *             ...
 *         }
 *     };
 *     ...
 *
 * both bindings (redundant):
 * ...
 * <j-button id="button0" action="myAction">text...</j-button>
 * ...
 * public class MyGui extends JFrame {
 *     \@ComponentAction({"button0"}) Action myAction = new AbstractAction() {
 *         {
 *             putValue(Action.NAME, "click me");
 *         }
 *         public void actionPerformed(final ActionEvent e) {
 *             ...
 *         }
 *     };
 *     ...
 *
 * The action attribute in the XML has higher precedence than the value of the annotation. Since using both methods
 * of binding is redundant, if both are specified the value of the \@ComponentAction will be ignored.
 * @see com.aaron.swingoutxml.annotation.UiComponent
 * @see com.aaron.swingoutxml.annotation.Listener
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentAction {
    /**
     * @return IDs of XML elements to attach this action to
     */
    String[] value() default {};
}
