package com.yuriytkach.tracker.fundraiser.model.exception;

import java.lang.RuntimeException;

public class FundTotalMismatchException extends RuntimeException {
    public FundTotalMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
