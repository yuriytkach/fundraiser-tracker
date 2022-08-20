package com.yuriytkach.tracker.fundraiser.forex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuriytkach.tracker.fundraiser.model.Currency;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.SneakyThrows;

@QuarkusTest
@QuarkusTestResource(MockServerTestResource.class)
class ForexServiceIntegrationTest {

  @Inject
  ForexService tested;

  private MockServerClient mockServerClient;

  @BeforeEach
  @SneakyThrows
  void init() {
    final var host = MockServerTestResource.CONTAINER.getHost();
    final var port = MockServerTestResource.CONTAINER.getServerPort();

    mockServerClient = new MockServerClient(host, port);
  }

  @ParameterizedTest
  @CsvSource({
    "UAH, USD, 30",
    "USD, UAH, 29500",
    "EUR, USD, 1057",
    "USD, EUR, 928"
  })
  void shouldConvertWithRate(final Currency from, final Currency to, final int expected) {
    initMockGoodResponse();
    assertThat(tested.convertCurrency(1000, from, to)).isEqualTo(expected);
  }

  @Test
  void shouldThrowExceptionOnWebBadResponse() {
    mockServerClient
      .when(request()
        .withPath("/bank/currency"))
      .respond(response()
        .withStatusCode(500));

    assertThatThrownBy(() -> tested.convertCurrency(1000, Currency.CHF, Currency.GBP))
      .isInstanceOf(WebApplicationException.class);
  }

  @SneakyThrows
  private void initMockGoodResponse() {
    final List<MonoCurrencyRate> currencies;

    try (var is = this.getClass().getClassLoader().getResourceAsStream("monobank_response.json")) {
      currencies = new ObjectMapper().readerForListOf(MonoCurrencyRate.class).readValue(is);
    }

    mockServerClient.reset();
    mockServerClient
      .when(request()
        .withPath("/bank/currency"))
      .respond(response()
        .withBody(new ObjectMapper().writeValueAsString(currencies), MediaType.JSON_UTF_8));
  }

}
