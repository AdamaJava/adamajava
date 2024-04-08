package org.qcmg.common.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import java.io.IOException;
import java.io.OutputStreamWriter;


public class XmlElementUtils {


    /**
     * @param parent
     * @param tagName: The name of the tag to match on
     * @return a list of Element of first generation child Elements with a given tag name and order
     */
    public static List<Element> getChildElementByTagName(Element parent, String tagName) {
        List<Element> elements = new ArrayList<>();
        if (parent == null) return elements;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }

            if (children.item(i).getNodeName().equals(tagName)) {
                elements.add((Element) children.item(i));
            }
        }

        return elements;
    }

    /**
     * @param parent   : parent element
     * @param tagName: first generation children element name
     * @param itemNo:  the order of first generation children
     * @return element if exists, otherwise return null
     */
    public static Element getChildElement(Element parent, String tagName, int itemNo) {
        List<Element> elements = getChildElementByTagName(parent, tagName);
        return (itemNo >= elements.size()) ? null : elements.get(itemNo);
    }

    /**
     * @param parent
     * @param tagName: The name of the tag to match on
     * @return a list of Element of all generation offspring Elements with a given tag name and order
     */
    public static List<Element> getOffspringElementByTagName(Element parent, String tagName) {

        List<Element> elements = new ArrayList<>();
        if (parent == null) return elements;

        NodeList offspring = parent.getElementsByTagName(tagName);
        for (int i = 0; i < offspring.getLength(); i++) {
            if (!(offspring.item(i) instanceof Element)) {
                continue;
            }
            if (offspring.item(i).getNodeName().equals(tagName)) {
                elements.add((Element) offspring.item(i));
            }
        }

        return elements;
    }

    /**
     * @param parent:  root element node
     * @param filename : output element and offsprings to text file
     */
    public static void asXmlText(Element parent, String filename) {

        try {
            DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0 LS 3.0");
            LSSerializer serializer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setCharacterStream(new OutputStreamWriter(Files.newOutputStream(Paths.get(filename)), StandardCharsets.UTF_8));
            serializer.write(parent.getOwnerDocument(), output);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException |
                 IOException ex) {
            ex.printStackTrace();
        }

    }


    /**
     * create a top level element without appending to any URI, and it's child element if specified
     *
     * @param parentName: name for top level element
     * @param childName:  The child name of the top element to be created or null.
     * @return the top level element
     * @throws ParserConfigurationException
     */
    public static Element createRootElement(String parentName, String childName) throws ParserConfigurationException {
        return createRootElement(parentName, childName, null);
    }

    /**
     * create a top level element without appending to any URI, and it's child element if specified
     *
     * @param parentName:   name for top level element
     * @param childName:    The child name of the top element to be created or null.
     * @param namespaceURI: The namespaceURI to be used for the top element.
     * @return the top level element
     * @throws ParserConfigurationException
     */
    public static Element createRootElement(String parentName, String childName, String namespaceURI) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation domImpl = builder.getDOMImplementation();
        Document doc = domImpl.createDocument(namespaceURI, parentName, null);
        Element root = doc.getDocumentElement();

        if (childName == null) {
            return root;
        }

        return XmlElementUtils.createSubElement(root, childName);
    }

    /**
     * Creates a sub-element with the given name and appends it to the parent element.
     *
     * @param parent The parent element to append the sub-element to.
     * @param name   The name of the sub-element.
     * @return The created sub-element.
     */
    public static Element createSubElement(Element parent, String name) {
        Element element = parent.getOwnerDocument().createElement(name);
        parent.appendChild(element);
        return element;
    }

}
