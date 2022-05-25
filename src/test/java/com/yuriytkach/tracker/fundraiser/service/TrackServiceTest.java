package com.yuriytkach.tracker.fundraiser.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TrackServiceTest {

  @ParameterizedTest
  @CsvSource({
    "list, true",
    "list fund, true",
    "abc      hello, true",
    "'', false"
  })
  void shouldMatchCommand(final String text, final boolean expected) {
    final Matcher matcher = TrackService.CMD_PATTERN.matcher(text);
    assertThat(matcher.matches()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "fund eur 123, true, fund, eur, 123, , ",
    "fund eur 123 \"description\", true, fund, eur, 123, description, ",
    "fund eur 123 \"description\" color, true, fund, eur, 123, description, color",
    "fund eur 123 444, true, fund, eur, 123, , 444",
    "fund eur 123 0xfff, true, fund, eur, 123, , 0xfff",
    "fund eur 123 color ffff, false, , , , , ",
    "fund-yeah eur 123, false, , , , , ",
    "eur 123, false, , , , , ",
    "fund eur, false, , , , , ",
    "fund 123, false, , , , , "
  })
  void shouldMatchCreateCommand(
    final String text,
    final boolean expected,
    final String expectedFund,
    final String expectedCurr,
    final String expectedAmt,
    final String expectedDesc,
    final String expectedColor
  ) {
    final Matcher matcher = TrackService.CREATE_PATTERN.matcher(text);
    assertThat(matcher.matches()).isEqualTo(expected);

    if (expected) {
      assertThat(matcher.group("name")).isEqualTo(expectedFund);
      assertThat(matcher.group("curr")).isEqualTo(expectedCurr);
      assertThat(matcher.group("goal")).isEqualTo(expectedAmt);
      assertThat(matcher.group("desc")).isEqualTo(expectedDesc);
      assertThat(matcher.group("color")).isEqualTo(expectedColor);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "'', true",
    "fund, true",
    "'  ', false"
  })
  void shouldMatchListCommand(final String text, final boolean expected) {
    final Matcher matcher = TrackService.LIST_PATTERN.matcher(text);
    assertThat(matcher.matches()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "'', false",
    "fund, true",
    "'  ', false"
  })
  void shouldMatchDeleteCommand(final String text, final boolean expected) {
    final Matcher matcher = TrackService.DELETE_PATTERN.matcher(text);
    assertThat(matcher.matches()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "fund UAH 123, true",
    "fund UAH 123 person, true",
    "fund UAH 123 person 2022-05-06 12:13, true",
    "fund UAH 123 person 12:13, true",
    "fund UAH 123 2022-05-06 12:13, true",
    "fund UAH 123 12:13, true",
    "UAH 123 person, false",
    "UAH 123, false",
    "fund us 123, false",
    "fund usd abc, false",
    "fund usd123, false"
  })
  void shouldMatchTrackCommand(final String text, final boolean expected) {
    final Matcher matcher = TrackService.TRACK_PATTERN.matcher(text);
    assertThat(matcher.matches()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "2022-02-01 05:03, 2022-02-01T05:03:00",
    "2022-02-01 15:13, 2022-02-01T15:13:00"
  })
  void shouldParseDateAndTime(final String text, final LocalDateTime expected) {
    final var parsed = TrackService.DATE_TIME_FORMATTER.parse(text);
    final var dateTime = LocalDateTime.from(parsed);

    assertThat(dateTime).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "05:03, 05:03:00",
    "15:13, 15:13:00"
  })
  void shouldParseTimeOnly(final String text, final LocalTime expected) {
    final var parsed = TrackService.DATE_TIME_FORMATTER_ONLY_TIME.parse(text);
    final var time = LocalTime.from(parsed);

    assertThat(time).isEqualTo(expected);
  }

}
