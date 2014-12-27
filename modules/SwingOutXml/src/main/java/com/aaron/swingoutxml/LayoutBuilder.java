package com.aaron.swingoutxml;

import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;
import java.awt.Container;
import java.awt.LayoutManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tool for building a LayoutManager
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public class LayoutBuilder {
    public static final String CONTENT_PANE_TOKEN = "contentPane";
    private static final Pattern stringArgPattern = Pattern.compile("(?:'([^']*)'|\"([^\"]*)\")");
    private static final String prefixes = "java.awt, javax.swing";
    private static final Set<String> fqPrefixes = Arrays.asList(prefixes.split("\\s*,\\s*")).stream().collect(Collectors.toSet());

    private static void addParsedObject(final String fqPrefix, final String string, final Collection<Object> arguments,
            final Collection<Class<?>> argTypes) {
        final String wholeString = fqPrefix.isEmpty() ? string : String.format("%s.%s", fqPrefix, string);
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
        arguments.add(obj);
        argTypes.add(field.getType());

    }

    private static void addParsedObject(final String string, final Collection<Object> arguments, final Collection<Class<?>> argTypes) {
        try {
            addParsedObject("", string, arguments, argTypes);
        } catch (final IllegalArgumentException iae) {
            boolean success = false;
            for (final String prefix: fqPrefixes) {
                try {
                    addParsedObject(prefix, string, arguments, argTypes);
                    success = true;
                } catch (final IllegalArgumentException ignored) {}
            }
            if (!success) {
                throw new IllegalArgumentException(String.format("Couldn't dereference %s by itself, or by prefixing it with any of: %s",
                    string, prefixes));
            }
        }
    }

    private static Class[] parseArguments(final Collection<Object> arguments, final Container container,
            final Iterable<String> constructorArgList) {
        final List<Class<?>> argTypes = new ArrayList<>();
        if (constructorArgList != null) {
            for (final String arg: constructorArgList) {
                if (arg.equals(CONTENT_PANE_TOKEN)) {
                    if (container instanceof JFrame) {
                        arguments.add(((JFrame) container).getContentPane());
                    } else if (container instanceof JDialog) {
                        arguments.add(((JDialog) container).getContentPane());
                    } else if (container instanceof JWindow) {
                        arguments.add(((JWindow) container).getContentPane());
                    } else if (container instanceof JApplet) {
                        arguments.add(((JApplet) container).getContentPane());
                    } else {
                        arguments.add(container);
                    }
                    argTypes.add(Container.class);
                } else {
                    final Matcher matcher = stringArgPattern.matcher(arg);
                    if (matcher.matches()) {
                        final String s = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                        arguments.add(s);
                        argTypes.add(String.class);
                    } else {
                        try {
                            arguments.add(Integer.parseInt(arg));
                            argTypes.add(int.class);
                        } catch (final NumberFormatException nfe) {
                            addParsedObject(arg, arguments, argTypes);
                        }
                    }
                }
            }
            return argTypes.toArray(new Class<?>[argTypes.size()]);
        }
        return new Class[]{};
    }

    private static Class getClass(final String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException cnf) {
            for (final String prefix: fqPrefixes) {
                try {
                    return Class.forName(prefix + "." + className);
                } catch (final ClassNotFoundException ignored) {}
            }
            throw new IllegalArgumentException(String.format("Couldn't instantiate %s by itself, or by prefixing it with any of: %s",
                className, prefixes));
        }
    }

    /**
     * Creates a LayoutManager given the parameters. className can be a fully qualified class name or just the class
     * name itself if it's located in the java.awt or javax.swing packages.
     * @param className          the class name of the LayoutManager to instantiate
     * @param container          the container being laid out, needed for some LayoutManager constructors
     * @param constructorArgList a list of arguments to pass to the layout manager's constructor, each of which will be
     *                           parsed first
     * @return a constructed LayoutManager
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static LayoutManager buildLayout(final String className, final Container container,
            final Iterable<String> constructorArgList) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        final Class rawClass = getClass(className);
        if (!LayoutManager.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException(String.format("%s does not extend LayoutManager", className));
        }
        @SuppressWarnings("unchecked") final Class<? extends LayoutManager> layoutClass = rawClass;
        final List<Object> arguments = new ArrayList<>();
        final Class[] parameterTypes = parseArguments(arguments, container, constructorArgList);
        final Constructor layoutConstructor = layoutClass.getDeclaredConstructor(parameterTypes);
        return (LayoutManager) layoutConstructor.newInstance(arguments.toArray());
    }
}
