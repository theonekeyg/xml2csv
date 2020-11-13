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

class XMLConverter {
  private HashSet<String> csv_headers = new HashSet<String>(10);
  private LinkedList<HashMap<String, String>> csv_values = new LinkedList<HashMap<String, String>>();
  // javax.xml's parser matches closing node as a Node.TEXT_NODE no matter if it contains
  // text or not. This workaround is mandatory for skipping these nodes.
  private final Pattern skip_pattern = Pattern.compile("\n\\s+");

  private void _parseLoop(Node node, HashMap<String, String> map, String head) {
    NodeList childs = node.getChildNodes();
    if (childs.getLength() > 0) {
      for (int i = 0; i < childs.getLength(); ++i) {
        String new_head = head.concat(String.format("%s__", node.getNodeName()));
        _parseLoop(childs.item(i), map, new_head);
      }
    } else {
      Matcher skip_matcher = skip_pattern.matcher(node.getNodeValue());
      String node_val = node.getNodeValue();
      // Decline closing nodes and nodes containing commas
      if (node.getNodeType() == Node.TEXT_NODE && !skip_matcher.matches() && !node_val.contains(",")) {
        head = head.substring(0, head.length()-2);
        if (!csv_headers.contains(head)) {
          csv_headers.add(head);
        }
        map.put(head, node_val);
      }
    }
  }

  private HashMap parseElement(Node node) {
    HashMap<String, String> map = new HashMap<String, String>();
    _parseLoop(node, map, "");
    return map;
  }

  private File dumpToCSV(File csvfp) {
    try {
      FileWriter writer = new FileWriter(csvfp);
      // Write headers
      for (String header : csv_headers) {
        writer.write(header + ",");
      }
      writer.write("\n");
      // Write the rest
      for (HashMap row : csv_values) {
        for (String header : csv_headers) {
          String val = "";
          if (row.containsKey(header)) {
            val = (String)row.get(header);
          }
          writer.write(val + ",");
        }
        writer.write("\n");
      }
      writer.flush();
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
      return null;
    }
    return csvfp;
  }

  public File toCSV(File xmlfp, String base_name) {
    File csvfp = null;
    try {
      csvfp = new File(Files.createTempFile(base_name, ".csv").toString());
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder builder = factory.newDocumentBuilder();
      final Document xmldoc = builder.parse(xmlfp);

      final NodeList allNodes = xmldoc.getElementsByTagName("*");
      if (allNodes.getLength() < 3) {
        csvfp.delete();
        return null;
      }
      Node first_elem = allNodes.item(1);
      NodeList all_elems = xmldoc.getElementsByTagName(first_elem.getNodeName());

      for (int i = 0; i < all_elems.getLength(); ++i) {
        csv_values.add(parseElement(all_elems.item(i)));
      }
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      if (csvfp.exists()) csvfp.delete();
      return null;
    }
    return dumpToCSV(csvfp);
  }
}

