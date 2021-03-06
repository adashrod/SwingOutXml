package com.adashrod.swingoutxml;

import com.adashrod.swingoutxml.annotation.CellRenderer;
import com.adashrod.swingoutxml.annotation.ComponentAction;
import com.adashrod.swingoutxml.annotation.Listener;
import com.adashrod.swingoutxml.annotation.SwingOutContainer;
import com.adashrod.swingoutxml.annotation.UiComponent;
import com.adashrod.swingoutxml.util.DomUtils;
import com.adashrod.swingoutxml.util.NameUtils;
import com.adashrod.swingoutxml.util.ReflectionUtils;
import com.adashrod.swingoutxml.xml.XmlLoader;
import javafx.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.text.JTextComponent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// todo:
// putting processRootNode into the body of treeTraverse would allow pack()ing of frames after they've been populated
// put XML attributes somewhere else
/**
 * SwingOutXml is used to instantiate Swing top-level containers. Instead of instantiating something that extends
 * JFrame/JDialog/etc then using add(), layout is done in an XML template file. A class implements
 * \@{@link com.adashrod.swingoutxml.annotation.SwingOutContainer} and {@link SwingOutXml#create(Class, Object...)} is
 * called with the class being instantiated.
 *
 * @see com.adashrod.swingoutxml.annotation.SwingOutContainer
 * @see com.adashrod.swingoutxml.annotation.UiComponent
 * @see com.adashrod.swingoutxml.annotation.Listener
 * @see com.adashrod.swingoutxml.annotation.ComponentAction
 * @see com.adashrod.swingoutxml.PostSetup
 * @author Aaron Rodriguez (adashrod@gmail.com)
 */
public class SwingOutXml {
    private static final Map<String, Class<? extends Container>> containerClasses = new HashMap<>();
    private static final Map<String, Class<? extends JComponent>> componentClasses = new HashMap<>();
    /**
     * Map containing all of the containers and components created by SwingOutXml
     */
    private static final Map<String, Container> idMap = new HashMap<>();
    /**
     * Map containing all of the {@link javax.swing.ButtonGroup}s being used in the application. Keys are a 2-tuple of
     * the top level container and a name for the ButtonGroup, so ButtonGroup names are namespaced by the SwingOut
     * container.
     */
    private static final Map<Pair<Container, String>, ButtonGroup> buttonGroups = new HashMap<>();

    private static final Collection<String> awtPackages = Arrays.asList("java.awt, javax.swing".split("\\s*,\\s*"));

    private static final Pattern monadicFunctionCallPattern = Pattern.compile("^\\s*([^()]+)\\s*\\(\\s*([^()]+)\\s*\\)\\s*$");

    private static final String A_ID = "id";
    private static final String A_FIELD = "field";
    private static final String A_ENABLED = "enabled";
    private static final String A_CONSTRUCTOR_ARGS = "constructor-args";
    private static final String A_TITLE = "title";
    private static final String A_VISIBLE = "visible";
    private static final String A_LAYOUT = "layout";
    private static final String A_CONSTRAINTS = "constraints";
    private static final String A_LAYOUT_CONSTRUCTOR_ARGS = "layout-constructor-args";
    private static final String A_LISTENERS = "listeners";
    private static final String A_ACTION = "action";
    private static final String A_PREFERRED_SIZE = "preferred-size";
    private static final String A_EDITABLE = "editable";
    private static final String A_ADD = "add";
    private static final String A_BUTTON_GROUP = "button-group";
    private static final String A_CELL_RENDERER = "cell-renderer";
    // todo:
    // default-close-operation
    // selection-mode
    // layout-orientation
    // tool-tip-text
    // modal
    // modality
    // JFrame: graphicsConfiguration (c only)
    // JWindow: owner (c only), graphicsConfiguration (c only)
    // JDialog: owner (c only), , graphicsConfiguration (c only)

    static {
        containerClasses.put("JFrame", JFrame.class);

        componentClasses.put("JButton", JButton.class);
        componentClasses.put("JCheckBox", JCheckBox.class);
        componentClasses.put("JComponent", JComponent.class);
        componentClasses.put("JLabel", JLabel.class);
        componentClasses.put("JList", JList.class);
        componentClasses.put("JPanel", JPanel.class);
        componentClasses.put("JRadioButton", JRadioButton.class);
        componentClasses.put("JScrollPane", JScrollPane.class);
        componentClasses.put("JTextArea", JTextArea.class);
        componentClasses.put("JTextField", JTextField.class);
    }

    /**
     * The context used for finding fields; the object that has the \@SwingOutXml annotation. When create() is used, this
     * is a new instance of the class passed in; in render(), it is the object passed in
     */
    private Object context;
    /**
     * The top level container being rendered, e.g. a JFrame. When create() is used, topLevelContainer == context
     */
    private Container topLevelContainer;
    /**
     * map of annotation type to which map should be queried for that annotation type to find ID associations
     */
    private final Map<Class<? extends Annotation>, Map<String, Collection<Pair<String, Field>>>> mapMap = new HashMap<>();

    private void setContext(final Object context) {
        this.context = context;
        final Map<String, Collection<Pair<String, Field>>> idUiComponentMap = new HashMap<>(),
            idListenerMap = new HashMap<>(),
            idComponentActionMap = new HashMap<>(),
            idCellRendererMap = new HashMap<>();
        for (final Field field: context.getClass().getDeclaredFields()) {
            final UiComponent uiComponent = field.getDeclaredAnnotation(UiComponent.class);
            field.setAccessible(true);
            if (uiComponent != null) {
                for (final String id: uiComponent.value()) {
                    final String trimmedId = id.trim();
                    idUiComponentMap.putIfAbsent(trimmedId, new HashSet<>());
                    idUiComponentMap.get(trimmedId).add(new Pair<>("", field));
                }
            }
            final Listener listener = field.getDeclaredAnnotation(Listener.class);
            if (listener != null) {
                for (final String id: listener.value()) {
                    final String trimmedId = id.trim();
                    idListenerMap.putIfAbsent(trimmedId, new HashSet<>());
                    idListenerMap.get(trimmedId).add(new Pair<>(listener.addFunction(), field));
                }
            }
            final ComponentAction componentAction = field.getDeclaredAnnotation(ComponentAction.class);
            if (componentAction != null) {
                for (final String id: componentAction.value()) {
                    final String trimmedId = id.trim();
                    if (idComponentActionMap.containsKey(trimmedId)) {
                        throw new IllegalArgumentException(
                            String.format("Only one action can be associated to an element using @ComponentAction(id). Multiple ComponentActions contain %s in %s",
                                trimmedId, context.getClass().getName()));
                    }
                    idComponentActionMap.put(trimmedId, Collections.singleton(new Pair<>("", field)));
                }
            }
            final CellRenderer cellRenderer = field.getDeclaredAnnotation(CellRenderer.class);
            if (cellRenderer != null) {
                for (final String id: cellRenderer.value()) {
                    final String trimmedId = id.trim();
                    idCellRendererMap.putIfAbsent(trimmedId, new HashSet<>());
                    idCellRendererMap.get(trimmedId).add(new Pair<>("", field));
                }
            }
        }
        mapMap.put(UiComponent.class, idUiComponentMap);
        mapMap.put(Listener.class, idListenerMap);
        mapMap.put(ComponentAction.class, idComponentActionMap);
        mapMap.put(CellRenderer.class, idCellRendererMap);
    }

    /**
     * Creates an instance of the swingClass and lays out its UI according to the template file specified in the
     * {@link com.adashrod.swingoutxml.annotation.SwingOutContainer} annotation. If swingClass implements
     * {@link PostSetup}, afterCreate is run as the last step.
     * @param swingClass the class to instantiate
     * @param paramConstructorArgs arguments to pass to the construction of swingClass. Arguments can also be passed in
     *                             from XML. Anything in paramConstructorArgs overrides these.
     * @throws IOException
     * @throws SAXException
     * @throws InvocationTargetException
     */
    public static <T extends Container> T create(final Class<T> swingClass, final Object... paramConstructorArgs)
            throws IOException, SAXException, InvocationTargetException, NoSuchMethodException, ParseException {
        final SwingOutContainer swingOutContainer = swingClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer == null) {
            throw new IllegalArgumentException("has to implement SwingOutContainer");
        }
        final String templateFile = swingOutContainer.template();
        final Document xmlDoc = new XmlLoader().load(templateFile);
        final Element rootElement = xmlDoc.getDocumentElement();

        final SwingOutXml swingOutXml = new SwingOutXml();
        final Container topLevelContainer = swingOutXml.processRootNodeForCreate(rootElement, swingClass, paramConstructorArgs);
        swingOutXml.setContext(topLevelContainer);
        swingOutXml.topLevelContainer = topLevelContainer;
        swingOutXml.treeTraverse(rootElement);
        if (topLevelContainer instanceof Window) {
            ((Window) topLevelContainer).pack();
        }
        if (topLevelContainer instanceof PostSetup) {
            ((PostSetup) topLevelContainer).afterCreate();
        }
        return swingClass.cast(topLevelContainer);
    }

    public static Container render(final Object object, final Object... paramConstructorArgs) throws IOException, SAXException,
            InvocationTargetException, NoSuchMethodException, ParseException {
        final SwingOutContainer swingOutContainer = object.getClass().getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer == null) {
            throw new IllegalArgumentException("has to implement SwingOutContainer");
        }
        final String templateFile = swingOutContainer.template();
        final Document xmlDoc = new XmlLoader().load(templateFile);
        final Element rootElement = xmlDoc.getDocumentElement();

        final SwingOutXml swingOutXml = new SwingOutXml();
        swingOutXml.setContext(object);
        swingOutXml.topLevelContainer = swingOutXml.processRootNode(rootElement, paramConstructorArgs);
        swingOutXml.treeTraverse(rootElement);
        ((Window) swingOutXml.topLevelContainer).pack();
        return swingOutXml.topLevelContainer;
    }

    private Container processRootNodeForCreate(final Element rootElement, final Class<? extends Container> swingClass, final Object... paramConstructorArgs) throws InvocationTargetException {
        final List<String> constructorArgString = DomUtils.getAttributeAsList(A_CONSTRUCTOR_ARGS, rootElement);
        final Class<?>[] xmlConstructorClasses = new Class<?>[constructorArgString.size()];
        final Object[] xmlConstructorArgs = new Object[constructorArgString.size()];
        // todo: consolidate this loop with code in createJComponent
        for (int i = 0; i < constructorArgString.size(); i++) {
            final Pair<Class<?>, Object> p;
            try {
                p = ReflectionUtils.parseToken(null, null, idMap, awtPackages, constructorArgString.get(i));
            } catch (final ParseException pe) {
                throw new IllegalArgumentException(String.format("%s in element %s", pe.getMessage(), DomUtils.toString(rootElement)));
            }
            xmlConstructorClasses[i] = p.getKey();
            xmlConstructorArgs[i] = p.getValue();
        }
        final Class<?>[] paramConstructorClasses = new Class<?>[paramConstructorArgs.length];
        Arrays.asList(paramConstructorArgs).stream().map(Object::getClass).collect(Collectors.toList()).toArray(paramConstructorClasses);
        final Constructor<? extends Container> constructor;
        final Container container;
        try {
            if (paramConstructorArgs.length > 0) {
                constructor = ReflectionUtils.getDeclaredConstructorPolymorphic(swingClass, paramConstructorClasses);
                container = constructor.newInstance(paramConstructorArgs);
            } else {
                constructor = ReflectionUtils.getDeclaredConstructorPolymorphic(swingClass, xmlConstructorClasses);
                container = constructor.newInstance(xmlConstructorArgs);
            }
            final String id = DomUtils.getAttribute(A_ID, rootElement);
            if (id != null) {
                if (idMap.containsKey(id)) {
                    throw new IllegalArgumentException(String.format("XML ID \"%s\" duplicated. First usage for %s; duplicate: %s",
                        id, idMap.get(id), DomUtils.toString(rootElement)));
                }
                idMap.put(id, container);
            }
        } catch (final NoSuchMethodException nsme) {
            throw new IllegalArgumentException(String.format("Unable to find a constructor for %s with the signature: %s",
                swingClass, paramConstructorArgs.length > 0 ? Arrays.toString(paramConstructorClasses) :
                    Arrays.toString(xmlConstructorClasses)), nsme);
        } catch (final IllegalAccessException | InstantiationException e) {
            //  shouldn't be a problem, but will probably be refactored out when instantiation is dependent on XML root element
            throw new IllegalArgumentException(e);
        }
        setLayout(rootElement, container);
        setTitle(rootElement, container);
        setPreferredSize(rootElement, container);
        final Boolean visible = DomUtils.getAttribute(A_VISIBLE, rootElement, Boolean.class);
        if (visible != null) {
            container.setVisible(visible);
        }
        return container;
    }

    private Container processRootNode(final Element rootElement, final Object... paramConstructorArgs) throws SAXException,
            IOException, InvocationTargetException, NoSuchMethodException, ParseException {
        // todo: use constructor args
        final Container container = createContainer(rootElement);

        if (container instanceof Window) {
            ((Window) container).pack();
        }
        final Boolean visible = DomUtils.getAttribute(A_VISIBLE, rootElement, Boolean.class);
        if (visible != null) {
            container.setVisible(visible);
        }

        setFields(rootElement, container);
        addListeners(rootElement, container);
        setLayout(rootElement, container);
        setTitle(rootElement, container);
        return container;
    }

    private void treeTraverse(final Element rootElement) throws SAXException, IOException, InvocationTargetException,
            NoSuchMethodException, ParseException {
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
                    final JComponent component = processNode(pairedNode.container, childNode);
                    if (component != null) {
                        // component shouldn't be non-null if childNode isn't an Element
                        queue.addLast(new PairedTreeNode((Element) childNode, component));
                    }
                }
            }
        }
    }

    /**
     * Processes an XML node, turning it into a JComponent if possible, and adding that component to its parent. Returns
     * null if the node isn't an element node
     * @param parentContainer the direct parent container of the new node
     * @param xmlNode the XML node to transform
     * @return the created JComponent, or null if nothing was created
     * @throws SAXException
     * @throws IOException
     * @throws InvocationTargetException
     */
    private JComponent processNode(final Container parentContainer, final Node xmlNode)
            throws SAXException, IOException, InvocationTargetException, NoSuchMethodException, ParseException {
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
            final Pair<Class<?>, Object> constraintsPair;
            try {
                constraintsPair = ReflectionUtils.parseToken(context, null, idMap, awtPackages, constraintsString);
            } catch (final ParseException pe) {
                throw new IllegalArgumentException(String.format("%s in element %s", pe.getMessage(), DomUtils.toString(childElement)));
            }
            constraints = constraintsPair.getValue();
        }
        final Boolean add = DomUtils.getAttribute(A_ADD, childElement, Boolean.class);
        if (add == null || add) {
            parentContainer.add(jComponent, constraints);
        }
        setFields(childElement, jComponent);
        addListeners(childElement, jComponent);
        setAction(childElement, jComponent);
        setButtonGroup(childElement, jComponent);
        setCellRenderer(childElement, jComponent);
        return jComponent;
    }

    @SuppressWarnings("unchecked")
    private Container createContainer(final Element element) throws SAXException, IOException, InvocationTargetException,
            NoSuchMethodException, ParseException {
        final String containerName = element.getTagName();
        final String className = NameUtils.getClassNameForElement(containerName);
        final Class<? extends Container> containerClass, finalContainerClass;
        if (containerClasses.containsKey(className)) {
            containerClass = containerClasses.get(className);
        } else {
            final Class customClass;
            try {
                customClass = Class.forName(className);
            } catch (final ClassNotFoundException cnfe) {
                throw new IllegalArgumentException(String.format("Unable to find class %s from XML: %s", className, DomUtils.toString(element)), cnfe);
            }
            if (!Container.class.isAssignableFrom(customClass)) {
                throw new IllegalArgumentException("custom element doesn't extend JComponent");
            }
            containerClass = customClass;
        }
        final Container container;
        if (containerClass == Container.class) {
            final Set<Field> fields = findAssociatedFields(element);
            if (fields.isEmpty()) {
                throw new IllegalArgumentException(String.format("when using Container in the XML, you must provide a field name of a member that is a concrete class, or annotate a field and include the ID of the element in the XML.: %s", DomUtils.toString(element)));
            }
            final Class concreteContainerClass = fields.iterator().next().getType();
            if (!containerClass.isAssignableFrom(concreteContainerClass)) {
                final String fieldString = DomUtils.getAttribute(A_FIELD, element);
                throw new IllegalArgumentException(String.format("%s.%s doesn't extend Container",
                    concreteContainerClass.getName(), fieldString));
            }
            for (final Field field: fields) {
                if (!field.getType().equals(concreteContainerClass)) {
                    throw new IllegalArgumentException(String.format("when using Container in the XML, all bound fields must be of the same type: %s", DomUtils.toString(element)));
                }
            }
            finalContainerClass = concreteContainerClass;
        } else {
            finalContainerClass = containerClass;
        }
        final SwingOutContainer swingOutContainer = finalContainerClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer != null) {
            // todo: pass constructor-args param to create
            container = SwingOutXml.create(finalContainerClass);
        } else {
            try {
                container = finalContainerClass.newInstance();
            } catch (final IllegalAccessException iae) {
                throw new IllegalArgumentException(String.format("Default constructor for %s is not public", finalContainerClass.getName()), iae);
            } catch (final InstantiationException ie) {
                throw new IllegalArgumentException(String.format("Unable to instantiate %s from XML: %s", finalContainerClass.getName(), DomUtils.toString(element)), ie);
            }
        }
        final String id = DomUtils.getAttribute(A_ID, element);
        if (id != null) {
            if (idMap.containsKey(id)) {
                throw new IllegalArgumentException(String.format("XML ID \"%s\" duplicated. First usage for %s; duplicate: %s",
                    id, idMap.get(id), DomUtils.toString(element)));
            }
            idMap.put(id, container);
        }
        setEnabled(element, container);
        setTitle(element, container);
//        setLayout(element, container);
//        setText(element, container);
        setPreferredSize(element, container);
        setEditable(element, container);
        return container;
    }

    /**
     * Creates a JComponent from an XML element in a template, and sets the text if there is any.
     * in the cases where Class casting is unchecked, it actually has been checked and an IllegalArgumentException is
     * thrown if there's a problem
     * @param xmlElement XML used to describe the new component
     * @return the created component
     * @throws SAXException
     * @throws IOException
     * @throws InvocationTargetException
     * todo: make a similar createContainer for heavyweights?
     */
    @SuppressWarnings("unchecked")
    private JComponent createJComponent(final Element xmlElement) throws SAXException, IOException, InvocationTargetException, ParseException, NoSuchMethodException {
        final String componentName = xmlElement.getLocalName();
        final String className = NameUtils.getClassNameForElement(componentName);
        final Class<? extends JComponent> componentClass, finalComponentClass;
        if (componentClasses.containsKey(className)) {
            componentClass = componentClasses.get(className);
        } else {
            final Class customClass;
            try {
                customClass = Class.forName(className);
            } catch (final ClassNotFoundException cnfe) {
                throw new IllegalArgumentException(String.format("Unable to find class %s from XML: %s", className, DomUtils.toString(xmlElement)), cnfe);
            }
            if (!JComponent.class.isAssignableFrom(customClass)) {
                throw new IllegalArgumentException("custom element doesn't extend JComponent");
            }
            componentClass = customClass;
        }
        final JComponent jComponent;
        if (componentClass == JComponent.class) {
            final Set<Field> fields = findAssociatedFields(xmlElement);
            if (fields.isEmpty()) {
                throw new IllegalArgumentException(String.format("when using JComponent in the XML, you must provide a field name of a member that is a concrete class, or annotate a field and include the ID of the element in the XML.: %s", DomUtils.toString(xmlElement)));
            }
            final Class concreteComponentClass = fields.iterator().next().getType();
            if (!componentClass.isAssignableFrom(concreteComponentClass)) {
                final String fieldString = DomUtils.getAttribute(A_FIELD, xmlElement);
                throw new IllegalArgumentException(String.format("%s.%s doesn't extend JComponent",
                    concreteComponentClass.getName(), fieldString));
            }
            for (final Field field: fields) {
                if (!field.getType().equals(concreteComponentClass)) {
                    throw new IllegalArgumentException(String.format("when using JComponent in the XML, all bound fields must be of the same type: %s", DomUtils.toString(xmlElement)));
                }
            }
            finalComponentClass = concreteComponentClass;
        } else {
            finalComponentClass = componentClass;
        }
        final SwingOutContainer swingOutContainer = finalComponentClass.getDeclaredAnnotation(SwingOutContainer.class);
        if (swingOutContainer != null) {
            // todo: pass constructor-args param to create
            jComponent = SwingOutXml.create(finalComponentClass);
        } else {
            try {
                // todo: refactor code out of processRootNodeForCreate() to get constructor
                final List<String> constructorArgStrings = DomUtils.getAttributeAsList(A_CONSTRUCTOR_ARGS, xmlElement);
                if (constructorArgStrings.isEmpty()) {
                    jComponent = finalComponentClass.newInstance();
                } else {
                    final Class<?>[] argClassesArray = new Class<?>[constructorArgStrings.size()];
                    final Object[] args = new Object[constructorArgStrings.size()];
                    for (int i = 0; i < constructorArgStrings.size(); i++) {
                        final String cArg = constructorArgStrings.get(i);
                        final Pair<Class<?>, Object> pair = ReflectionUtils.parseToken(context, null, idMap, awtPackages, cArg);
                        argClassesArray[i] = pair.getKey();
                        args[i] = pair.getValue();
                    }
                    final Constructor<? extends JComponent> constructor = ReflectionUtils.getDeclaredConstructorPolymorphic(finalComponentClass, argClassesArray);
                    jComponent = constructor.newInstance(args);
                }
            } catch (final IllegalAccessException iae) {
                throw new IllegalArgumentException(String.format("Default constructor for %s is not public", finalComponentClass.getName()), iae);
            } catch (final InstantiationException ie) {
                throw new IllegalArgumentException(String.format("Unable to instantiate %s from XML: %s", finalComponentClass.getName(), DomUtils.toString(xmlElement)), ie);
            }
        }
        final String id = DomUtils.getAttribute(A_ID, xmlElement);
        if (id != null) {
            if (idMap.containsKey(id)) {
                throw new IllegalArgumentException(String.format("XML ID \"%s\" duplicated. First usage for %s; duplicate: %s",
                    id, idMap.get(id), DomUtils.toString(xmlElement)));
            }
            idMap.put(id, jComponent);
        }
        setEnabled(xmlElement, jComponent);
        setTitle(xmlElement, jComponent);
        setLayout(xmlElement, jComponent);
        setText(xmlElement, jComponent);
        setPreferredSize(xmlElement, jComponent);
        setEditable(xmlElement, jComponent);
        return jComponent;
    }

    private static void validateXml(final Element node) {
        // todo
    }

    /**
     * Enables/disables the container according to the attribute on the element
     * @param element the XML element containing text
     * @param container the component to enable/disable
     */
    private void setEnabled(final Element element, final Container container) {
        final Boolean enabled = DomUtils.getAttribute(A_ENABLED, element, Boolean.class);
        if (enabled != null) {
            container.setEnabled(enabled);
        }
    }

    /**
     * Sets the text of the component if applicable. The text comes from the TextNode child node of the element
     * @param element the XML element containing text
     * @param jComponent the component on which to set text
     */
    private void setText(final Node element, final JComponent jComponent) {
        if (element.getChildNodes().getLength() == 1 && element.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
            if (jComponent instanceof AbstractButton) {
                ((AbstractButton) jComponent).setText(element.getChildNodes().item(0).getNodeValue());
            } else if (jComponent instanceof JLabel) {
                ((JLabel) jComponent).setText(element.getChildNodes().item(0).getNodeValue());
            } else if (jComponent instanceof JTextComponent) {
                ((JTextComponent) jComponent).setText(element.getChildNodes().item(0).getNodeValue());
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
            ((JFrame) container).setTitle(title);
        } else if (container instanceof JDialog) {
            ((JDialog) container).setTitle(title);
        } else {
            // todo: add more context
            throw new IllegalArgumentException(String.format("The title attribute is not supported on %s elements", rootElement.getTagName()));
        }
    }

    /**
     * Sets the layout on the container to one described by the attributes on element
     * @param element   the XML element that was used to instantiate the JComponent
     * @param container container to set a layout on
     * @throws InvocationTargetException
     */
    private void setLayout(final Element element, final Container container) throws InvocationTargetException {
        final String layout = DomUtils.getAttribute(A_LAYOUT, element);
        if (layout != null) {
            final List<String> layoutConstructorArgs = DomUtils.getAttributeAsList(A_LAYOUT_CONSTRUCTOR_ARGS, element);
            final LayoutManager layoutManager;
            try {
                layoutManager = LayoutBuilder.buildLayout(awtPackages, idMap, layout, container, layoutConstructorArgs);
            } catch (final NoSuchMethodException nsme) {
                throw new IllegalArgumentException(String.format("Unable to find a constructor for %s with the signature: %s",
                    layout, DomUtils.getAttribute(A_LAYOUT_CONSTRUCTOR_ARGS, element)), nsme);
            } catch (final IllegalAccessException iae) {
                throw new IllegalArgumentException(String.format("Constructor for %s with the signature: %s is not public",
                    layout, DomUtils.getAttribute(A_LAYOUT_CONSTRUCTOR_ARGS, element)), iae);
            } catch (final InstantiationException ie) {
                throw new IllegalArgumentException(String.format("Unable to instantiate layout %s", layout), ie);
            } catch (final ParseException pe) {
                throw new IllegalArgumentException(String.format("%s in element %s", pe.getMessage(), DomUtils.toString(element)));
            }
            container.setLayout(layoutManager);
        }
    }

    /**
     * Sets the preferred size of the container according to the attribute on the element
     * @param element   the XML element that was used to instantiate the JComponent
     * @param container container to set preferred size on
     */
    private void setPreferredSize(final Element element, final Container container) {
        final String preferredSizeString = DomUtils.getAttribute(A_PREFERRED_SIZE, element);
        if (preferredSizeString != null) {
            final String[] dimensions = preferredSizeString.split("\\s*,\\s*");
            if (dimensions.length != 2) {
                throw new IllegalArgumentException(String.format("Error parsing %s attribute in element %s", A_PREFERRED_SIZE, DomUtils.toString(element)));
            }
            final int width = Integer.parseInt(dimensions[0]);
            final int height = Integer.parseInt(dimensions[1]);
            container.setPreferredSize(new Dimension(width, height));
        }
    }

    private void setEditable(final Element element, final Container container) {
        final Boolean editable = DomUtils.getAttribute(A_EDITABLE, element, Boolean.class);
        if (container instanceof JTextComponent && editable != null) {
            ((JTextComponent) container).setEditable(editable);
        }
    }

    /**
     * Finds the field or fields specified by an XML element's attribute or in an annotation. When the annotationType is
     * Listener, the key of the Pair in each set element could be a string representing a function name that is used to
     * add the listener to a component.
     * @param element           the XML element
     * @param annotationType    the type of annotation to look at on found field(s)
     * @param attribute         which attribute to look at (field, listeners, action)
     * @return a Set of String/Field Pairs that match the element
     */
    private Set<Pair<String, Field>> findAssociatedFields(final Element element, final Class<? extends Annotation> annotationType,
            final String attribute) {
        final Set<Pair<String, Field>> result = new HashSet<>();
        final List<String> parts = DomUtils.getAttributeAsList(attribute, element);
        if (!parts.isEmpty()) {
            for (final String part: parts) {
                try {
                    final Matcher matcher = monadicFunctionCallPattern.matcher(part);
                    if (matcher.matches()) {
                        final String fieldName = matcher.group(2);
                        final Field field = context.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        result.add(new Pair<>(matcher.group(1), field));
                    } else {
                        final Field field = context.getClass().getDeclaredField(part);
                        field.setAccessible(true);
                        result.add(new Pair<>("", field));
                    }
                } catch (final NoSuchFieldException nsfe) {
                    throw new IllegalArgumentException(String.format("can't find member \"%s\" in class %s",
                        part, context.getClass().getName()));
                }
            }
        } else {
            final String id = DomUtils.getAttribute(A_ID, element);
            if (id != null) {
                final Collection<Pair<String, Field>> fieldCollection = mapMap.get(annotationType).get(id);
                if (fieldCollection != null) {
                    result.addAll(fieldCollection);
                }
            }
        }
        return result;
    }

    /**
     * Finds the fields in context that should be associated (bound) with the XML element. First tries to find
     * the field by the "field" attribute in element; if there is none, tries to find the field by its ID value of its
     * UiComponent annotation.
     * @see com.adashrod.swingoutxml.annotation.UiComponent documentation for examples
     * @param element XML element corresponding to a component
     * @return the found fields
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private Set<Field> findAssociatedFields(final Element element) {
        return findAssociatedFields(element, UiComponent.class, A_FIELD).stream().map(Pair::getValue).collect(Collectors.toSet());
    }

    /**
     * Finds the fields in context that are listeners to be added to the JComponent corresponding to element.
     * If the listener annotations/XML attributes contain function names to invoke to add the listeners, those are
     * returned in the key part of the Pairs, otherwise the key is ""
     * @param element XML element corresponding to a component
     * @return the found fields
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private Set<Pair<String, Field>> findAssociatedListeners(final Element element) {
        return findAssociatedFields(element, Listener.class, A_LISTENERS);
    }

    /**
     * Finds the field in context that is an action to be set on the JComponent corresponding to element
     * @param element XML element corresponding to a component
     * @return the found field
     * @throws IllegalArgumentException invalid config that didn't match a field
     */
    private Field findAssociatedAction(final Element element) {
        final Set<Pair<String, Field>> fields = findAssociatedFields(element, ComponentAction.class, A_ACTION);
        // todo: error for 2 or more
        return !fields.isEmpty() ? fields.iterator().next().getValue() : null;
    }

    private Field findAssociatedCellRenderer(final Element element) {
        final Set<Pair<String, Field>> fields = findAssociatedFields(element, CellRenderer.class, A_CELL_RENDERER);
        // todo: error for 2 or more
        return !fields.isEmpty() ? fields.iterator().next().getValue() : null;
    }

    /**
     * Binds (a) field(s) in the class being instantiated to a component created from an element in the XML
     * @param element the XML element that was used to instantiate the JComponent
     * @param component the component to set
     */
    private void setFields(final Element element, final Container component) {
        final Set<Field> fields = findAssociatedFields(element);
        if (!fields.isEmpty()) {
            for (final Field field: fields) {
                try {
                    // todo: catch some exception if setting an incompatible type
                    field.set(context, component);
                } catch (final IllegalAccessException ignored) {}
            }
        }
    }

    private void addListeners(final Element element, final Container container) {
        // todo: add all of the container listeners here
    }

    /**
     * Adds all specified listeners to the component
     * @param xmlElement XML element describing the component
     * @param component the component to add listeners to
     * todo: throw different exceptions when ClassCastException happens for better error messages
     */
    private void addListeners(final Element xmlElement, final JComponent component) throws InvocationTargetException {
        final Set<Pair<String, Field>> listenerFields = findAssociatedListeners(xmlElement);
        for (final Pair<String, Field> pair: listenerFields) {
            final String functionName = pair.getKey().trim();
            final Field field = pair.getValue();
            if (!EventListener.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException(String.format("%s in %s is not an EventListener", field, context));
            }
            final EventListener listener;
            try {
                // todo: should probably cast this to Object since custom listeners don't need to implement EventListener
                listener = (EventListener) field.get(context);
            } catch (final IllegalAccessException iae) {
                // impossible
                continue;
            }
            boolean added = false;
            // todo: use getClass or isAssignableFrom instead since instanceof behaves weirdly with some of these Mouse ones
            if (listener instanceof MouseListener) {
                component.addMouseListener((MouseListener) listener);
                added = true;
            }
            if (listener instanceof MouseMotionListener) {
                component.addMouseMotionListener((MouseMotionListener) listener);
                added = true;
            }
            if (listener instanceof MouseWheelListener) {
                component.addMouseWheelListener((MouseWheelListener) listener);
                added = true;
            }
            if (listener instanceof ActionListener) {
                try {
                    ((AbstractButton) component).addActionListener((ActionListener) listener);
                } catch (final ClassCastException cce) {
                    throw new IllegalArgumentException(String.format("%s is not an AbstractButton and therefore cannot accept the ActionListener %s",
                        component, field));
                }
                added = true;
            }
            if (listener instanceof KeyListener) {
                component.addKeyListener((KeyListener) listener);
                added = true;
            }
            if (listener.getClass() == TreeWillExpandListener.class) {
                try {
                    ((JTree) component).addTreeWillExpandListener((TreeWillExpandListener) listener);
                } catch (final ClassCastException cce) {
                    throw new IllegalArgumentException(String.format("%s is not a JTree and therefore cannot accept the TreeWillExpandListener %s",
                        component, field));
                }
                added = true;
            }
            if (listener instanceof TreeExpansionListener) {
                try {
                    ((JTree) component).addTreeExpansionListener((TreeExpansionListener) listener);
                } catch (final ClassCastException cce) {
                    throw new IllegalArgumentException(String.format("%s is not a JTree and therefore cannot accept the TreeExpansionListener %s",
                        component, field));
                }
                added = true;
            }
            if (listener instanceof ListSelectionListener) {
                try {
                    ((JList) component).addListSelectionListener((ListSelectionListener) listener);
                } catch (final ClassCastException cce) {
                    throw new IllegalArgumentException(String.format("%s is not a JList and therefore cannot accept the ListSelectionListener %s",
                        component, field));
                }
                added = true;
            }
            if (!functionName.isEmpty()) {
                Method function = null;
                final Method[] allFunctions = component.getClass().getDeclaredMethods();
                for (final Method m: allFunctions) {
                    if (m.getName().equals(functionName)) {
                        function = m;
                        try {
                            function.invoke(component, listener);
                            added = true;
                        } catch (final IllegalArgumentException iae) {
                            // wrong overloaded function: keep trying
                        } catch (final IllegalAccessException iae) {
                            throw new IllegalArgumentException(String.format("addFunction for listener is not public: %s", functionName), iae);
                        }
                    }
                }
                if (function == null) {
                    throw new IllegalArgumentException(String.format("No function %s in %s", functionName, component.getClass()));
                }
            }
            if (!added) {
                //todo logger
                System.out.println(String.format("Couldn't add listener %s from element %s", listener, DomUtils.toString(xmlElement)));
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
                    button.setAction((Action) field.get(context));
                    // todo: override action name with xml node value (maybe)
                } catch (final IllegalAccessException ignored) {}
            }
        }
    }

    private void setButtonGroup(final Element xmlElement, final JComponent component) {
        final String groupName = DomUtils.getAttribute(A_BUTTON_GROUP, xmlElement);
        if (groupName != null) {
            final Pair<Container, String> key = new Pair<>(topLevelContainer, groupName);
            final ButtonGroup buttonGroup = buttonGroups.getOrDefault(key, new ButtonGroup());
            if (component instanceof AbstractButton) {
                buttonGroup.add((AbstractButton) component);
                buttonGroups.put(key, buttonGroup);
            } else {
                throw new IllegalArgumentException(String.format("%s attr is not allowed for %s: %s", A_BUTTON_GROUP,
                    component.getClass().getSimpleName(), DomUtils.toString(xmlElement)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setCellRenderer(final Element element, final JComponent component) {
        if (component instanceof JList) {
            final Field rendererField = findAssociatedCellRenderer(element);
            if (rendererField != null) {
                try {
                    ((JList) component).setCellRenderer((ListCellRenderer) rendererField.get(context));
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
