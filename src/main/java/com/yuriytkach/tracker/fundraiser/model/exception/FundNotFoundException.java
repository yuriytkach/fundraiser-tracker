package com.yuriytkach.tracker.fundraiser.model.exception;

public final class FundNotFoundException extends RuntimeException {

  private FundNotFoundException(final String message) {
    super(message);
  }

  public static FundNotFoundException withFundName(final String fundName) {
    return new FundNotFoundException("Fund not found by name: " + fundName);
  }
}
