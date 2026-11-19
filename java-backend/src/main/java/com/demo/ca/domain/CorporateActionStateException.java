package com.demo.ca.domain;

/**
 * Raised when a lifecycle transition is attempted from an invalid state.
 */
public class CorporateActionStateException extends RuntimeException {

    public CorporateActionStateException(Long corporateActionId,
                                         CorporateAction.Status currentStatus,
                                         String message) {
        super(message + " [id=" + corporateActionId + ", status=" + currentStatus + "]");
    }
}
