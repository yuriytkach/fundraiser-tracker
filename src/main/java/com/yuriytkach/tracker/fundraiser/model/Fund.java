package com.yuriytkach.tracker.fundraiser.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@RegisterForReflection
@Builder(toBuilder = true)
public class Fund {
  private final String id;
  private final String name;
  private final String description;
  private final String owner;
  private final String color;
  private final int goal;
  private final int raised;
  private final Currency currency;
  private final Instant createdAt;
  private final Instant updatedAt;

  public String toOutputStringLong() {
    return String.format(
      Locale.ENGLISH,
      "%.2f%% `%s` [%d of %d] %s - %s [%s] - :open_book: %s",
      getRaisedPercent(),
      name,
      raised,
      goal,
      currency,
      description,
      color,
      toFundDurationString()
    );
  }

  public String toOutputStringShort() {
    return String.format(
      Locale.ENGLISH,
      "`%s` %.2f%% [%d of %d] %s",
      name,
      getRaisedPercent(),
      raised,
      goal,
      currency
    );
  }

  double getRaisedPercent() {
    return ((double) raised * 100) / goal;
  }

  String toFundDurationString() {
    final var fundDuration = Duration.between(createdAt, Instant.now());
    return String.format(
      Locale.ENGLISH,
      "%s%s",
      fundDuration.toDaysPart() > 0 ? fundDuration.toDaysPart() + " day(s), " : "",
      fundDuration.toHoursPart() + " hour(s)"
    );
  }
}
