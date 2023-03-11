package com.yuriytkach.tracker.fundraiser.privatbank;

import static com.yuriytkach.tracker.fundraiser.privatbank.PrivatbankTransactionMapper.TRANSACTION_STATUS_PASSED;
import static com.yuriytkach.tracker.fundraiser.privatbank.PrivatbankTransactionMapper.TRANSACTION_TYPE_CREDIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Transaction;
import com.yuriytkach.tracker.fundraiser.service.PersonNameConverter;

@ExtendWith(MockitoExtension.class)
class PrivatbankTransactionMapperTest {

  private static final ZoneId ZONE_ID = ZoneId.of("Europe/Kiev");

  @Mock
  private PrivatbankProperties properties;

  @Mock
  private PersonNameConverter personNameConverter;

  @InjectMocks
  private PrivatbankTransactionMapper tested;

  @ParameterizedTest
  @CsvSource({
    "aaa, 800.00, 01.03.2023 09:02:00, " + TRANSACTION_STATUS_PASSED + ", " + TRANSACTION_TYPE_CREDIT,
    "UAH, xxx, 01.03.2023 09:02:00, " + TRANSACTION_STATUS_PASSED + ", " + TRANSACTION_TYPE_CREDIT,
    "UAH, 800.00, xxx, " + TRANSACTION_STATUS_PASSED + ", " + TRANSACTION_TYPE_CREDIT,
    "UAH, 800.00, 01.03.2023 09:02:00, Q, " + TRANSACTION_TYPE_CREDIT,
    "UAH, 800.00, 01.03.2023 09:02:00, " + TRANSACTION_STATUS_PASSED + ", Q",
    "UAH, 0, 01.03.2023 09:02:00, " + TRANSACTION_STATUS_PASSED + ", " + TRANSACTION_TYPE_CREDIT,
    "UAH, -100, 01.03.2023 09:02:00, " + TRANSACTION_STATUS_PASSED + ", " + TRANSACTION_TYPE_CREDIT,
  })
  void shouldReturnEmptyIfFailedToConvertFieldsOrInvalidFields(
    final String currency,
    final String amount,
    final String dateTime,
    final String status,
    final String type
  ) {
    lenient().when(properties.timeZoneId()).thenReturn(ZONE_ID);

    final Transaction transaction = new Transaction(
      "id", currency, status, type, dateTime, "accountId", amount, "100", "desc"
    );
    assertThat(tested.convertToDonation(transaction)).isEmpty();
  }

  @Test
  void shouldMapDonation() {
    when(properties.timeZoneId()).thenReturn(ZONE_ID);
    when(personNameConverter.convertFromPrivatDescription(anyString())).thenReturn("name");

    final Transaction transaction = new Transaction(
      "id",
      "UAH",
      TRANSACTION_STATUS_PASSED,
      TRANSACTION_TYPE_CREDIT,
      "01.03.2023 09:02:04",
      "accountId",
      "800.00",
      "100",
      "desc"
    );

    final Optional<Donation> donation = tested.convertToDonation(transaction);

    assertThat(donation).hasValue(Donation.builder()
      .id("id")
      .person("name")
      .amount(800)
      .currency(Currency.UAH)
      .dateTime(LocalDateTime.parse("2023-03-01T09:02:04").toInstant(ZONE_ID.getRules().getOffset(Instant.now())))
      .build());

    verify(personNameConverter).convertFromPrivatDescription("desc");
  }
}
