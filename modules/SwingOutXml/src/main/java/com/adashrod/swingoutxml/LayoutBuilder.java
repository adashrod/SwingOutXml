package com.adashrod.swingoutxml;

import com.adashrod.swingoutxml.util.ReflectionUtils;
import javafx.util.Pair;

import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;
import java.awt.Container;
import java.awt.LayoutManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
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

    private static Class[] parseArguments(final Collection<String> packages, final Map<String, Container> idMap,
            final Collection<Object> arguments, final Container container, final List<String> constructorArgList) throws ParseException {
        final List<Class<?>> argTypes = new ArrayList<>();
        if (constructorArgList != null) {
            final Map<String, Object> keywordMap = new HashMap<>();
            if (container instanceof JFrame) {
                keywordMap.put(CONTENT_PANE_TOKEN, ((JFrame) container).getContentPane());
            } else if (container instanceof JDialog) {
                keywordMap.put(CONTENT_PANE_TOKEN, ((JDialog) container).getContentPane());
            } else if (container instanceof JWindow) {
                keywordMap.put(CONTENT_PANE_TOKEN, ((JWindow) container).getContentPane());
            } else if (container instanceof JApplet) {
                keywordMap.put(CONTENT_PANE_TOKEN, ((JApplet) container).getContentPane());
            } else {
                keywordMap.put(CONTENT_PANE_TOKEN, container);
            }

            for (final String arg: constructorArgList) {
                final Pair<Class<?>, Object> p = ReflectionUtils.parseToken(container, keywordMap, idMap, packages, arg);
                argTypes.add(p.getKey());
                arguments.add(p.getValue());
            }
            // for the purpose of finding constructors, the contentPane argument is always a Container. This would work
            // with the concrete class, such as JPanel.class as opposed to Container.class since ReflectionUtils.getDeclaredConstructorPolymorphic()
            // checks superclasses, but doing this will save that function a bit of time.
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
     * @param layoutName         the class name of the LayoutManager to instantiate
     * @param context            the container being laid out, needed for some LayoutManager constructors, also needed
     *                           as context for parsing
     * @param constructorArgList a list of arguments to pass to the layout manager's constructor, each of which will be
     *                           parsed first
     * @return a constructed LayoutManager
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static LayoutManager buildLayout(final Collection<String> packages, final Map<String, Container> idMap,
            final String layoutName, final Container context, final List<String> constructorArgList)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ParseException {
        final Class rawClass = ReflectionUtils.classForName(packages, layoutName);
        if (!LayoutManager.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException(String.format("%s does not extend LayoutManager", layoutName));
        }
        @SuppressWarnings("unchecked") final Class<? extends LayoutManager> layoutClass = rawClass;
        final List<Object> arguments = new ArrayList<>();
        final Class[] parameterTypes = parseArguments(packages, idMap, arguments, context, constructorArgList);
        final Constructor layoutConstructor = ReflectionUtils.getDeclaredConstructorPolymorphic(layoutClass, parameterTypes);
        return (LayoutManager) layoutConstructor.newInstance(arguments.toArray());
    }
}
