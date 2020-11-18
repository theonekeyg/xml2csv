package org.interview.xml2csv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XMLConverter {

    private final Logger logger = LoggerFactory.getLogger(XMLConverter.class);

    private HashSet<String> csvHeaders = new HashSet<String>(10);

    private LinkedList<HashMap<String, String>> csvValues
        = new LinkedList<HashMap<String, String>>();

    /*
     * javax.xml's parser matches closing node as a Node.TEXT_NODE no matter if
     * it contains text or not. This workaround is mandatory for skipping these
     * nodes.
     */
    private final Pattern SKIP_PATTERN = Pattern.compile("\n\\s+");

    private void parseLoop(Node node, HashMap<String, String> map, String head) {
        NodeList childs = node.getChildNodes();
        if (childs.getLength() > 0) {
            for (int i = 0; i < childs.getLength(); ++i) {
                String newHead = head.concat(String.format("%s__",
                                              node.getNodeName()));
                parseLoop(childs.item(i), map, newHead);
            }
        } else {
            Matcher skipMatcher = SKIP_PATTERN.matcher(node.getNodeValue());
            String nodeVal = node.getNodeValue();
            /* Ignore closing nodes */
            if (node.getNodeType() == Node.TEXT_NODE && !skipMatcher.matches()) {
                head = head.substring(0, head.length()-2);
                if (!csvHeaders.contains(head)) {
                    csvHeaders.add(head);
                }
                map.put(head, nodeVal);
            }
        }
    }

    private HashMap parseElement(Node node) {
        HashMap<String, String> map = new HashMap<String, String>();
        parseLoop(node, map, "");
        return map;
    }

    private File dumpToCSV(File csvfp) {
        try {
            Boolean rowStart = true;
            FileWriter writer = new FileWriter(csvfp);
            /* Write headers */
            for (String header : csvHeaders) {
                if (rowStart) {
                    writer.write(header);
                    rowStart = false;
                } else {
                    writer.write("," + header);
                }
            }
            writer.write("\n");
            rowStart = true;
            /* Write the rest */
            for (HashMap row : csvValues) {
                for (String header : csvHeaders) {
                    String val = "";
                    if (row.containsKey(header)) {
                        val = ((String) row.get(header)).replace(',', (char) 0);
                    }
                    if (rowStart) {
                        writer.write(val);
                        rowStart = false;
                    } else {
                        writer.write("," + val);
                    }
                }
                writer.write("\n");
                rowStart = true;
            }
            writer.flush();
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            return null;
        }
        return csvfp;
    }

    public File toCSV(File xmlfp, String baseName) {
        File csvfp = null;
        try {
            csvfp = new File(Files.createTempFile(baseName, ".csv").toString());
            final DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document xmldoc = builder.parse(xmlfp);

            final NodeList allNodes = xmldoc.getElementsByTagName("*");
            if (allNodes.getLength() < 3) {
                csvfp.delete();
                return null;
            }
            Node firstElem = allNodes.item(1);
            NodeList allElems = xmldoc.getElementsByTagName(
                                    firstElem.getNodeName());

            for (int i = 0; i < allElems.getLength(); ++i) {
                csvValues.add(parseElement(allElems.item(i)));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            if (csvfp.exists()) csvfp.delete();
            return null;
        }
        return dumpToCSV(csvfp);
    }
}

