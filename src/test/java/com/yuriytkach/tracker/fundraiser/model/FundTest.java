package com.yuriytkach.tracker.fundraiser.model;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

  @Test
  void shouldReturnOutputStringLong() {
    final Fund fund = createFund(now());
    assertThat(fund.toOutputStringLong()).isEqualTo(
      "30.00% `name` [30 of 100] UAH - desc [color] - :open_book: 0 hour(s)"
    );
  }

  @Test
  void shouldReturnOutputStringLongWithMono() {
    final Fund fund = createFund(now()).toBuilder().monobankAccount("acc").build();
    assertThat(fund.toOutputStringLong()).isEqualTo(
      "30.00% `name` [30 of 100] UAH - desc [color] - :open_book: 0 hour(s) - :cat:"
    );
  }

  @Test
  void shouldReturnOutputStringShort() {
    final Fund fund = createFund(now());
    assertThat(fund.toOutputStringShort()).isEqualTo(
      "`name` 30.00% [30 of 100] UAH"
    );
  }

  @Test
  void shouldReturnOutputStringShortWithMono() {
    final Fund fund = createFund(now()).toBuilder().monobankAccount("account").build();
    assertThat(fund.toOutputStringShort()).isEqualTo(
      "`name` 30.00% [30 of 100] UAH Mono"
    );
  }

  @Test
  void shouldReturnFundDurationString() {
    final Instant minDay = now().minusSeconds(60 * 60 * 25);
    assertThat(createFund(minDay).toFundDurationString()).isEqualTo("1 day(s), 1 hour(s)");

    final Instant minHour = now().minusSeconds(60 * 60 * 3);
    assertThat(createFund(minHour).toFundDurationString()).isEqualTo("3 hour(s)");
  }

  private Fund createFund(final Instant createdAt) {
    return Fund.builder()
      .id("id")
      .name("name")
      .raised(30)
      .goal(100)
      .currency(Currency.UAH)
      .owner("owner")
      .color("color")
      .description("desc")
      .updatedAt(now())
      .createdAt(createdAt)
      .build();
  }

}
