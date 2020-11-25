package org.interview.xml2csv;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class XMLConverter {

    private HashSet<String> csvHeaders = new HashSet<String>(10);

    private LinkedList<HashMap<String, String>> csvValues
        = new LinkedList<HashMap<String, String>>();

    /*
     * javax.xml's parser matches every closing node as a Node.TEXT_NODE.
     * This workaround is mandatory for skipping these nodes.
     */
    private final Pattern SKIP_PATTERN = Pattern.compile("\n\\s+");

    public XMLConverter(InputStream xmlInputStream) throws SAXParseException, SAXException,
                                                 ParserConfigurationException,
                                                 IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document xmldoc = builder.parse(xmlInputStream);

        final NodeList allNodes = xmldoc.getElementsByTagName("*");
        if (allNodes.getLength() < 3) {
            throw new SAXException("too few xml rows");
        }
        Node firstElem = allNodes.item(1);
        NodeList allElems = xmldoc.getElementsByTagName(firstElem.getNodeName());

        for (int i = 0; i < allElems.getLength(); ++i) {
            csvValues.add(parseElement(allElems.item(i)));
        }
    }

    private HashMap parseElement(Node node) {
        HashMap<String, String> map = new HashMap<String, String>();
        NodeList childs = node.getChildNodes();
        for (int i = 0; i < childs.getLength(); ++i) {
            parseLoop(childs.item(i), map, "");
        }
        return map;
    }

    private void parseLoop(Node node, HashMap<String, String> map, String ascendName) {
        NodeList childs = node.getChildNodes();
        if (childs.getLength() > 0) {
            for (int i = 0; i < childs.getLength(); ++i) {
                String newName = String.format("%s__", ascendName.concat(node.getNodeName()));
                parseLoop(childs.item(i), map, newName);
            }
        } else {
            String nodeVal = node.getNodeValue();
            /* Decline closing nodes */
            if (node.getNodeType() == Node.TEXT_NODE && !isClosingNode(node)
                && ascendName != "") {
                ascendName = ascendName.substring(0, ascendName.length() - 2);
                csvHeaders.add(ascendName);
                map.put(ascendName, nodeVal);
            }
        }
    }

    private Boolean isClosingNode(Node node) {
        Matcher skipMatcher = SKIP_PATTERN.matcher(node.getNodeValue());
        return skipMatcher.matches();
    }

    public String toCSV() {
        StringBuilder csvBuilder = new StringBuilder();
        boolean rowStart = true;

        /* Write headers */
        for (String header : csvHeaders) {
            if (rowStart) {
                csvBuilder.append(header);
                rowStart = false;
            } else {
                csvBuilder.append("," + header);
            }
        }
        csvBuilder.append("\n");
        rowStart = true;

        /* Write the rest */
        for (HashMap<String, String> row : csvValues) {
            for (String header : csvHeaders) {
                String val = "";
                if (row.containsKey(header)) {
                    val = row.get(header).replaceAll(",", "");
                }
                if (rowStart) {
                    csvBuilder.append(val);
                    rowStart = false;
                } else {
                    csvBuilder.append("," + val);
                }
            }
            csvBuilder.append("\n");
            rowStart = true;
        }
        return csvBuilder.toString();
    }
}
