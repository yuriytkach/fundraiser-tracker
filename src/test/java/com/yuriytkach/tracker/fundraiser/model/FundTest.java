package com.yuriytkach.tracker.fundraiser.model;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FundTest {

  @ParameterizedTest
  @ValueSource(doubles = { 30.0, 50.0 })
  void shouldReturnRaisedPercent(final double raised) {
    final var fund = Fund.builder()
      .raised((int) raised)
      .goal(100)
      .build();

    assertThat(fund.getRaisedPercent()).isCloseTo(raised, Offset.offset(0.001));
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void shouldReturnOutputStringLong(final boolean enabled) {
    final Fund fund = createFund(enabled, now(), now());
    assertThat(fund.toOutputStringLong()).isEqualTo(
      (enabled ? ":open_book:" : ":closed_book:")
        + " 30.00% `name` [30 of 100] UAH - desc [color] - "
        + (enabled ? "0 h" : "0 d (Closed 0 d ago)")
    );
  }

  @Test
  void shouldReturnOutputStringLongWithMono() {
    final Fund fund = createFund(now()).toBuilder().monobankAccount("acc").build();
    assertThat(fund.toOutputStringLong()).isEqualTo(
      ":open_book: 30.00% `name` [30 of 100] UAH - desc [color] - 0 h - :cat:"
    );
  }

  @Test
  void shouldReturnOutputStringShort() {
    final Fund fund = createFund(now());
    assertThat(fund.toOutputStringShort()).isEqualTo(
      ":open_book: `name` 30.00% [30 of 100] UAH"
    );
  }

  @Test
  void shouldReturnOutputStringShortWithMono() {
    final Fund fund = createFund(now()).toBuilder().monobankAccount("account").build();
    assertThat(fund.toOutputStringShort()).isEqualTo(
      ":open_book: `name` 30.00% [30 of 100] UAH Mono"
    );
  }

  @ParameterizedTest
  @CsvSource({
    "true, 25, 0, '1 d, 1 h'",
    "true, 3, 0, '3 h'",
    "false, 9, 3, '0 d (Closed 0 d ago)'",
    "false, 96, 25, '2 d (Closed 1 d ago)'",
  })
  void shouldReturnFundDurationString(
    final boolean enabled,
    final int createdAtHoursMinus,
    final int updatedAtHoursMinus,
    final String expected
  ) {
    final var fund = createFund(
      enabled,
      now().minus(createdAtHoursMinus, HOURS),
      now().minus(updatedAtHoursMinus, HOURS)
    );
    assertThat(fund.toFundDurationString()).isEqualTo(expected);
  }

  private Fund createFund(final Instant createdAt) {
    return createFund(true, createdAt, now());
  }

  private Fund createFund(final boolean enabled, final Instant createdAt, final Instant updatedAt) {
    return Fund.builder()
      .id("id")
      .enabled(enabled)
      .name("name")
      .raised(30)
      .goal(100)
      .currency(Currency.UAH)
      .owner("owner")
      .color("color")
      .description("desc")
      .updatedAt(updatedAt)
      .createdAt(createdAt)
      .build();
  }

}
