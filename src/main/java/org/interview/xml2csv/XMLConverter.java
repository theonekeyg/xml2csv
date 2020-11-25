package org.interview.xml2csv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class XMLConverter {

    private Document xmldoc;
    private Source xmlsource;

    public XMLConverter(InputStream xmlInputStream) throws SAXParseException, SAXException,
                                                    ParserConfigurationException,
                                                    IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        this.xmldoc = builder.parse(xmlInputStream);
        this.xmlsource = new DOMSource(this.xmldoc);
    }

    public String toCSV() throws TransformerConfigurationException, TransformerException {
        File stylesheet = new File("src/main/resources/style.xsl");
        StreamSource stylesource = new StreamSource(stylesheet);

        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(stylesource);
        StringWriter writer = new StringWriter();
        Result outputTarget = new StreamResult(writer);

        transformer.transform(this.xmlsource, outputTarget);
        return writer.toString();
    }
}
