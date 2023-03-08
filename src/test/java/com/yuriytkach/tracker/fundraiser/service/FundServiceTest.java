package com.yuriytkach.tracker.fundraiser.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.model.Fund;

@ExtendWith(MockitoExtension.class)
class FundServiceTest {

  private static final String ACCOUNT_ID = "accountId";

  @Mock
  private FundStorageClient fundStorageClient;

  @InjectMocks
  private FundService tested;

  @Test
  void shouldReturnEmptyIfFundIsNotFoundWhenSearchedByBankAccount() {
    when(fundStorageClient.getActiveFundByBankAccount(any())).thenReturn(Optional.empty());

    final Optional<Fund> result = tested.findEnabledByMonoAccount(ACCOUNT_ID);
    assertThat(result).isEmpty();

    verify(fundStorageClient).getActiveFundByBankAccount(ACCOUNT_ID);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void shouldReturnEmptyIfFundIsDisabledWhenSearchedByBankAccount(final boolean enabled) {
    final Fund mockFund = mock(Fund.class);
    when(mockFund.isEnabled()).thenReturn(enabled);

    when(fundStorageClient.getActiveFundByBankAccount(any())).thenReturn(Optional.of(mockFund));

    final Optional<Fund> result = tested.findEnabledByMonoAccount(ACCOUNT_ID);
    if (enabled) {
      assertThat(result).hasValue(mockFund);
    } else {
      assertThat(result).isEmpty();
    }

    verify(fundStorageClient).getActiveFundByBankAccount(ACCOUNT_ID);
  }

}
