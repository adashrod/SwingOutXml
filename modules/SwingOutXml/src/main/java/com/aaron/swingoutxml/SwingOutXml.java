package com.aaron.swingoutxml;

import com.aaron.swingoutxml.annotation.Listener;
import com.aaron.swingoutxml.annotation.ComponentAction;
import com.aaron.swingoutxml.annotation.SwingOutContainer;
import com.aaron.swingoutxml.annotation.UiComponent;
import com.aaron.swingoutxml.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// todo:
// try to fix casting warnings with Class
// maybe make all of this instantiate a SwingOutXml that contains a JFrame/whatever so that topLevelContainer isn't getting passed around everywhere
// consider changing top-level instantiation logic to be like: found <j-frame/>, use JFrame.class, then it might not be necessary for swingClasses to extend JFrame
// add some proper exception handling
/**
 * SwingOutXml is used to instantiate Swing top-level containers. Instead of instantiating something that extends
 * JFrame/JDialog/etc then using add(), layout is done in an XML template file. A class implements
 * \@{@link com.aaron.swingoutxml.annotation.SwingOutContainer} and {@link SwingOutXml#create(Class)} is
 * called with the class being instantiated.
 *
 * @see com.aaron.swingoutxml.annotation.SwingOutContainer
 * @see com.aaron.swingoutxml.annotation.UiComponent
 * @see com.aaron.swingoutxml.PostSetup
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public class SwingOutXml {
    private static final Map<String, Class<? extends JComponent>> componentClasses = new HashMap<>();
    private static final Collection<Class<? extends JComponent>> leafTypeClasses = new HashSet<>();

    private static final String A_ID = "id";
    private static final String A_FIELD = "field";
    private static final String A_TITLE = "title";
    private static final String A_VISIBLE = "visible";
    private static final String A_LAYOUT = "layout";
    private static final String A_LISTENERS = "listeners";
    private static final String A_ACTION = "action";

    static {
        componentClasses.put("JButton", JButton.class);
        componentClasses.put("JLabel", JLabel.class);
        componentClasses.put("JPanel", JPanel.class);
        componentClasses.put("JComponent", JComponent.class);

        leafTypeClasses.add(JButton.class);
        leafTypeClasses.add(JLabel.class);
    }

    private static void validateXml(final Element node) {
        // todo
    }

    /**
     * Binds a field in the class being instantiated to a component created from an element in the XML
     * @param element the XML element that was used to instantiate the JComponent
     * @param topLevelContainer the Container on which to set a field
     * @param jComponent the component to set
     * @throws NoSuchFieldException if the field specified in the XML DNE in the container
     */
    private static void setField(final Element element, final Container topLevelContainer, final JComponent jComponent) throws NoSuchFieldException {
        final Field field = findAssociatedField(element, topLevelContainer);
        if (field != null) {
            try {
                // todo: catch some exception if setting an incompatible type
                field.set(topLevelContainer, jComponent);
            } catch (final IllegalAccessException ignored) {}
        }
    }

    /**
     * Sets the title of the top-level container if applicable. The title comes from the title attribute of the rootElement.
     * @param container top level container
     * @param rootElement the XML root element of the template
     */
    private static void setTitle(final Container container, final Element rootElement) {
        final String title = rootElement.getAttribute(A_TITLE);
        if (title == null) {
            return;
        }
        if (container instanceof JFrame) {
            ((JFrame) container).setTitle(title);
        }
    }

    /**
     * Sets the text of the component if applicable. The text comes from the TextNode child node of the element
     * @param element the XML element containing text
     * @param jComponent the component on which to set text
     */
    private static void setText(final Node element, final JComponent jComponent) {
        if (element.getChildNodes().getLength() == 1 && element.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
            if (jComponent.getClass() == JButton.class) {
                ((JButton) jComponent).setText(element.getChildNodes().item(0).getNodeValue());
            } else if (jComponent.getClass() == JLabel.class) {
                ((JLabel) jComponent).setText(element.getChildNodes().item(0).getNodeValue());
            }
        }
    }

    /**
     *
     * @param element
     * @param topLevelContainer
     * @param annotationType
     * @param fieldNames
     * @param callback
     * @param allowMultiple
     * @return
     */
    private static Set<Field> findAssociatedFields(final Element element, final Container topLevelContainer,
            final Class<? extends Annotation> annotationType, final Collection<String> fieldNames,
            final Function<Annotation, Iterable<String>> callback, final boolean allowMultiple) {
        final Set<Field> fields = new HashSet<>();
        if (!fieldNames.isEmpty()) {
            for (final String fieldName: fieldNames) {
                try {
                    final Field field = topLevelContainer.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    fields.add(field);
                } catch (final NoSuchFieldException nsfe) {
                    throw new IllegalArgumentException(String.format("can't find member \"%s\" in class %s",
                        fieldName.trim(), topLevelContainer.getClass().getName()));
                }
            }
        } else {
            final String id = element.getAttribute(A_ID);
            if (id != null && !id.trim().isEmpty()) {
                final Field[] allFields = topLevelContainer.getClass().getDeclaredFields();
                for (final Field field: allFields) {
                    field.setAccessible(true);
                    final Annotation annotation = field.getDeclaredAnnotation(annotationType);
                    if (annotation == null) {
                        continue;
                    }
                    final Iterable<String> things = callback.apply(annotation);
                    for (final String thing: things) {
                        if (id.trim().equals(thing.trim())) {
                            fields.add(field);
                            break;
                        }
                    }
                    if (!fields.isEmpty() && !allowMultiple) {
                        break;
                    }
                }
            }
        }
        return fields;
    }

    /**
     * Finds the field in topLevelContainer that should be associated with the XML element. First tries to find the field
     * by the "field" attribute in element; if there is none, tries to find the field by its ID value of its UiComponent
     * annotation.
     * @see com.aaron.swingoutxml.annotation.UiComponent documentation for examples
     * @param element the XML element describing a field
     * @param topLevelContainer the Container on which to find a field
     * @return the found field, or null if it couldn't be found
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private static Field findAssociatedField(final Element element, final Container topLevelContainer) {
        final String field = element.getAttribute(A_FIELD);
        final Collection<String> fieldSet = new HashSet<>();
        if (field != null && !field.isEmpty()) {
            fieldSet.add(field);
        }
        final Set<Field> fields = findAssociatedFields(element, topLevelContainer, UiComponent.class,
            fieldSet, (final Annotation annotation) -> {
                return Collections.singleton(((UiComponent) annotation).value());
            }, true);
        return !fields.isEmpty() ? fields.iterator().next() : null;
    }

    private static Set<Field> findAssociatedListeners(final Element element, final Container topLevelContainer) {
        final String listenersString = element.getAttribute(A_LISTENERS);
        final Collection<String> fieldSet = new HashSet<>();
        if (listenersString != null && !listenersString.isEmpty()) {
            Collections.addAll(fieldSet, listenersString.split("\\s*,\\s*"));
        }
        return findAssociatedFields(element, topLevelContainer, Listener.class, fieldSet, (final Annotation annotation) -> {
            final String[] ids = ((Listener) annotation).value();
            final Collection<String> idSet = new HashSet<>();
            Collections.addAll(idSet, ids);
            return idSet;
        }, true);
    }

    private static Field findAssociatedAction(final Element element, final Container topLevelContainer) {
        final String actionString = element.getAttribute(A_ACTION);
        final Collection<String> fieldSet = new HashSet<>();
        if (actionString != null && !actionString.isEmpty()) {
            Collections.addAll(fieldSet, actionString.split("\\s*,\\s*"));
        }
        final Set<Field> fields = findAssociatedFields(element, topLevelContainer, ComponentAction.class, fieldSet, (final Annotation annotation) -> {
            final String[] ids = ((ComponentAction) annotation).value();
            final Collection<String> idSet = new HashSet<>();
            Collections.addAll(idSet, ids);
            return idSet;
        }, false);
        return !fields.isEmpty() ? fields.iterator().next() : null;
    }

    /**
     * Creates a JComponent from an XML element in a template, and sets the text if there is any.
     * @param topLevelContainer the container that contains (not necessarily directly) the component being created
     * @param xmlElement XML used to describe the new component
     * @return the created component
     * @throws InstantiationException todo: audit these
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws SAXException
     * @throws NoSuchFieldException
     * @throws IOException
     */
    private static JComponent createJComponent(final Container topLevelContainer, final Element xmlElement)
            throws InstantiationException, ClassNotFoundException, IllegalAccessException, SAXException, NoSuchFieldException, IOException {
        final String componentName = xmlElement.getLocalName();
        final String className = NameUtils.getClassNameForElement(componentName);
        final Class componentClass, finalComponentClass;
        if (componentClasses.containsKey(className)) {
            componentClass = componentClasses.get(className);
        } else {
            componentClass = Class.forName(className);
            if (!JComponent.class.isAssignableFrom(componentClass)) {
                throw new IllegalArgumentException("custom element doesn't extend JComponent");
            }
        }
        final JComponent jComponent;
        if (componentClass == JComponent.class) {
            final String fieldString = xmlElement.getAttribute(A_FIELD);
            final Field field = findAssociatedField(xmlElement, topLevelContainer);
            if (field == null) {
                throw new IllegalArgumentException("when using JComponent in the XML, you must provide a field name of a member that is a concrete class, or annotate a field and include the ID of an element in the XML.");
            }
            field.setAccessible(true);
            final Class concreteComponentClass = field.getType();
            @SuppressWarnings("unchecked") final boolean assignable = componentClass.isAssignableFrom(concreteComponentClass);
            if (!assignable) {
                throw new IllegalArgumentException(String.format("%s %s doesn't extend JComponent",
                    concreteComponentClass.getName(), fieldString.trim()));
            }
            finalComponentClass = concreteComponentClass;
        } else {
            finalComponentClass = componentClass;
        }
        final SwingOutContainer swingOutContainer = (SwingOutContainer) finalComponentClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer != null) {
            jComponent = (JComponent) SwingOutXml.create(finalComponentClass);
        } else {
            jComponent = (JComponent) finalComponentClass.newInstance();
        }
        if (leafTypeClasses.contains(finalComponentClass)) { // todo: add support for things that extend JLabel, JButton, etc
            setText(xmlElement, jComponent);
        }
        return jComponent;
    }

    /**
     * Adds all specified ActionListeners to the button.
     * @param topLevelContainer the container that contains (not necessarily directly) the button
     * @param component the button to add ActionListeners to
     * @param xmlElement XML element describing the button being modified
     */
    private static void addListeners(final Container topLevelContainer, final JComponent component, final Element xmlElement) {
        final Set<Field> listenerFields = findAssociatedListeners(xmlElement, topLevelContainer);
        for (final Field field: listenerFields) {
            if (!EventListener.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException(String.format("%s in %s is not an EventListener", field, topLevelContainer));
            }
            final EventListener listener;
            try {
                listener = (EventListener) field.get(topLevelContainer);
            } catch (final IllegalAccessException iae) {
                // impossible
                continue;
            }
            if (listener instanceof MouseListener) {
                component.addMouseListener((MouseListener) listener);
            }
            if (listener instanceof MouseMotionListener) {
                component.addMouseMotionListener((MouseMotionListener) listener);
            }
            if (listener instanceof MouseWheelListener) {
                component.addMouseWheelListener((MouseWheelListener) listener);
            }
            if (listener instanceof ActionListener) {
                try {
                    ((AbstractButton) component).addActionListener((ActionListener) listener);
                } catch (final ClassCastException cce) {
                    throw new IllegalArgumentException(String.format("%s is not an AbstractButton and therefore cannot accept the ActionListener %s",
                        component, field));
                }
            }
        }
    }

    private static void setAction(final Container topLevelContainer, final JComponent component, final Element xmlElement) {
        if (component instanceof AbstractButton) {
            final AbstractButton button = (AbstractButton) component;
            final Field field = findAssociatedAction(xmlElement, topLevelContainer);
            if (field != null) {
                try {
                    button.setAction((Action) field.get(topLevelContainer));
                } catch (final IllegalAccessException e) {
                    // impossible
                }
            }
        }
    }

    /**
     * Processes an XML node, turning it into a JComponent if possible, and adding that component to its parent. Returns
     * null if the node isn't an element node
     * @param xmlNode the XML node to transform
     * @param topLevelContainer the GUI container
     * @param parentContainer the direct parent container of the new node
     * @return the created JComponent, or null if nothing was created
     * @throws InstantiationException todo: audit these from createJComponent
     * @throws IllegalAccessException
     * @throws NoSuchFieldException problem finding a field by name in setting up component attributes
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws IOException
     */
    private static JComponent processNode(final Node xmlNode, final Container topLevelContainer, final Container parentContainer)
            throws InstantiationException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException, SAXException, IOException {
        final Element childElement;
        if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
            childElement = (Element) xmlNode;
        } else if (xmlNode.getNodeType() == Node.TEXT_NODE) {
            if (xmlNode.getNodeValue().trim().isEmpty()) {
                return null;
            } else {
                throw new IllegalArgumentException("Can't put text in a top level element");
            }
        } else {
            return null;
        }
        final JComponent jComponent = createJComponent(topLevelContainer, childElement);
        setField(childElement, topLevelContainer, jComponent);
        parentContainer.add(jComponent);
        addListeners(topLevelContainer, jComponent, childElement);
        setAction(topLevelContainer, jComponent, childElement);
        return jComponent;
    }

    /**
     * Creates an instance of the swingClass and lays out its UI according to the template file specified in the
     * {@link com.aaron.swingoutxml.annotation.SwingOutContainer} annotation. If swingClass implements
     * {@link PostSetup}, afterCreate is run as the last step.
     * @param swingClass the class to instantiate
     * @throws FileNotFoundException
     * @throws IllegalAccessException if the swingClass default constructor isn't public todo: make sure all other instances of this exception are caught and ignored
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     */
    public static Container create(final Class<? extends Container> swingClass)
            throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchFieldException, SAXException {
        final SwingOutContainer swingOutContainer = swingClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer == null) {
            throw new IllegalArgumentException("has to implement SwingOutContainer");
        }
        final String templateFile = swingOutContainer.template();
        final Document xmlDoc = XmlLoader.load(new File(templateFile));
        final Element rootElement = (Element) xmlDoc.getChildNodes().item(0);

        // todo put in processNode?
        final Container topLevelContainer = swingClass.newInstance();
        final String layout = rootElement.getAttribute(A_LAYOUT);
        if (layout != null && !layout.trim().isEmpty()) {
            final Class layoutClass = Class.forName(layout.trim());
            topLevelContainer.setLayout((LayoutManager) layoutClass.newInstance());
        }
        setTitle(topLevelContainer, rootElement);

        final Deque<PairedTreeNode> queue = new LinkedList<>();
        queue.addLast(new PairedTreeNode(rootElement, topLevelContainer));
        while (!queue.isEmpty()) {
            final PairedTreeNode pairedNode = queue.removeFirst();
            validateXml(pairedNode.node);
            final NodeList childNodes = pairedNode.node.getChildNodes();
            // only process child nodes if there are element children, not text children
            if (childNodes.getLength() > 1 || (childNodes.getLength() == 1 && childNodes.item(0).getNodeType() != Node.TEXT_NODE)) {
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node childNode = childNodes.item(i);
                    final JComponent component = processNode(childNode, topLevelContainer, pairedNode.container);
                    if (component != null) {
                        // component shouldn't be non-null if childNode isn't an Element
                        queue.addLast(new PairedTreeNode((Element) childNode, component));
                    }
                }
            }
        }

        if (topLevelContainer instanceof Window) {
            ((Window) topLevelContainer).pack();
            final String visibleString = rootElement.getAttribute(A_VISIBLE);
            if (visibleString != null && !visibleString.isEmpty()) {
                final boolean visible = Boolean.parseBoolean(visibleString);
                topLevelContainer.setVisible(visible);
            }
        }
        if (topLevelContainer instanceof PostSetup) {
            ((PostSetup) topLevelContainer).afterCreate();
        }
        return topLevelContainer;
    }

    /**
     * Contains a node from an XML document and a node from a Swing GUI structure.
     * These are used for cloning an XML DOM tree into a Swing GUI tree with the same parent-child relationships.
     */
    private static class PairedTreeNode {
        final Element node;
        final Container container;
        public PairedTreeNode(final Element node, final Container container) {
            this.node = node;
            this.container = container;
        }

        @Override
        public String toString() {
            return String.format("node: <%s/>, container: %s", node.getNodeName(), container.getClass().getName());
        }
    }
}
