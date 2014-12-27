package com.aaron.swingoutxml.util;

public class NameUtils {
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

    public static String getClassNameForElement(final String name) {
        if (name.indexOf('.') == -1) {
            return dashedToCamel(name);
        } else { // fully-qualified class name, return as-is
            return name;
        }
    }
}
