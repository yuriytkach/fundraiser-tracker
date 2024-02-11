package com.yuriytkach.tracker.fundraiser.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@RegisterForReflection
@Builder(toBuilder = true)
public class Fund {

  public static final String ENABLED_N_KEY = "1";
  public static final String DISABLED_N_KEY = "0";

  private final String id;
  private final boolean enabled;
  private final String name;
  private final String description;
  private final String owner;
  private final String color;
  private final int goal;
  private final int raised;
  private final Currency currency;
  @Builder.Default
  private final Set<String> bankAccounts = Set.of();
  private final Instant createdAt;
  private final Instant updatedAt;
  private final boolean monoOnly;

  public String getEnabledNkey() {
    return enabled ? ENABLED_N_KEY : DISABLED_N_KEY;
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
      bankAccounts.isEmpty() ? "" : " - :bank:-" + bankAccounts.size()
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
      bankAccounts.isEmpty() ? "" : " - :bank:-" + bankAccounts.size()
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
