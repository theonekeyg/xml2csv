package org.interview.xml2csv;

import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class ControllerExceptionHandler {
    final private Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(SAXException.class)
    public ResponseEntity parseException(SAXException ex, WebRequest request) {
        logger.error(ex.getMessage(), ex);
        return new ResponseEntity<>(ex.toString(), HttpStatus.EXPECTATION_FAILED);
    }
}
