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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for building a LayoutManager
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public class LayoutBuilder {
    public static final String CONTENT_PANE_TOKEN = "{contentPane}";

    private static Class[] parseArguments(final Collection<String> packages, final Collection<Object> arguments,
            final Container container, final List<String> constructorArgList) {
        final List<Class<?>> argTypes = new ArrayList<>();
        if (constructorArgList != null) {
            final Map<String, Object> map = new HashMap<>();
            if (container instanceof JFrame) {
                map.put(CONTENT_PANE_TOKEN, ((JFrame) container).getContentPane());
            } else if (container instanceof JDialog) {
                map.put(CONTENT_PANE_TOKEN, ((JDialog) container).getContentPane());
            } else if (container instanceof JWindow) {
                map.put(CONTENT_PANE_TOKEN, ((JWindow) container).getContentPane());
            } else if (container instanceof JApplet) {
                map.put(CONTENT_PANE_TOKEN, ((JApplet) container).getContentPane());
            } else {
                map.put(CONTENT_PANE_TOKEN, container);
            }

            for (final String arg: constructorArgList) {
                final Pair<Class<?>, Object> p = ReflectionUtils.parseToken(container, map, packages, arg);
                argTypes.add(p.getKey());
                arguments.add(p.getValue());
            }
            // for the purpose of finding constructors, the contentPane argument is always a Container, so using something
            // that extends Container like JPanel wouldn't find the constructor
            for (int i = 0; i < constructorArgList.size(); i++) {
                if (constructorArgList.get(i).equals(CONTENT_PANE_TOKEN)) {
                    argTypes.set(i, Container.class);
                }
            }
            return argTypes.toArray(new Class<?>[argTypes.size()]);
        }
        return new Class[]{};
    }

    /**
     * Creates a LayoutManager given the parameters. className can be a fully qualified class name or just the class
     * name itself if it's located in a package in packages.
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
            final List<String> constructorArgList) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
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
