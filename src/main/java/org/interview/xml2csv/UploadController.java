package org.interview.xml2csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

  @RequestMapping(path="/", method=RequestMethod.GET)
  public String root() {
    return "root";
  }

  @RequestMapping(path="/upload_xml", method=RequestMethod.POST, consumes=MediaType.ALL_VALUE)
  public ResponseEntity ProcessXML(@RequestParam("file") MultipartFile file) {
    File xmlfp;
    Path xmlfp_path;
    try {
      // Receive the file
      System.out.println(String.format("\033[33m%s\033[m", new String(file.getBytes())));
      xmlfp_path = Files.createTempFile(file.getName(), ".xml");
      xmlfp = new File(xmlfp_path.toString());
      file.transferTo(xmlfp);
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
      return new ResponseEntity<>(HttpStatus.INSUFFICIENT_STORAGE);
    }
    XMLConverter converter = new XMLConverter();
    File csvfp = converter.toCSV(xmlfp, file.getName());
    if (xmlfp.exists()) xmlfp.delete();
    return getFileResponse(csvfp);
  }

  private ResponseEntity<Object> getFileResponse(File fp) {
    if (fp == null) {
      return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }
    InputStreamResource istream;
    try {
      istream = new InputStreamResource(new FileInputStream(fp));
    } catch (FileNotFoundException ex) {
      System.out.print(ex);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    HttpHeaders headers = new HttpHeaders();

    headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", fp.getName()));
    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
    headers.add("Pragma", "no-cache");
    headers.add("Expires", "0");

    ResponseEntity<Object>
    response = ResponseEntity.ok().headers(headers).contentLength(fp.length()).contentType(
        MediaType.parseMediaType("application/txt")).body(istream);
    if (fp.exists()) fp.delete();
    return response;
  }
}
