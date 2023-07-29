package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_BANK_ACCOUNT_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
class DynamoDbFundStorageClientTest extends AbstractFundOperationsTestCommon {

  private static final String OTHER_ACCOUNT_ID = "some-other-account-id";

  @Test
  void shouldFindOnlyActiveFundByBankAccountId() {
    final Fund activeFund = FUND.toBuilder()
      .bankAccounts(StreamEx.of(FUND.getBankAccounts()).append("account").toImmutableSet())
      .build();
    fundStorageClient.save(activeFund);

    createDisabledFund();

    final Optional<Fund> result = fundStorageClient.getActiveFundByBankAccount(FUND_BANK_ACCOUNT_ID);
    assertThat(result).hasValue(activeFund);
  }

  @Test
  void shouldNotFindActiveFundByBankAccountIfNoSuchFund() {
    createDisabledFund();

    final Optional<Fund> result = fundStorageClient.getActiveFundByBankAccount(OTHER_ACCOUNT_ID);
    assertThat(result).isEmpty();
  }

  private void createDisabledFund() {
    fundStorageClient.save(Fund.builder()
      .id("secondFundId")
      .enabled(false)
      .name("fund name")
      .owner("owner")
      .description("desc")
      .color("color")
      .bankAccounts(Set.of(FUND_BANK_ACCOUNT_ID, OTHER_ACCOUNT_ID))
      .currency(Currency.UAH)
      .goal(10)
      .createdAt(Instant.now())
      .updatedAt(Instant.now())
      .build());
  }

}
