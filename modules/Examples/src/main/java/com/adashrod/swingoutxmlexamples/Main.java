package com.adashrod.swingoutxmlexamples;

import com.adashrod.swingoutxml.SwingOutXml;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

public class Main {
    public static void main(final String[] arguments) throws IOException, SAXException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, InterruptedException, ParseException {
//        SwingOutXml.create(HelloWorld.class);
        SwingOutXml.create(CustomComponent.class);
//        SwingOutXml.create(MouseExample.class);
//        SwingOutXml.create(ActionTest.class);
//        SwingOutXml.create(ABunchOfStuff.class);
//        SwingOutXml.create(DialogParent.class);
//        SwingOutXml.create(CellRenderers.class);


//        final InstanceHelloWorld instanceHelloWorld = new InstanceHelloWorld();
//        SwingOutXml.render(instanceHelloWorld);

        /*
        possible APIs for static/non-static stuff

        HelloWorld h = new HelloWorld();
        JFrame f = SwingOutXml.render(h); // <j-frame> is the top element of the XML and can be bound to a field in h
        // HelloWorld doesn't need to extend anything
        // HelloWorld still has class-level annotation for specifying XML template

        HelloWorld h = SwingOutXml.create(HelloWorld.class); // current, top element is irrelevant
        // HelloWorld extends JFrame
         */
    }
}
