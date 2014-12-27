package com.aaron.swingoutxml.util;

import javafx.util.Pair;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Tools for doing reflective operations such as parsing strings as code
 */
public class ReflectionUtils {
    /**
     * Attempts to get a Class by its name. This is just a wrapper for {@link Class#forName(String)}, except that it also
     * attempts to get a class by prefixing className with every entry in potentialPrefixes until one is found.
     * E.g. className = "BoxLayout", potentialPrefixes = ["java.awt", "javax.swing"]; it would try
     *     Class.forName("BoxLayout"), then Class.forName("java.awt.BoxLayout"), then Class.forName("javax.swing.BoxLayout")
     * @param potentialPrefixes a collection of packages that the class might be in
     * @param className a class name to find
     * @return the found class
     * @throws java.lang.IllegalArgumentException if the class couldn't be found
     */
    public static Class<?> classForName(final Collection<String> potentialPrefixes, final String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException cnf) {
            for (final String prefix: potentialPrefixes) {
                try {
                    return Class.forName(prefix + "." + className);
                } catch (final ClassNotFoundException ignored) {}
            }
            final String prefixes = potentialPrefixes.stream().reduce((final String s1, final String s2) -> {
                return String.format("%s, %s", s1, s2);
            }).get();
            throw new IllegalArgumentException(String.format("Couldn't instantiate %s by itself, or by prefixing it with any of: %s",
                className, prefixes));
        }
    }

    private static Pair<Class<?>, Object> parseConstant(final String prefix, final String constantName) {
        final String wholeString = prefix.isEmpty() ? constantName : String.format("%s.%s", prefix, constantName);
        int pos = -1;
        Class<?> c = null;
        while (c == null) {
            pos = wholeString.indexOf('.', pos + 1);
            if (pos == -1) {
                break;
            }
            try {
                c = Class.forName(wholeString.substring(0, pos));
            } catch (final ClassNotFoundException e) {
                // continue;
            }
        }
        if (c == null) {
            throw new IllegalArgumentException(String.format("Couldn't parse \"%s\"", wholeString));
        }
        final String topClass = wholeString.substring(0, pos);
        final int lastDot = wholeString.lastIndexOf('.');
        String className = topClass;
        if (lastDot != pos) {
            final String innerClasses = wholeString.substring(pos + 1, lastDot).replace('.', '$');
            className += "$" + innerClasses;
        }
        try {
            c = Class.forName(className);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Couldn't parse \"%s\"", wholeString));
        }
        final Field field;
        try {
            field = c.getDeclaredField(wholeString.substring(lastDot + 1));
            field.setAccessible(true);
        } catch (final NoSuchFieldException e) {
            throw new IllegalArgumentException(String.format("Couldn't parse \"%s\"", wholeString));
        }
        Object obj = null;
        try {
            obj = field.get(null);
        } catch (final IllegalAccessException e) {
            // impossible
        }
        return new Pair<>(field.getType(), obj);
    }

    /**
     * Parses a string, such as "javax.swing.BoxLayout.X_AXIS" and returns the found constant value and its class
     * @param potentialPrefixes a collection of packages that the class might be in
     * @param constantName a constant name to find
     * @return a pair containing the found constant and its class. It's necessary to include them separately because if
     *         the constant is a primitive, then result.getValue().getClass() would be the primitive wrapper class, not
     *         the primitive class
     * @throws java.lang.IllegalArgumentException if the class couldn't be found
     */
    public static Pair<Class<?>, Object> parseConstant(final Collection<String> potentialPrefixes, final String constantName) {
        Pair<Class<?>, Object> result = null;
        try {
            result = parseConstant("", constantName);
        } catch (final IllegalArgumentException iae) {
            for (final String prefix: potentialPrefixes) {
                try {
                    result = parseConstant(prefix, constantName);
                } catch (final IllegalArgumentException ignored) {}
            }
            if (result == null) {
                final String prefixes = potentialPrefixes.stream().reduce((final String s1, final String s2) -> {
                    return String.format("%s, %s", s1, s2);
                }).get();
                throw new IllegalArgumentException(String.format("Couldn't dereference %s by itself, or by prefixing it with any of: %s",
                    constantName, prefixes));
            }
        }
        return result;
    }
}
