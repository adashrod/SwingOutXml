package com.aaron.swingoutxml;

import com.aaron.swingoutxml.annotation.ComponentAction;
import com.aaron.swingoutxml.annotation.Listener;
import com.aaron.swingoutxml.annotation.SwingOutContainer;
import com.aaron.swingoutxml.annotation.UiComponent;
import com.aaron.swingoutxml.util.DomUtils;
import com.aaron.swingoutxml.util.NameUtils;
import com.aaron.swingoutxml.util.ReflectionUtils;
import com.aaron.swingoutxml.xml.XmlLoader;
import javafx.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// todo:
// consider changing top-level instantiation logic to be like: found <j-frame/>, use JFrame.class, then it might not be necessary for swingClasses to extend JFrame (could do both as alternatives)
// add some proper exception handling
// put XML attributes somewhere else
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

    private static final Collection<String> awtPackages = Arrays.asList("java.awt, javax.swing".split("\\s*,\\s*")).stream().collect(Collectors.toList());

    private static final String A_ID = "id";
    private static final String A_FIELD = "field";
    private static final String A_TITLE = "title";
    private static final String A_VISIBLE = "visible";
    private static final String A_LAYOUT = "layout";
    private static final String A_CONSTRAINTS = "constraints";
    private static final String A_CONSTRUCTOR_ARGS = "layout-constructor-args";
    private static final String A_LISTENERS = "listeners";
    private static final String A_ACTION = "action";
    // todo: attributes
    // enabled, editable, preferred-size

    static {
        componentClasses.put("JButton", JButton.class);
        componentClasses.put("JComponent", JComponent.class);
        componentClasses.put("JLabel", JLabel.class);
        componentClasses.put("JPanel", JPanel.class);
        componentClasses.put("JTextArea", JTextArea.class);

        leafTypeClasses.add(JButton.class);
        leafTypeClasses.add(JLabel.class);
        leafTypeClasses.add(JTextArea.class);
    }

    private final Container topLevelContainer;
    /**
     * map of annotation type to which map should be queried for that annotation type to find ID associations
     */
    private final Map<Class<? extends Annotation>, Map<String, Collection<Field>>> mapMap = new HashMap<>();

    private SwingOutXml(final Container topLevelContainer) {
        this.topLevelContainer = topLevelContainer;
        final Map<String, Collection<Field>> idUiComponentMap = new HashMap<>(),
            idListenerMap = new HashMap<>(),
            idComponentActionMap = new HashMap<>();
        for (final Field field: topLevelContainer.getClass().getDeclaredFields()) {
            final UiComponent uiComponent = field.getDeclaredAnnotation(UiComponent.class);
            field.setAccessible(true);
            if (uiComponent != null) {
                for (final String id: uiComponent.value()) {
                    final String trimmedId = id.trim();
                    idUiComponentMap.putIfAbsent(trimmedId, new HashSet<>());
                    idUiComponentMap.get(trimmedId).add(field);
                }
            }
            final Listener listener = field.getDeclaredAnnotation(Listener.class);
            if (listener != null) {
                for (final String id: listener.value()) {
                    final String trimmedId = id.trim();
                    idListenerMap.putIfAbsent(trimmedId, new HashSet<>());
                    idListenerMap.get(trimmedId).add(field);
                }
            }
            final ComponentAction componentAction = field.getDeclaredAnnotation(ComponentAction.class);
            if (componentAction != null) {
                for (final String id: componentAction.value()) {
                    final String trimmedId = id.trim();
                    if (idComponentActionMap.containsKey(trimmedId)) {
                        throw new IllegalArgumentException(
                            String.format("Only one action can be associated to an element using @ComponentAction(id). Multiple ComponentActions contain %s in %s",
                                trimmedId, topLevelContainer.getClass().getName()));
                    }
                    idComponentActionMap.put(trimmedId, Collections.singleton(field));
                }
            }
        }
        mapMap.put(UiComponent.class, idUiComponentMap);
        mapMap.put(Listener.class, idListenerMap);
        mapMap.put(ComponentAction.class, idComponentActionMap);
    }

    /**
     * Creates an instance of the swingClass and lays out its UI according to the template file specified in the
     * {@link com.aaron.swingoutxml.annotation.SwingOutContainer} annotation. If swingClass implements
     * {@link PostSetup}, afterCreate is run as the last step.
     * @param swingClass the class to instantiate
     * @throws FileNotFoundException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public static Container create(final Class<? extends Container> swingClass)
            throws IOException, InstantiationException, ClassNotFoundException, SAXException, InvocationTargetException {
// todo: stuff to add to heavyweight component's XML attributes
//        JFrame: graphicsConfiguration (c only)
//        JWindow: owner (c only), graphicsConfiguration (c only)
//        JDialog: owner (c only), modal, modality, graphicsConfiguration (c only)

        final SwingOutContainer swingOutContainer = swingClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer == null) {
            throw new IllegalArgumentException("has to implement SwingOutContainer");
        }
        final String templateFile = swingOutContainer.template();
        final Document xmlDoc = new XmlLoader().load(templateFile);
        final Element rootElement = (Element) xmlDoc.getChildNodes().item(0); // todo: make this account for comments

        // todo put in processNode?
        final Container topLevelContainer;
        try {
            topLevelContainer = swingClass.newInstance();
        } catch (final IllegalAccessException iae) {
            //  shouldn't be a problem, but will probably be refactored out when instantiation is dependent on XML root element
            throw new IllegalArgumentException(iae);
        }

        final SwingOutXml swingOutXml = new SwingOutXml(topLevelContainer);

        swingOutXml.setLayout(rootElement, topLevelContainer);
        swingOutXml.setTitle(rootElement, topLevelContainer);

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
                    final JComponent component = swingOutXml.processNode(pairedNode.container, childNode);
                    if (component != null) {
                        // component shouldn't be non-null if childNode isn't an Element
                        queue.addLast(new PairedTreeNode((Element) childNode, component));
                    }
                }
            }
        }

        if (topLevelContainer instanceof Window) {
            ((Window) topLevelContainer).pack();
            final String visibleString = DomUtils.getAttribute(A_VISIBLE, rootElement);
            if (visibleString != null) {
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
     * Processes an XML node, turning it into a JComponent if possible, and adding that component to its parent. Returns
     * null if the node isn't an element node
     * @param parentContainer the direct parent container of the new node
     * @param xmlNode the XML node to transform
     * @return the created JComponent, or null if nothing was created
     * @throws InstantiationException todo: audit these from createJComponent
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws IOException
     */
    private JComponent processNode(final Container parentContainer, final Node xmlNode)
            throws InstantiationException, ClassNotFoundException, SAXException, IOException, InvocationTargetException {
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
        final JComponent jComponent = createJComponent(childElement);
        final String constraintsString = DomUtils.getAttribute(A_CONSTRAINTS, childElement);
        Object constraints = null;
        if (constraintsString != null) {
            final Pair<Class<?>, Object> constraintsPair = ReflectionUtils.parseToken(topLevelContainer, null, awtPackages, constraintsString);
            constraints = constraintsPair.getValue();
        }
        parentContainer.add(jComponent, constraints);
        setFields(childElement, jComponent);
        addListeners(childElement, jComponent);
        setAction(childElement, jComponent);
        return jComponent;
    }

    /**
     * Creates a JComponent from an XML element in a template, and sets the text if there is any.
     * in the cases where Class casting is unchecked, it actually has been checked and an IllegalArgumentException is
     * thrown if there's a problem
     * @param xmlElement XML used to describe the new component
     * @return the created component
     * @throws InstantiationException todo: audit these
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private JComponent createJComponent(final Element xmlElement)
            throws InstantiationException, ClassNotFoundException, SAXException, IOException, InvocationTargetException {
        final String componentName = xmlElement.getLocalName();
        final String className = NameUtils.getClassNameForElement(componentName);
        final Class<? extends JComponent> componentClass, finalComponentClass;
        if (componentClasses.containsKey(className)) {
            componentClass = componentClasses.get(className);
        } else {
            final Class customClass = Class.forName(className);
            if (!JComponent.class.isAssignableFrom(customClass)) {
                throw new IllegalArgumentException("custom element doesn't extend JComponent");
            }
            componentClass = customClass;
        }
        final JComponent jComponent;
        if (componentClass == JComponent.class) {
            final Set<Field> fields = findAssociatedFields(xmlElement);
            if (fields.isEmpty()) {
                throw new IllegalArgumentException(String.format("when using JComponent in the XML, you must provide a field name of a member that is a concrete class, or annotate a field and include the ID of the element in the XML.: %s", xmlElement));
            }
            final Class concreteComponentClass = fields.iterator().next().getType();
            if (!componentClass.isAssignableFrom(concreteComponentClass)) {
                final String fieldString = DomUtils.getAttribute(A_FIELD, xmlElement);
                throw new IllegalArgumentException(String.format("%s.%s doesn't extend JComponent",
                    concreteComponentClass.getName(), fieldString));
            }
            for (final Field field: fields) {
                if (!field.getType().equals(concreteComponentClass)) {
                    throw new IllegalArgumentException(String.format("when using JComponent in the XML, all bound fields must be of the same type: %s", xmlElement));
                }
            }
            finalComponentClass = concreteComponentClass;
        } else {
            finalComponentClass = componentClass;
        }
        final SwingOutContainer swingOutContainer = finalComponentClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer != null) {
            jComponent = (JComponent) SwingOutXml.create(finalComponentClass);
        } else {
            try {
                jComponent = finalComponentClass.newInstance();
            } catch (final IllegalAccessException iae) {
                throw new IllegalArgumentException(String.format("Default constructor for %s is not public", finalComponentClass.getName()), iae);
            }
        }
        setTitle(xmlElement, jComponent);
        setLayout(xmlElement, jComponent);
        if (leafTypeClasses.contains(finalComponentClass)) { // todo: add support for things that extend JLabel, JButton, etc
            setText(xmlElement, jComponent);
        }
        return jComponent;
    }

    private static void validateXml(final Element node) {
        // todo
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
     * Sets the title of the top-level container if applicable. The title comes from the title attribute of the rootElement.
     * @param rootElement the XML root element of the template
     */
    private void setTitle(final Element rootElement, final Container container) {
        final String title = DomUtils.getAttribute(A_TITLE, rootElement);
        if (title == null) {
            return;
        }
        if (container instanceof JFrame) {
            ((JFrame) topLevelContainer).setTitle(title);
        } else if (container instanceof JDialog) {
            ((JDialog) topLevelContainer).setTitle(title);
        } else {
            // todo: add more context
            throw new IllegalArgumentException(String.format("The title attribute is not supported on %s elements", rootElement.getTagName()));
        }
        // todo: finish this list
    }

    /**
     * Sets the layout on the container to one described by the attributes on element
     * @param element   the XML element that was used to instantiate the JComponent
     * @param container container to set a layout on
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    private void setLayout(final Element element, final Container container)
            throws InvocationTargetException, InstantiationException {
        final String layout = DomUtils.getAttribute(A_LAYOUT, element);
        if (layout != null) {
            final List<String> layoutConstructorArgs = DomUtils.getAttributeAsList(A_CONSTRUCTOR_ARGS, element);
            final LayoutManager layoutManager;
            try {
                layoutManager = LayoutBuilder.buildLayout(awtPackages, layout, container, layoutConstructorArgs);
            } catch (final NoSuchMethodException nsme) {
                throw new IllegalArgumentException(String.format("Unable to find a constructor for %s with the signature: %s",
                    layout, DomUtils.getAttribute(A_CONSTRUCTOR_ARGS, element)), nsme);
            } catch (final IllegalAccessException iae) {
                throw new IllegalArgumentException(String.format("Constructor for %s with the signature: %s is not public",
                    layout, DomUtils.getAttribute(A_CONSTRUCTOR_ARGS, element)), iae);
            }
            container.setLayout(layoutManager);
        }
    }

    /**
     * Finds the field or fields specified by an XML element's attribute.
     * @param element           the XML element
     * @param annotationType    the type of annotation to look at on found field(s)
     * @param attribute         which attribute to look at (field, listeners, action)
     * @return a Set of Fields that match the element, or a singleton set if allowMultiple is true and a match is found
     */
    private Set<Field> findAssociatedFields(final Element element, final Class<? extends Annotation> annotationType,
            final String attribute) {
        final Set<Field> result = new HashSet<>();
        final List<String> fieldNames = DomUtils.getAttributeAsList(attribute, element);
        if (!fieldNames.isEmpty()) {
            for (final String fieldName: fieldNames) {
                try {
                    final Field field = topLevelContainer.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    result.add(field);
                } catch (final NoSuchFieldException nsfe) {
                    throw new IllegalArgumentException(String.format("can't find member \"%s\" in class %s",
                        fieldName, topLevelContainer.getClass().getName()));
                }
            }
        } else {
            final String id = DomUtils.getAttribute(A_ID, element);
            if (id != null) {
                final Collection<Field> fieldCollection = mapMap.get(annotationType).get(id);
                if (fieldCollection != null) {
                    result.addAll(fieldCollection);
                }
            }
        }
        return result;
    }

    /**
     * Finds the fields in topLevelContainer that should be associated (bound) with the XML element. First tries to find
     * the field by the "field" attribute in element; if there is none, tries to find the field by its ID value of its
     * UiComponent annotation.
     * @see com.aaron.swingoutxml.annotation.UiComponent documentation for examples
     * @param element XML element corresponding to a component
     * @return the found fields
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private Set<Field> findAssociatedFields(final Element element) {
        return findAssociatedFields(element, UiComponent.class, A_FIELD);
    }

    /**
     * Finds the fields in topLevelContainer that are listeners to be added to the JComponent corresponding to element
     * @param element XML element corresponding to a component
     * @return the found fields
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private Set<Field> findAssociatedListeners(final Element element) {
        return findAssociatedFields(element, Listener.class, A_LISTENERS);
    }

    /**
     * Finds the field in topLevelContainer that is an action to be set on the JComponent corresponding to element
     * @param element XML element corresponding to a component
     * @return the found field
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private Field findAssociatedAction(final Element element) {
        final Set<Field> fields = findAssociatedFields(element, ComponentAction.class, A_ACTION);
        return !fields.isEmpty() ? fields.iterator().next() : null;
    }

    /**
     * Binds (a) field(s) in the class being instantiated to a component created from an element in the XML
     * @param element the XML element that was used to instantiate the JComponent
     * @param component the component to set
     */
    private void setFields(final Element element, final JComponent component) {
        final Set<Field> fields = findAssociatedFields(element);
        if (!fields.isEmpty()) {
            for (final Field field: fields) {
                try {
                    // todo: catch some exception if setting an incompatible type
                    field.set(topLevelContainer, component);
                } catch (final IllegalAccessException ignored) {}
            }
        }
    }

    /**
     * Adds all specified ActionListeners to the button.
     * @param xmlElement XML element describing the button being modified
     * @param component the component to add ActionListeners to
     */
    private void addListeners(final Element xmlElement, final JComponent component) {
        final Set<Field> listenerFields = findAssociatedListeners(xmlElement);
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
            // todo: use getClass instead since instanceof behaves weirdly with some of these
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

    /**
     * Sets the action on the component to an action specified by the XML element or an annotation
     * @param xmlElement XML element that was used to instantiate the JComponent
     * @param component the button to set an action on
     */
    private void setAction(final Element xmlElement, final JComponent component) {
        if (component instanceof AbstractButton) {
            final AbstractButton button = (AbstractButton) component;
            final Field field = findAssociatedAction(xmlElement);
            if (field != null) {
                try {
                    button.setAction((Action) field.get(topLevelContainer));
                    // todo: override action name with xml node value
                } catch (final IllegalAccessException ignored) {}
            }
        }
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
