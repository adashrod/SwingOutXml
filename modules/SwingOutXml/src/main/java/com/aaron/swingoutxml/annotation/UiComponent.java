package com.aaron.swingoutxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * UiComponent is used on fields in GUI classes, e.g. on a JLabel that is a field in a JFrame, to bind that field
 * to the component specified in the XML declaration.
 *
 * It has an optional value that specifies an XML element ID. If the value is omitted, then this is effectively a
 * marker annotation. If specified, it can be used to bind the field to that XML element by the element's ID attribute.
 * This is an alternative to specifying a field attribute in the XML and setting that to the name of the field. When
 * using the field XML attribute, the UiComponent annotation is not needed. When using the \@UiComponent with value, the
 * XML field attribute is not needed.
 *
 * Examples:
 * XML field only:
 * ...
 * <j-label id="label0" field="myLabel">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     JLabel myLabel;
 *     ...
 *
 * XML field only, with \@UiComponent (marker annotation, has no effect):
 * ...
 * <j-label id="label0" field="myLabel">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     \@UiComponent JLabel myLabel;
 *     ...
 *
 * annotation only:
 * ...
 * <j-label id="label0"">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     \@UiComponent("label0") JLabel myLabel;
 *     ...
 *
 * both bindings (redundant):
 * ...
 * <j-label id="label0" field="myLabel">text...</j-label>
 * ...
 * public class MyGui extends JFrame {
 *     \@UiComponent("label0") JLabel myLabel;
 *     ...
 *
 * The field attribute in the XML has higher precedence than the value of the annotation. Since using both methods of
 * binding is redundant, if both are specified the value of the \@UiComponent annotation will be ignored.
 * @see com.aaron.swingoutxml.annotation.Listener
 * @see com.aaron.swingoutxml.annotation.ComponentAction
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
@Target({ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface UiComponent {
    String value() default "";
}
