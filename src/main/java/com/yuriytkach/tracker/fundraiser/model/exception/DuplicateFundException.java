package com.yuriytkach.tracker.fundraiser.model.exception;

import com.yuriytkach.tracker.fundraiser.model.Fund;

public class DuplicateFundException extends RuntimeException {

  private DuplicateFundException(final String message) {
    super(message);
  }

  public static DuplicateFundException withFund(final Fund fund) {
    return new DuplicateFundException("Fund with such name already exists: " + fund.getName());
  }
}
