package com.adashrod.swingoutxml.util;

/**
 * Utilities for name-related tasks, such as parsing or finding class names.
 */
public class NameUtils {
    /**
     * Converts a dashed name to a camel-case one, e.g. "j-text-field" to "JTextField". The first letter will always be
     * capitalized. If the dashedName has no dashes, then it is returned as-is, but with the first letter capitalized.
     * @param dashedName dashed name to convert
     * @return a camel-case name
     */
    public static String dashedToCamel(final String dashedName) {
        if (dashedName.isEmpty()) {
            return "";
        }
        final StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(dashedName.charAt(0)));
        for (int i = 1; i < dashedName.length(); i++) {
            final char c = dashedName.charAt(i);
            if (c == '-') {
                if (i + 1 < dashedName.length()) {
                    result.append(Character.toUpperCase(dashedName.charAt(i + 1)));
                    i++;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Returns the presumed class name for the given name. If the name has dots in it, it is presumed to be a fully-
     * qualified class name. Otherwise it is presumed to be a reserved name in either dashed or camel case, such as
     * "j-text-field" or "JTextField"
     * @param name name to parse
     * @return a class name for the given name
     */
    public static String getClassNameForElement(final String name) {
        if (name.indexOf('.') == -1) {
            return dashedToCamel(name);
        } else { // fully-qualified class name, return as-is
            return name;
        }
    }
}
