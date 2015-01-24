package com.aaron.swingoutxml.util;

import javafx.util.Pair;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tools for doing reflective operations such as parsing strings as code
 */
public class ReflectionUtils {
    private static final Pattern stringArgPattern = Pattern.compile("(?:'([^']*)'|\"([^\"]*)\")");
    private static final Pattern keywordPattern = Pattern.compile("(\\{[^}]*\\})");

    /**
     * Attempts to get a Class by its name. This is just a wrapper for {@link Class#forName(String)}, except that it also
     * attempts to get a class by prefixing className with every entry in potentialPrefixes until one is found.
     * E.g. className = "BoxLayout", potentialPrefixes = ["java.awt", "javax.swing"]; it would try
     *     Class.forName("BoxLayout"), then Class.forName("java.awt.BoxLayout"), then Class.forName("javax.swing.BoxLayout")
     * @param potentialPrefixes a collection of packages that the class might be in
     * @param className a class name to find
     * @return the found class
     * @throws IllegalArgumentException if the class couldn't be found
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
        } catch (final IllegalAccessException ignored) {}
        return new Pair<>(field.getType(), obj);
    }

    /**
     * Parses a string, such as "javax.swing.BoxLayout.X_AXIS" and returns the found constant value and its class
     * @param potentialPrefixes a collection of packages that the class might be in
     * @param constantName a constant name to find
     * @return a pair containing the found constant and its class. It's necessary to include them separately because if
     *         the constant is a primitive, then result.getValue().getClass() would be the primitive wrapper class, not
     *         the primitive class
     * @throws IllegalArgumentException if the class couldn't be found
     */
    public static Pair<Class<?>, Object> parseConstant(final Collection<String> potentialPrefixes, final String constantName) {
        Pair<Class<?>, Object> result = null;
        if (constantName != null) {
            try {
                result = parseConstant("", constantName);
            } catch (final IllegalArgumentException iae) {
                for (final String prefix : potentialPrefixes) {
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
        }
        return result;
    }

    /**
     * Parses a string, such as "top.width" where top is a member of context and width is a member of top. Currently only
     * supports getting fields on context's class, not on any of its superclasses or superinterfaces.
     * @param context    the context object
     * @param fieldToken a dot-delimited string of members
     * @return a pair containing the found field and its class. It's necessary to include them separately because if
     *         the constant is a primitive, then result.getValue().getClass() would be the primitive wrapper class, not
     *         the primitive class
     * @throws IllegalArgumentException if any of the fields couldn't be found
     */
    public static Pair<Class<?>, Object> parseField(final Object context, final String fieldToken) {
        if (fieldToken == null || fieldToken.isEmpty()) {
            throw new IllegalArgumentException(String.format("Can't parse a null/empty field from %s", context));
        }
        final String[] fields = fieldToken.split("\\s*\\.\\s*");
        Object obj = context;
        Field f = null;
        for (final String field: fields) {
            try {
                f = obj.getClass().getDeclaredField(field);
            } catch (final NoSuchFieldException nsfe) {
                throw new IllegalArgumentException(String.format("Can't find field \"%s\" in object %s", field, obj));
            }
            f.setAccessible(true);
            try {
                obj = f.get(obj);
            } catch (final IllegalAccessException ignored) {}
        }
        return new Pair<>(f.getType(), obj);
    }

    /**
     * Parses a token. The token could be an int, a string literal 'str' or "str", a keyword that is present in keywordMap,
     * a constant (see {@link com.aaron.swingoutxml.util.ReflectionUtils#parseConstant(java.util.Collection, String)})
     * or a field (see {@link com.aaron.swingoutxml.util.ReflectionUtils#parseField(Object, String)}
     * @param context           context object for parsing fields
     * @param keywordMap        keyword map for parsing keywords
     * @param potentialPrefixes a collection of packages that the class might be in, when parsing a constant
     * @param token             the string to parse
     * @return a pair containing the parsed object and its class. It's necessary to include them separately because if
     *         the constant is a primitive, then result.getValue().getClass() would be the primitive wrapper class, not
     *         the primitive class
     * @throws IllegalArgumentException if the token couldn't be parsed
     */
    public static Pair<Class<?>, Object> parseToken(final Object context, final Map<String, Object> keywordMap,
            final Collection<String> potentialPrefixes, final String token) {
        final Matcher stringMatcher = stringArgPattern.matcher(token);
        if (stringMatcher.matches()) {
            final String s = stringMatcher.group(1) != null ? stringMatcher.group(1) : stringMatcher.group(2);
            return new Pair<>(String.class, s);
        } else {
            final Matcher keywordMatcher = keywordPattern.matcher(token);
            if (keywordMatcher.matches()) {
                final String keyword = keywordMatcher.group(1);
                if (keywordMap == null) {
                    throw new IllegalArgumentException(String.format("Unable to get keyword \"%s\" from null map", keyword));
                }
                if (!keywordMap.containsKey(keyword)) {
                    throw new IllegalArgumentException(String.format("Keyword \"%s\" was missing in map %s", keyword, keywordMap));
                }
                final Object o = keywordMap.get(keyword);
                return new Pair<>(o.getClass(), o);
            } else {
                try {
                    final int i = Integer.parseInt(token);
                    return new Pair<>(int.class, i);
                } catch (final NumberFormatException nfe) {
                    try {
                        return parseConstant(potentialPrefixes, token);
                    } catch (final IllegalArgumentException iae) {
                        return parseField(context, token);
                    }
                }
            }
        }
    }
}
