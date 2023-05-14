package com.yuriytkach.tracker.fundraiser.privatbank;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.privatbank.api.PrivatApi;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Balance;
import com.yuriytkach.tracker.fundraiser.privatbank.model.BalancesResponse;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Transaction;
import com.yuriytkach.tracker.fundraiser.privatbank.model.TransactionsResponse;
import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;
import com.yuriytkach.tracker.fundraiser.service.FundService;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

@ExtendWith(MockitoExtension.class)
class PrivatbankServiceTest {

  private static final String TOKEN_SECRET_NAME = "name";
  private static final String TOKEN = "token";
  private static final String PRIVAT_ACCOUNT = "privatAcc";
  private static final Fund FUND_FOR_PRIVAT_ACCOUNT = Fund.builder()
    .id("fundId")
    .bankAccounts(Set.of(PRIVAT_ACCOUNT))
    .build();
  private static final int SYNC_DURATION_DAYS = 2;

  @Mock
  private PrivatbankProperties properties;

  @Mock
  private SecretsReader secretsReader;

  @Mock
  private FundService fundService;

  @Mock
  private PrivatApi api;

  @Mock
  private PrivatbankTransactionSyncService privatbankTransactionSyncService;


  @InjectMocks
  private PrivatbankService tested;

  @BeforeEach
  void setupProps() {
    when(properties.tokenSecretName()).thenReturn(TOKEN_SECRET_NAME);
  }

  @Nested
  class HappyPassTests {
    @Test
    void shouldSyncTransactions() {
      when(properties.syncPeriodDays()).thenReturn(SYNC_DURATION_DAYS);
      when(secretsReader.readSecret(any())).thenReturn(Optional.of(TOKEN));
      when(api.finalBalances(any())).thenReturn(new BalancesResponse("OK", false, "id", List.of(
        new Balance(PRIVAT_ACCOUNT)
      )));
      when(fundService.findAllEnabled()).thenReturn(List.of(FUND_FOR_PRIVAT_ACCOUNT));
      final Transaction transactionMock = mock(Transaction.class);
      when(api.transactions(any(), any(), any())).thenReturn(new TransactionsResponse("OK", false, "id", List.of(
        transactionMock
      )));

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verify(api).finalBalances(TOKEN);
      verify(api).transactions(
        TOKEN,
        PrivatbankService.DATE_FORMATTER.format(LocalDate.now().minusDays(SYNC_DURATION_DAYS)),
        PRIVAT_ACCOUNT
      );
      verify(fundService).findAllEnabled();
      verify(privatbankTransactionSyncService).syncTransactions(FUND_FOR_PRIVAT_ACCOUNT, Set.of(transactionMock));
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class FailureTests {

    @Test
    void shouldNotSyncIfTokenIsNotRead() {
      when(secretsReader.readSecret(any())).thenReturn(Optional.empty());

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verifyNoInteractions(fundService, api, privatbankTransactionSyncService);
    }

    @Test
    void shouldNotSyncIfNoAvailablePrivatAccounts() {
      when(secretsReader.readSecret(any())).thenReturn(Optional.of(TOKEN));
      when(api.finalBalances(any())).thenReturn(new BalancesResponse("OK", false, "id", List.of()));

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verify(api).finalBalances(TOKEN);
      verifyNoInteractions(fundService, privatbankTransactionSyncService);
      verifyNoMoreInteractions(api);
    }

    @ParameterizedTest
    @ValueSource(classes = { WebApplicationException.class, ProcessingException.class })
    void shouldNotSyncIfFailedToGetPrivatAccounts(final Class<? extends Throwable> exceptionClass) {
      when(secretsReader.readSecret(any())).thenReturn(Optional.of(TOKEN));
      when(api.finalBalances(any())).thenThrow(exceptionClass);

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verify(api).finalBalances(TOKEN);
      verifyNoInteractions(fundService, privatbankTransactionSyncService);
      verifyNoMoreInteractions(api);
    }

    Stream<Arguments> noValidFundsParams() {
      final Fund differentAccountFund = mock(Fund.class);
      when(differentAccountFund.getBankAccounts()).thenReturn(Set.of("ptn-pnh"));
      final Fund noAccountsFund = mock(Fund.class);
      when(noAccountsFund.getBankAccounts()).thenReturn(Set.of());
      return Stream.of(
        Arguments.of(List.of()),
        Arguments.of(List.of(differentAccountFund, noAccountsFund))
      );
    }

    @ParameterizedTest
    @MethodSource("noValidFundsParams")
    void shouldNotSyncIfNoEnabledFundsByAccount(final List<Fund> funds) {
      when(secretsReader.readSecret(any())).thenReturn(Optional.of(TOKEN));
      when(api.finalBalances(any())).thenReturn(new BalancesResponse("OK", false, "id", List.of(
        new Balance(PRIVAT_ACCOUNT)
      )));
      when(fundService.findAllEnabled()).thenReturn(funds);

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verify(api).finalBalances(TOKEN);
      verify(fundService).findAllEnabled();
      verifyNoInteractions(privatbankTransactionSyncService);
      verifyNoMoreInteractions(api);
    }

    @Test
    void shouldNotSyncIfNoTransactionsFound() {
      when(properties.syncPeriodDays()).thenReturn(SYNC_DURATION_DAYS);
      when(secretsReader.readSecret(any())).thenReturn(Optional.of(TOKEN));
      when(api.finalBalances(any())).thenReturn(new BalancesResponse("OK", false, "id", List.of(
        new Balance(PRIVAT_ACCOUNT)
      )));
      when(fundService.findAllEnabled()).thenReturn(List.of(FUND_FOR_PRIVAT_ACCOUNT));
      when(api.transactions(any(), any(), any())).thenReturn(new TransactionsResponse("OK", false, "id", List.of()));

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verify(api).finalBalances(TOKEN);
      verify(api).transactions(
        TOKEN,
        PrivatbankService.DATE_FORMATTER.format(LocalDate.now().minusDays(SYNC_DURATION_DAYS)),
        PRIVAT_ACCOUNT
      );
      verify(fundService).findAllEnabled();
      verifyNoInteractions(privatbankTransactionSyncService);
    }

    @ParameterizedTest
    @ValueSource(classes = { WebApplicationException.class, ProcessingException.class })
    void shouldNotSyncIfFailsToGetTransactions(final Class<? extends Throwable> exceptionClass) {
      when(properties.syncPeriodDays()).thenReturn(SYNC_DURATION_DAYS);
      when(secretsReader.readSecret(any())).thenReturn(Optional.of(TOKEN));
      when(api.finalBalances(any())).thenReturn(new BalancesResponse("OK", false, "id", List.of(
        new Balance(PRIVAT_ACCOUNT)
      )));
      when(fundService.findAllEnabled()).thenReturn(List.of(FUND_FOR_PRIVAT_ACCOUNT));
      when(api.transactions(any(), any(), any())).thenThrow(exceptionClass);

      tested.syncData();

      verify(secretsReader).readSecret(TOKEN_SECRET_NAME);
      verify(api).finalBalances(TOKEN);
      verify(api).transactions(
        TOKEN,
        PrivatbankService.DATE_FORMATTER.format(LocalDate.now().minusDays(SYNC_DURATION_DAYS)),
        PRIVAT_ACCOUNT
      );
      verify(fundService).findAllEnabled();
      verifyNoInteractions(privatbankTransactionSyncService);
    }
  }
}
