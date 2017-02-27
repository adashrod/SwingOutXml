package com.adashrod.swingoutxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by aaron on 2015-10-07.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CellRenderer {
    /**
     * @return IDs of XML elements to set this cell renderer on
     */
    String[] value() default {};
}
