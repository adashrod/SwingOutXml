package com.aaron.swingoutxml.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class XmlLoader {
    /**
     * Loads the specified file as an XML document and returns the document. The filename can be an absolute path to a
     * file on the file system, a relative path to a file on the file system, or an absolute path in a JAR file where
     * the "root" of that path is the resources directory in the jar
     * @param filename XML file to load
     * @return the parsed XML doc
     * @throws IOException error loading the file
     * @throws SAXException error parsing the file
     */
    public Document load(final String filename) throws IOException, SAXException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (final ParserConfigurationException pce) {
            pce.printStackTrace();
            return null;
        }
        final InputStream inputStream = getClass().getResourceAsStream(filename);
        if (inputStream != null) {
            // for files found in JARs
            return builder.parse(inputStream);
        } else {
            // for files directly on the file system
            return builder.parse(new File(filename));
        }
    }
}
