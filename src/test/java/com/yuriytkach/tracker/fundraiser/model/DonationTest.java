package com.yuriytkach.tracker.fundraiser.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DonationTest {

  @Test
  void shouldOutputStringLong() {
    final Donation donation = createDonation();

    assertThat(donation.toStringLong()).isEqualTo(
      "2022-02-03 13:12 - UAH   300 - person (_00000000-0000-0001-0000-000000000001_)"
    );
  }

  @Test
  void shouldOutputStringShort() {
    final Donation donation = createDonation();

    assertThat(donation.toStringShort()).isEqualTo(
      "300 UAH by person at 2022-02-03 13:12"
    );
  }

  private Donation createDonation() {
    return Donation.builder()
      .id(new UUID(1, 1).toString())
      .person("person")
      .amount(300)
      .currency(Currency.UAH)
      .dateTime(Instant.parse("2022-02-03T10:12:15Z"))
      .build();
  }

}
