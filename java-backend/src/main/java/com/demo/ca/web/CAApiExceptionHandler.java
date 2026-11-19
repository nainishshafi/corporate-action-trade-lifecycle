package com.demo.ca.web;

import com.demo.ca.application.CorporateActionNotFoundException;
import com.demo.ca.domain.CorporateActionStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates application-layer failures into RFC 7807 problem responses so clients receive
 * semantically correct status codes instead of a generic 500.
 */
@RestControllerAdvice
public class CAApiExceptionHandler {

    @ExceptionHandler(CorporateActionNotFoundException.class)
    public ProblemDetail handleNotFound(CorporateActionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CorporateActionStateException.class)
    public ProblemDetail handleInvalidState(CorporateActionStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
