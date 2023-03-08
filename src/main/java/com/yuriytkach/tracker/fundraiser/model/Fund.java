package com.yuriytkach.tracker.fundraiser.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@RegisterForReflection
@Builder(toBuilder = true)
public class Fund {
  private final String id;
  private final boolean enabled;
  private final String name;
  private final String description;
  private final String owner;
  private final String color;
  private final int goal;
  private final int raised;
  private final Currency currency;
  @Nullable
  private final String monobankAccount;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Optional<String> getMonobankAccount() {
    return Optional.ofNullable(monobankAccount);
  }

  public String toOutputStringLong() {
    return String.format(
      Locale.ENGLISH,
      "%s %.2f%% `%s` [%d of %d] %s - %s [%s] - %s%s",
      toFundEnabledString(),
      getRaisedPercent(),
      name,
      raised,
      goal,
      currency,
      description,
      color,
      toFundDurationString(),
      monobankAccount == null ? "" : " - :cat:"
    );
  }

  public String toOutputStringShort() {
    return String.format(
      Locale.ENGLISH,
      "%s `%s` %.2f%% [%d of %d] %s%s",
      toFundEnabledString(),
      name,
      getRaisedPercent(),
      raised,
      goal,
      currency,
      monobankAccount == null ? "" : " Mono"
    );
  }

  double getRaisedPercent() {
    return ((double) raised * 100) / goal;
  }

  String toFundDurationString() {
    final var fundDuration = Duration.between(createdAt, enabled ? Instant.now() : updatedAt);
    final var fromLastUpdate = Duration.between(updatedAt, Instant.now());

    final String fundDurationStr;
    if (enabled) {
      fundDurationStr = String.format(
        Locale.ENGLISH,
        "%s%s",
        fundDuration.toDaysPart() > 0 ? fundDuration.toDaysPart() + " d, " : "",
        fundDuration.toHoursPart() + " h"
      );
    } else {
      fundDurationStr = fundDuration.toDays() + " d";
    }

    return String.format(
      Locale.ENGLISH,
      "%s%s",
      fundDurationStr,
      enabled ? "" : " (Closed " + fromLastUpdate.toDays() + " d ago)"
    );
  }

  private String toFundEnabledString() {
    return enabled ? ":open_book:" : ":closed_book:";
  }
}
