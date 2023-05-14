package com.yuriytkach.tracker.fundraiser.privatbank;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.privatbank.api.PrivatApi;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Balance;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Transaction;
import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;
import com.yuriytkach.tracker.fundraiser.service.FundService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@ApplicationScoped
public class PrivatbankService {

  static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Inject
  PrivatbankProperties properties;

  @Inject
  SecretsReader secretsReader;

  @Inject
  FundService fundService;

  @Inject
  @RestClient
  PrivatApi api;

  @Inject
  PrivatbankTransactionSyncService privatbankTransactionSyncService;

  public void syncData() {
    secretsReader.readSecret(properties.tokenSecretName()).ifPresentOrElse(
      this::syncPrivatData,
      () -> log.warn("Privatbank token was not found in secrets reader by name: {}", properties.tokenSecretName())
    );
  }

  private void syncPrivatData(final String token) {
    final Set<String> allAccounts = readAllAvailablePrivatAccounts(token);
    log.info("Available bank accounts: {}", allAccounts.size());

    if (!allAccounts.isEmpty()) {
      final List<Fund> allEnabledFunds = fundService.findAllEnabled();

      final Map<String, Fund> accountsToSync = findAccountsToSync(allAccounts, allEnabledFunds);
      log.info("Accounts to sync: {}", accountsToSync.size());

      accountsToSync.forEach((acc, fund) -> syncPrivatAccount(token, acc, fund));
    }
  }

  private void syncPrivatAccount(final String token, final String account, final Fund fund) {
    final LocalDate startDate = LocalDate.now().minusDays(properties.syncPeriodDays());
    final Set<Transaction> transactions = readAllAccountTransactions(token, startDate, account);

    log.info("Read transactions in account {} from {}: {}", account, startDate, transactions.size());
    if (!transactions.isEmpty()) {
      privatbankTransactionSyncService.syncTransactions(fund, transactions);
    }
  }

  private Map<String, Fund> findAccountsToSync(
    final Collection<String> allAccounts,
    final Collection<Fund> allEnabledFunds
  ) {
    return StreamEx.of(allEnabledFunds)
      .mapToEntry(Fund::getBankAccounts)
      .flatMapValues(Collection::stream)
      .filterValues(allAccounts::contains)
      .invert()
      .toImmutableMap();
  }

  private Set<Transaction> readAllAccountTransactions(
    final String token,
    final LocalDate startDate,
    final String account
  ) {
    try {
      final var response = api.transactions(token, DATE_FORMATTER.format(startDate), account);
      return Set.copyOf(response.getTransactions());

    } catch (final WebApplicationException ex) {
      final Response response = ex.getResponse();
      final String body;
      final int status;
      if (response == null) {
        body = ex.getMessage();
        status = -1;
      } else {
        body = response.hasEntity() ? response.readEntity(String.class) : "<no body>";
        status = response.getStatus();
      }
      log.warn("Failed to get transactions: [{}] {}", status, body);

    } catch (final ProcessingException ex) {
      log.error("Failed to get transactions: {}", ex.getMessage());
    }

    return Set.of();
  }

  private Set<String> readAllAvailablePrivatAccounts(final String token) {
    try {
      final var response = api.finalBalances(token);

      return StreamEx.of(response.getBalances())
        .map(Balance::getAcc)
        .toImmutableSet();

    } catch (final WebApplicationException ex) {
      final Response response = ex.getResponse();
      final String body;
      final int status;
      if (response == null) {
        body = ex.getMessage();
        status = -1;
      } else {
        body = response.hasEntity() ? response.readEntity(String.class) : "<no body>";
        status = response.getStatus();
      }
      log.warn("Failed to get all balances: [{}] {}", status, body);

    } catch (final ProcessingException ex) {
      log.error("Failed to get all balances: {}", ex.getMessage());
    }

    return Set.of();
  }
}
