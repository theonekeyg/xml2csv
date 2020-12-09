package org.interview.xml2csv;

import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.SAXException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.interview.xml2csv.XMLConverter;

@Controller
public class UploadController {

    @GetMapping(path="/")
    public String root() {
        return "root.html";
    }

    @PostMapping(path="/upload_xml")
    public ResponseEntity ProcessXML(@RequestParam("file") MultipartFile file,
            @RequestParam(name="outType", required=false, defaultValue="csv")
            String outType) throws IOException, ParserConfigurationException,
                                   TransformerConfigurationException,
                                   TransformerException, SAXException {

        if (!outType.equals("csv")) {
            return new ResponseEntity<>(
                    String.format("output file format `%s` is not implemented", outType),
                    HttpStatus.NOT_IMPLEMENTED
            );
        }

        InputStream xmlInputStream = file.getInputStream();
        XMLConverter converter = new XMLConverter(xmlInputStream);
        try {
            xmlInputStream.close();
        } catch (IOException ex) {
            System.err.println(ex.getStackTrace());
        }
        String outHolder = converter.toCSV();

        return makeFileResponse(outHolder, outType);
    }

    private ResponseEntity makeFileResponse(String body, String fileExt) {
        if (body == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", String.format("attachment; filename=output.%s",
                                                         fileExt));
        headers.add("Content-Type", "text/csv; charset=UTF-8");
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
