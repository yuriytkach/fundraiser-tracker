package com.yuriytkach.tracker.fundraiser.privatbank;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Transaction;
import com.yuriytkach.tracker.fundraiser.service.PersonNameConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
class PrivatbankTransactionMapper {

  static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
  static final String TRANSACTION_TYPE_CREDIT = "C";
  static final String TRANSACTION_STATUS_PASSED = "r";

  private final PrivatbankProperties properties;
  private final PersonNameConverter personNameConverter;

  Optional<Donation> convertToDonation(final Transaction transaction) {
    if (!verifyTransaction(transaction)) {
      log.debug("Skipping transaction of type `{}` and status `{}`", transaction.getType(), transaction.getStatus());
      return Optional.empty();
    }

    final Optional<Currency> currencyOpt = Currency.fromString(transaction.getCurr());
    if (currencyOpt.isEmpty()) {
      log.warn("Cannot convert privatbank currency: {}", transaction.getCurr());
      return Optional.empty();
    }

    final Optional<Integer> amountOpt = safeParseAndCheckAmount(transaction.getSum());
    if (amountOpt.isEmpty()) {
      return Optional.empty();
    }

    final Optional<Instant> dateTimeOpt = safeParseDateTime(transaction.getDateTime());
    if (dateTimeOpt.isEmpty()) {
      return Optional.empty();
    }

    final var donation = Donation.builder()
      .id(transaction.getId())
      .amount(amountOpt.orElseThrow())
      .currency(currencyOpt.orElseThrow())
      .dateTime(dateTimeOpt.orElseThrow())
      .person(personNameConverter.convertFromPrivatDescription(transaction.getDescription()))
      .build();

    return Optional.of(donation);
  }

  private boolean verifyTransaction(final Transaction transaction) {
    return transaction.getType().equals(TRANSACTION_TYPE_CREDIT)
      && transaction.getStatus().equals(TRANSACTION_STATUS_PASSED);
  }

  private Optional<Instant> safeParseDateTime(final String dateTime) {
    try {
      final ZoneOffset zoneOffset = properties.timeZoneId().getRules().getOffset(Instant.now());
      final TemporalAccessor parsed = DATE_TIME_FORMATTER.parse(dateTime);
      return Optional.of(LocalDateTime.from(parsed).toInstant(zoneOffset));
    } catch (final Exception ex) {
      log.warn("Cannot parse privatbank datetime: {} {}", dateTime, ex.getMessage());
      return Optional.empty();
    }
  }

  private Optional<Integer> safeParseAndCheckAmount(final String sum) {
    try {
      final int amount = Double.valueOf(sum).intValue();
      if (amount > 0) {
        return Optional.of(amount);
      } else {
        log.debug("Skipping transaction because amount: {}", amount);
        return Optional.empty();
      }
    } catch (final NumberFormatException ex) {
      log.warn("Cannot parse privatbank amount: {} {}", sum, ex.getMessage());
      return Optional.empty();
    }
  }

}
