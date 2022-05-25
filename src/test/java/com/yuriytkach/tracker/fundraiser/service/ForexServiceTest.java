package com.yuriytkach.tracker.fundraiser.service;

import static com.yuriytkach.tracker.fundraiser.model.Currency.UAH;
import static com.yuriytkach.tracker.fundraiser.model.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.forex.MonoCurrencyRate;
import com.yuriytkach.tracker.fundraiser.forex.MonobankApi;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.exception.UnknownForexException;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class ForexServiceTest {

  @Mock
  MonobankApi monobankApi;

  @InjectMocks
  ForexService tested;

  @BeforeEach
  @SneakyThrows
  @SuppressWarnings("unchecked")
  void initRestClientResponse() {
    final List<MonoCurrencyRate> currencies;

    try(var is = this.getClass().getClassLoader().getResourceAsStream("monobank_response.json")) {
      currencies = new ObjectMapper().readerForListOf(MonoCurrencyRate.class).readValue(is);
    }

    lenient().when(monobankApi.currencies()).thenReturn(currencies);
  }

  @Test
  @SneakyThrows
  void shouldReturnSameAmountIfSameCurrency() {
    assertThat(tested.convertCurrency(10, UAH, UAH)).isEqualTo(10);
    verifyNoInteractions(monobankApi);
  }

  @Test
  void shouldThrowExceptionIfNoForexConfig() {
    when(monobankApi.currencies()).thenReturn(List.of());
    assertThatThrownBy(() -> tested.convertCurrency(10, UAH, USD))
      .isInstanceOf(UnknownForexException.class)
      .hasMessageContaining("UAH to USD");
  }

  @ParameterizedTest
  @CsvSource({
    "UAH, USD, 30",
    "USD, UAH, 29500",
    "EUR, USD, 1057",
    "USD, EUR, 928"
  })
  void shouldConvertWithRate(final Currency from, final Currency to, final int expected) {
    assertThat(tested.convertCurrency(1000, from, to)).isEqualTo(expected);
  }

  @ParameterizedTest
  @EnumSource(Currency.class)
  void shouldConvertToAllSupportedCurrencies(final Currency currency) {
    assertThat(tested.convertCurrency(1000, UAH, currency)).isGreaterThan(0);
  }

}
