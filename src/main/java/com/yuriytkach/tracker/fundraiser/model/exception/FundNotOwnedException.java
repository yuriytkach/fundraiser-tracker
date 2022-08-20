package com.yuriytkach.tracker.fundraiser.model.exception;

import com.yuriytkach.tracker.fundraiser.model.Fund;

public final class FundNotOwnedException extends RuntimeException {

  private FundNotOwnedException(final String message) {
    super(message);
  }

  public static FundNotOwnedException withFundAndMessage(final Fund fund, final String message) {
    return new FundNotOwnedException(
      String.format("Fund `%s` owned by `%s`: %s", fund.getName(), fund.getOwner(), message)
    );
  }
}
