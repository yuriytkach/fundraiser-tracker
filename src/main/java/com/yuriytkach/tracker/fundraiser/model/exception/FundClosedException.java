package com.yuriytkach.tracker.fundraiser.model.exception;

import java.time.Duration;
import java.time.Instant;

import com.yuriytkach.tracker.fundraiser.model.Fund;

public final class FundClosedException extends RuntimeException {

  private FundClosedException(final String message) {
    super(message);
  }

  public static FundClosedException withFundAndMessage(final Fund fund, final String message) {
    final var closedAgo = Duration.between(fund.getUpdatedAt(), Instant.now());
    return new FundClosedException(
      String.format("Fund `%s` closed `%d days` ago: %s", fund.getName(), closedAgo.toDays(), message)
    );
  }
}
