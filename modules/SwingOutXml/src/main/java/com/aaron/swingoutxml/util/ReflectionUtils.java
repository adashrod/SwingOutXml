package com.aaron.swingoutxml.util;

import javafx.util.Pair;

import java.awt.Container;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tools for doing reflective operations such as parsing strings as code
 */
public class ReflectionUtils {
    private static final Pattern stringArgPattern = Pattern.compile("(?:'([^']*)'|\"([^\"]*)\")");
    private static final Pattern keywordPattern = Pattern.compile("(\\{[^}:]*\\})");
    private static final Pattern idPattern = Pattern.compile("^\\s*\\{id:([^}:]*)\\}\\s*$");

    private static final Map<Class<?>, Class<?>> primitiveMap = new HashMap<>();
    private static final Map<Class<?>, Class<?>> primitiveInverseMap = new HashMap<>();

    static {
        primitiveMap.put(Boolean.class, boolean.class);
        primitiveMap.put(Character.class, char.class);
        primitiveMap.put(Byte.class, byte.class);
        primitiveMap.put(Short.class, short.class);
        primitiveMap.put(Integer.class, int.class);
        primitiveMap.put(Long.class, long.class);
        primitiveMap.put(Float.class, float.class);
        primitiveMap.put(Double.class, double.class);

        primitiveInverseMap.put(boolean.class, Boolean.class);
        primitiveInverseMap.put(char.class, Character.class);
        primitiveInverseMap.put(byte.class, Byte.class);
        primitiveInverseMap.put(short.class, Short.class);
        primitiveInverseMap.put(int.class, Integer.class);
        primitiveInverseMap.put(long.class, Long.class);
        primitiveInverseMap.put(float.class, Float.class);
        primitiveInverseMap.put(double.class, Double.class);
    }

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

    /**
     * Gets a field on a class, but unlike {@link java.lang.Class#getDeclaredField(String)}, this searches up the
     * inheritance chain if the field isn't found on the base class. This searches up the chain until it's found or until
     * it gets to {@link java.lang.Object} and fails. It also calls setAccessible(true) on the field.
     * @param c    the class to find a field on
     * @param name the name of the field to find
     * @return the field, or null if not found
     */
    public static Field getDeclaredFieldHierarchical(final Class c, final String name) {
        Class step = c;
        while (step != null) {
            try {
                final Field field = step.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (final NoSuchFieldException nsfe) {
                step = step.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Tries to find a constructor on the class with a signature matching classes. While
     * {@link java.lang.Class#getDeclaredConstructor(Class[])} will only return a constructor if the signature exactly
     * matches the declared classes, this will attempt to find a constructor that would be invocable with the argument
     * types in classes, even if one or more of the types of classes is a sub-type of the corresponding parameter in the
     * constructor signature.
     * E.g. a {@link javax.swing.BoxLayout} can be instantiated with a {@link javax.swing.JPanel}: new BoxLayout(jPanel, BoxLayout.X_AXIS),
     * but calling BoxLayout.class.getDeclaredConstructor(JPanel.class, int.class) will throw a NoSuchMethodException.
     * This is because the constructor signature is BoxLayout(Container target, int axis). Even though JPanel descends
     * from Container, Class#getDeclaredConstructor doesn't check superclasses, so the only way to get the constructor
     * would be to call BoxLayout.class.getDeclaredConstructor(Container.class, int.class).
     * This function will find constructors if one or more of the supplied argument types is a subclass of the formal
     * parameter type.
     * @param c       the class to find a constructor on
     * @param classes the name of the field to find
     * @return the found constructor
     * @throws NoSuchMethodException if the constructor couldn't be found, i.e. if there is no constructor that can be
     * invoked with arguments of the supplied types, even accounting for superclasses
     */
    public static <T> Constructor<T> getDeclaredConstructorPolymorphic(final Class<T> c, final Class<?>... classes)
            throws NoSuchMethodException {
        final Class[] copy = Arrays.copyOf(classes, classes.length);
        final Iterator<Class<?>[]> iterator = new Iterator<Class<?>[]>() {
            private boolean firstIteration = true;
            /**
             * Returns the superclass of klass with special behavior for primitives and primitive wrappers. In the case
             * where a primitive is auto-boxed to its wrapper class, this iterator would for example go Integer ->
             * Number -> Object -> null, when it should have gone int -> null. This function changes that path to be
             * Integer -> int -> Number -> Object -> null so that construction signatures with primitive formal types
             * will not be missed.
             * @param klass a class to get a superclass of
             * @return the superclass, corresponding primitive, or corresponding wrapper's superclass
             */
            private Class<?> getNextClass(final Class<?> klass) {
                if (primitiveMap.keySet().contains(klass)) {
                    return primitiveMap.get(klass);
                } else if (primitiveMap.values().contains(klass)) {
                    return primitiveInverseMap.get(klass).getSuperclass();
                } else {
                    return klass.getSuperclass();
                }
            }
            public boolean hasNext() {
                if (firstIteration) {
                    firstIteration = false;
                    return true;
                }
                /* incrementing (changing from class to superclass) each index in the array like incrementing each digit
                   in a number. copy[0] is least significant; copy[n - 1] is most
                 */
                if (copy.length == 0) {
                    return false;
                }
                copy[0] = getNextClass(copy[0]);
                for (int i = 0; i < copy.length; i++) {
                    if (copy[i] == null) {
                        copy[i] = classes[i];
                        if (i + 1 < copy.length) {
                            copy[i + 1] = getNextClass(copy[i + 1]);
                        } else {
                            // the most significant class has cycled - we've iterated over all permutations
                            return false;
                        }
                    }
                }
                return true;
            }
            public Class<?>[] next() { return copy; }
        };
        while (iterator.hasNext()) {
            try {
                return c.getDeclaredConstructor(iterator.next());
            } catch (final NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(String.format("Unable to find constructor for %s with parameter list %s",
            c.getName(), Arrays.toString(classes)));
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
        final Field field = getDeclaredFieldHierarchical(c, wholeString.substring(lastDot + 1));
        if (field == null) {
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
    private static Pair<Class<?>, Object> parseConstant(final Collection<String> potentialPrefixes, final String constantName) {
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
     * Parses a string, such as "top.width" or "this.top.width" where this is the context object, top is a member of
     * context and width is a member of top. Supports getting members of any access level from the context object or any
     * of its superclasses by climbing up the inheritance chain.
     * @param context    the context object
     * @param fieldToken a dot-delimited string of members
     * @return a pair containing the found field and its class. It's necessary to include them separately because if
     *         the constant is a primitive, then result.getValue().getClass() would be the primitive wrapper class, not
     *         the primitive class
     * @throws IllegalArgumentException if any of the fields couldn't be found
     * @throws ParseException if any of the fields couldn't be found
     */
    private static Pair<Class<?>, Object> parseField(final Object context, final String fieldToken) throws ParseException {
        if (fieldToken == null || fieldToken.isEmpty()) {
            throw new ParseException(String.format("Can't parse a null/empty field from %s", context), 0);
        }
        if ("this".equals(fieldToken)) {
            return new Pair<>(context.getClass(), context);
        }
        final String[] fields = fieldToken.split("\\s*\\.\\s*");
        Object obj = context;
        Field f = null;
        for (int i = 0; i < fields.length; i++) {
            final String field = fields[i];
            if ("this".equals(field) && i == 0) {
                continue;
            }
            f = getDeclaredFieldHierarchical(obj.getClass(), field);
            if (f == null) {
                throw new ParseException(String.format("Can't find field \"%s\" in object %s", field, obj), 0);
            }
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
            final Map<String, Container> idMap, final Collection<String> potentialPrefixes, final String token) throws ParseException {
        final Matcher stringMatcher = stringArgPattern.matcher(token);
        if (stringMatcher.matches()) {
            final String s = stringMatcher.group(1) != null ? stringMatcher.group(1) : stringMatcher.group(2);
            return new Pair<>(String.class, s);
        } else {
            final Matcher keywordMatcher = keywordPattern.matcher(token);
            if (keywordMatcher.matches()) {
                final String keyword = keywordMatcher.group(1);
                if (keywordMap == null) {
                    throw new ParseException(String.format("Unable to get keyword \"%s\" from null map", keyword), 0);
                }
                if (!keywordMap.containsKey(keyword)) {
                    throw new ParseException(String.format("Couldn't find keyword \"%s\" in map %s", keyword, keywordMap), 0);
                }
                final Object o = keywordMap.get(keyword);
                return new Pair<>(o.getClass(), o);
            } else {
                final Matcher idMatcher = idPattern.matcher(token);
                if (idMatcher.matches()) {
                    final String id = idMatcher.group(1);
                    final Container c = idMap.get(id);
                    if (c == null) {
                        throw new ParseException(String.format("Couldn't find object with ID: %s", id), 0);
                    }
                    return new Pair<>(c.getClass(), c);
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
}
