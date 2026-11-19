package com.demo.ca.application;

/**
 * Raised when a corporate action referenced by an operation does not exist.
 * Mapped to HTTP 404 by the web layer.
 */
public class CorporateActionNotFoundException extends RuntimeException {

    public CorporateActionNotFoundException(Long id) {
        super("Corporate action not found: " + id);
    }
}
