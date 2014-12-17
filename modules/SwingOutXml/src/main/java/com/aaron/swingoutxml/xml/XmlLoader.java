package com.aaron.swingoutxml.xml;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class XmlLoader {
    public static Document load(final File file) throws IOException, SAXException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (final ParserConfigurationException pce) {
            pce.printStackTrace();
            return null;
        }
        return builder.parse(new InputSource(new FileReader(file)));
    }
}
