package com.yuriytkach.tracker.fundraiser.model;

import java.util.Optional;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Currency {
  UAH(980),
  USD(840),
  EUR(978),
  PLN(985),
  GBP(826),
  CHF(756);

  private final int isoCode;

  public static Optional<Currency> fromString(final String text) {
    return Stream.of(Currency.values())
      .filter(value -> value.name().equalsIgnoreCase(text))
      .findFirst();
  }

  public static Optional<Currency> fromIsoCode(final int isoCode) {
    return Stream.of(Currency.values())
      .filter(value -> value.getIsoCode() == isoCode)
      .findFirst();
  }
}
