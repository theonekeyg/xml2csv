package org.interview.xml2csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.xml.sax.SAXParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public ResponseEntity ProcessXML(@RequestParam("file") MultipartFile file) {
        File xmlfp;
        Path xmlfpPath;
        try {
            /* Receive the file */
            xmlfpPath = Files.createTempFile(file.getName(), ".xml");
            xmlfp = new File(xmlfpPath.toString());
            file.transferTo(xmlfp);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INSUFFICIENT_STORAGE);
        }
        XMLConverter converter = new XMLConverter();
        File csvfp = null;
        try {
            csvfp = converter.toCSV(xmlfp, file.getName());
        } catch (SAXParseException ex) {
            String errmsg = ex.toString();
            logger.error(errmsg, ex);
            if (xmlfp.exists()) xmlfp.delete();
            return new ResponseEntity<>(errmsg, HttpStatus.EXPECTATION_FAILED);
        }
        if (xmlfp.exists()) xmlfp.delete();
        return getFileResponse(csvfp);
    }

    private ResponseEntity<Object> getFileResponse(File file) {
        if (file == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        InputStreamResource istream;
        try {
            istream = new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Disposition",
                    String.format("attachment; filename=\"%s\"", file.getName()));
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        ResponseEntity<Object>
        response = ResponseEntity.ok().headers(headers)
                       .contentLength(file.length())
                       .contentType(MediaType.parseMediaType("application/txt"))
                       .body(istream);
        if (file.exists()) file.delete();
        return response;
    }
}
