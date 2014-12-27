package com.aaron.swingoutxml;

import com.aaron.swingoutxml.util.ReflectionUtils;
import javafx.util.Pair;

import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;
import java.awt.Container;
import java.awt.LayoutManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool for building a LayoutManager
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public class LayoutBuilder {
    public static final String CONTENT_PANE_TOKEN = "contentPane";
    private static final Pattern stringArgPattern = Pattern.compile("(?:'([^']*)'|\"([^\"]*)\")");

    private static void addParsedObject(final Collection<String> packages, final String string,
            final Collection<Object> arguments, final Collection<Class<?>> argTypes) {
        final Pair<Class<?>, Object> pair = ReflectionUtils.parseConstant(packages, string);
        arguments.add(pair.getValue());
        argTypes.add(pair.getKey());
    }

    private static Class[] parseArguments(final Collection<String> packages, final Collection<Object> arguments,
            final Container container, final Iterable<String> constructorArgList) {
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
                            addParsedObject(packages, arg, arguments, argTypes);
                        }
                    }
                }
            }
            return argTypes.toArray(new Class<?>[argTypes.size()]);
        }
        return new Class[]{};
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
    public static LayoutManager buildLayout(final Collection<String> packages, final String className, final Container container,
            final Iterable<String> constructorArgList) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        final Class rawClass = ReflectionUtils.classForName(packages, className);
        if (!LayoutManager.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException(String.format("%s does not extend LayoutManager", className));
        }
        @SuppressWarnings("unchecked") final Class<? extends LayoutManager> layoutClass = rawClass;
        final List<Object> arguments = new ArrayList<>();
        final Class[] parameterTypes = parseArguments(packages, arguments, container, constructorArgList);
        final Constructor layoutConstructor = layoutClass.getDeclaredConstructor(parameterTypes);
        return (LayoutManager) layoutConstructor.newInstance(arguments.toArray());
    }
}
