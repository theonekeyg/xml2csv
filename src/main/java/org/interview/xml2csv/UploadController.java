package org.interview.xml2csv;

import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.interview.xml2csv.XMLConverter;

@Controller
public class UploadController {
    private final Logger logger = LoggerFactory.getLogger(UploadController.class);

    @RequestMapping(path="/", method=RequestMethod.GET)
    public String root() {
        return "root.html";
    }

    @RequestMapping(path="/upload_xml", method=RequestMethod.POST)
    public ResponseEntity ProcessXML(@RequestParam("file") MultipartFile file,
            @RequestParam(name="outType", required=false, defaultValue="csv")
            String outType) {
        InputStream xmlInputStream;
        /* Receive the file */
        try {
            xmlInputStream = file.getInputStream();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        /* Parse the .xml file */
        XMLConverter converter;
        try {
            converter = new XMLConverter(xmlInputStream);
        } catch (SAXException ex) {
            String errmsg = ex.toString();
            logger.info("Bad file format received: " + errmsg);
            return new ResponseEntity<>(errmsg, HttpStatus.EXPECTATION_FAILED);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /* Convert file into output format */
        String outHolder = null;
        if (outType.equals("csv")) {
            outHolder = converter.toCSV();
        } else {
            return new ResponseEntity<>(
                    String.format("output file format `%s` is not implemented", outType),
                    HttpStatus.NOT_IMPLEMENTED
            );
        }
        return makeFileResponse(outHolder, outType);
    }

    private ResponseEntity makeFileResponse(String body, String fileExt) {
        if (body == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", String.format("attachment; filename=output.%s",
                                                         fileExt));
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
