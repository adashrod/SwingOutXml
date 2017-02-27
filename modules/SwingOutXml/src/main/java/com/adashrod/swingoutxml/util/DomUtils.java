package com.adashrod.swingoutxml.util;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
    private static final String toStringIndent = "    ";

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
     * Gets an attribute on an element. Returns null if the attribute is missing or whitespace only
     * @param attribute name of the attribute to get
     * @param node      element to get an attribute on
     * @param type      a return type to cast the attribute value to (Boolean|Short|Integer|Long|Float|Double|String)
     * @return attribute value, or null if the attribute was missing or whitespace only
     */
    public static <T> T getAttribute(final String attribute, final Element node, final Class<T> type) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException(String.format("Can't use primitive types in getAttribute() since missing attribute values are null: %s", type));
        }
        final String value = getAttribute(attribute, node);
        if (value == null) {
            return null;
        }
        if (type == Boolean.class) {
            return type.cast(Boolean.parseBoolean(value));
        } else if (type == Short.class) {
            return type.cast(Short.parseShort(value));
        } else if (type == Integer.class) {
            return type.cast(Integer.parseInt(value));
        } else if (type == Long.class) {
            return type.cast(Long.parseLong(value));
        } else if (type == Float.class) {
            return type.cast(Float.parseFloat(value));
        } else if (type == Double.class) {
            return type.cast(Double.parseDouble(value));
        } else if (type == String.class) {
            return type.cast(value);
        } else {
            throw new IllegalArgumentException(String.format("Invalid type for getAttribute(): %s", type));
        }
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

    /**
     * Pretty, recursive indentation helper for {@link com.adashrod.swingoutxml.util.DomUtils#toString(org.w3c.dom.Element)}
     * @param element an element to stringify
     * @param indent  level of tab indentation for level of element depth
     * @return an XML-like representation of the XML element
     */
    private static String toString(final Element element, final String indent) {
        final NamedNodeMap namedNodeMap = element.getAttributes();
        final StringBuilder result = new StringBuilder();
        result.append(indent).append("<").append(element.getTagName());
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            final Attr attribute = (Attr) namedNodeMap.item(i);
            result.append(" ").append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\"");
        }

        final boolean noChildren = element.getChildNodes().getLength() == 0;
        final boolean oneTextChild = element.getChildNodes().getLength() == 1 && element.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE;
        final boolean oneNonWhitespaceTextChild = oneTextChild && !element.getChildNodes().item(0).getNodeValue().trim().isEmpty();
        final boolean oneWhitespaceTextChild = oneTextChild && element.getChildNodes().item(0).getNodeValue().trim().isEmpty();
        if (oneNonWhitespaceTextChild) {
            result.append(">").append(element.getChildNodes().item(0).getNodeValue()).append("</").append(element.getTagName()).append(">");
        } else if (noChildren || oneWhitespaceTextChild) {
            result.append("/>");
        } else {
            result.append(">\n");
            for (int i = 0; i < element.getChildNodes().getLength(); i++) {
                final Node child = element.getChildNodes().item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                result.append(toString((Element) child, indent + toStringIndent)).append("\n");
            }
            result.append(indent).append("</").append(element.getTagName()).append(">");
        }
        return result.toString();
    }

    /**
     * @param element an element to stringify
     * @return an XML-like representation of the XML element
     */
    public static String toString(final Element element) {
        return toString(element, "");
    }
}
