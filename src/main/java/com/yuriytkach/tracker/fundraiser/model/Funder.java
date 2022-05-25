package com.yuriytkach.tracker.fundraiser.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@RegisterForReflection
public class Funder {
  private final String name;
  private final String fundedAt;
  private final int amount;
  private final Currency currency;

  public static Funder fromDonation(final Donation donation) {
    return Funder.builder()
      .name(donation.getPerson())
      .amount(donation.getAmount())
      .currency(donation.getCurrency())
      .fundedAt(donation.getDateTime().toString())
      .build();
  }
}
