package com.yuriytkach.tracker.fundraiser.model;

import static com.yuriytkach.tracker.fundraiser.service.Patterns.DATE_TIME_FORMATTER;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@RegisterForReflection
public class Donation {

  private final UUID id;
  private final Currency currency;
  private final int amount;
  private final Instant dateTime;
  private final String person;

  public String toStringLong() {
    return String.format(
      "%s - %s %5d - %s (_%s_)",
      formatInstantForPrettyOutput(dateTime),
      currency,
      amount,
      person,
      id
    );
  }

  public String toStringShort() {
    return String.format(
      "%d %s by %s at %s",
      amount,
      currency,
      person,
      formatInstantForPrettyOutput(dateTime)
    );
  }

  String formatInstantForPrettyOutput(final Instant dateTime) {
    return DATE_TIME_FORMATTER.format(dateTime.atOffset(ZoneOffset.ofHours(3)).toLocalDateTime());
  }
}
