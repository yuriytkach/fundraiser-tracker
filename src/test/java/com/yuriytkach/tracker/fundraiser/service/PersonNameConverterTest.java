package com.yuriytkach.tracker.fundraiser.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PersonNameConverterTest {

  private final PersonNameConverter tested = new PersonNameConverter();

  @ParameterizedTest
  @CsvSource({
    "Юрій Ткач, YuriyT",
    "Слава Україні, SlavaU",
    "Ігор, Ihor",
    "Andriy Bar, Andriy",
    "Мар'яна, Maryana",
    "Сіль, Sil",
    "Від: \uD83D\uDC08, \uD83D\uDC08",
    "Від: Олексій, Oleksiy",
  })
  void shouldConvertNames(final String name, final String expected) {
    assertThat(tested.convertToPersonName(name)).isEqualTo(expected);
  }

}
