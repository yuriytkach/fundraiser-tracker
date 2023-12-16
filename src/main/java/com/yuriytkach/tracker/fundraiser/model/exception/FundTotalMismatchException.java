package com.yuriytkach.tracker.fundraiser.model.exception;

import java.lang.RuntimeException;

public class FundTotalMismatchException extends RuntimeException {

    public FundTotalMismatchException(final String message) {
        super(message);
    }

    public FundTotalMismatchException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
