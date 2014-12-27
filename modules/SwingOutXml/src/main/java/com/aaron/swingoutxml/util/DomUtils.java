package com.aaron.swingoutxml.util;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utils for DOM-related operations
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public class DomUtils {
    /**
     * Gets an attribute on an element. Returns null if the attribute is missing or whitespace only
     * @param attribute name of the attribute to get
     * @param node      element to get an attribute on
     * @return attribute value, or null if the attribute was missing or whitespace only
     */
    public static String getAttribute(final String attribute, final Element node) {
        final String value = node.getAttribute(attribute).trim();
        return !value.isEmpty() ? value : null;
    }

    /**
     * Gets an attribute on an element, parses it as a comma-separated list, and returns that list with empty strings
     * removed.
     * @param attribute name of the attribute to get
     * @param node      element to get an attribute on
     * @return list of string values separated by commas in the attribute value; empty list if the attribute is empty
     */
    public static List<String> getAttributeAsList(final String attribute, final Element node) {
        final String value = getAttribute(attribute, node);
        if (value != null) {
            final Collection<String> list = new ArrayList<>();
            Collections.addAll(list, value.split("\\s*,\\s*"));
            return list.stream().filter((final String s) -> { return !s.isEmpty(); }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
}
