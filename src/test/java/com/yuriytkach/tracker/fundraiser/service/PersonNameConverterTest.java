package com.yuriytkach.tracker.fundraiser.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PersonNameConverterTest {

  private final PersonNameConverter tested = new PersonNameConverter();

  @ParameterizedTest
  @CsvSource({
    ", noname",
    "' ', noname",
    "Юрій Ткач, YuriyT",
    "Слава Україні, SlavaU",
    "Ігор, Ihor",
    "Andriy Bar, Andriy",
    "Мар'яна, Maryana",
    "Сіль, Sil",
    "Від: \uD83D\uDC08, \uD83D\uDC08",
    "Від: Олексій, Oleksiy",
  })
  void shouldConvertFromMonoDescription(final String name, final String expected) {
    assertThat(tested.convertFromMonoDescription(name)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    ", noname",
    "' ', noname",
    "Благодійний внесок Платник: ІПН9998887777ТКАЧ ЮРІЙ БезПДВ, YuriyT",
    "'Збір на, Червона Мар'яна Володимирівна', MaryanaCH",
    "'Оплата за ..., White Walter Walterovych', Walter",
  })
  void shouldConvertFromPrivat(final String description, final String expected) {
    assertThat(tested.convertFromPrivatDescription(description)).isEqualTo(expected);
  }

}
