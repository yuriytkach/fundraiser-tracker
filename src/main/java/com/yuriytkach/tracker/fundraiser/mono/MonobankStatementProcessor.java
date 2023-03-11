package com.yuriytkach.tracker.fundraiser.mono;

import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.mono.model.MonobankStatement;
import com.yuriytkach.tracker.fundraiser.service.DonationTracker;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.PersonNameConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MonobankStatementProcessor {

  private final FundService fundService;
  private final DonationTracker donationTracker;
  private final PersonNameConverter nameConverter;

  public void processStatement(final MonobankStatement statement) {
    if (statement.getData().getStatementItem().getAmount() < 0) {
      log.info("Amount is less then 0. Skipping statement processing");
      return;
    }

    if (Currency.fromIsoCode(statement.getData().getStatementItem().getCurrencyCode()).isEmpty()) {
      log.warn("No currency found for ISO code: {}", statement.getData().getStatementItem().getCurrencyCode());
      return;
    }

    log.debug("Received mono statement: {}", statement);

    fundService.findEnabledByBankAccount(statement.getData().getAccount())
      .ifPresent(fund -> trackStatementForFund(fund, statement.getData().getStatementItem()));
  }

  private void trackStatementForFund(final Fund fund, final MonobankStatement.MonobankStatementItem statementItem) {
    final Donation donation = createDonation(statementItem);
    donationTracker.trackDonation(fund, donation);
  }

  private Donation createDonation(final MonobankStatement.MonobankStatementItem statementItem) {
    return Donation.builder()
      .id(statementItem.getId())
      .currency(Currency.fromIsoCode(statementItem.getCurrencyCode()).orElseThrow()) // verified above
      .amount(statementItem.getAmount() / 100)
      .dateTime(Instant.ofEpochSecond(statementItem.getTime()))
      .person(nameConverter.convertFromMonoDescription(statementItem.getDescription()))
      .build();
  }

}
