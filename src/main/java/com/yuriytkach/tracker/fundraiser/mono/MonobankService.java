package com.yuriytkach.tracker.fundraiser.mono;

import java.time.Instant;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.mono.api.MonoApi;
import com.yuriytkach.tracker.fundraiser.service.FundService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class MonobankService {

  @Inject
  FundService fundService;

  @Inject
  ForexService forexService;

  @Inject
  @RestClient
  MonoApi api;

  public void syncData() {
    fundService.findAllEnabled().stream()
      .filter(Fund::isMonoOnly)
      .forEach(this::syncMonoJarTotal);
  }

  private void syncMonoJarTotal(final Fund fund) {
    log.info("Syncing mono jar for fund: {}", fund.getName());
    try {
      final var jar = api.jarStatus(fund.getBankAccounts().stream().findFirst().orElseThrow());
      final Currency currency = Currency.fromIsoCode(jar.currency()).orElseThrow();

      final int fundNewAmount = forexService.convertCurrency((int) jar.amount(), currency, fund.getCurrency()) / 100;

      if (fundNewAmount > fund.getRaised()) {
        log.info("New amount is greater than current: {}", fundNewAmount);
        final var newFund = fund.toBuilder()
          .raised(fundNewAmount)
          .updatedAt(Instant.now())
          .build();
        log.info("Updating fund with new amount: {} > {}", fundNewAmount, fund.getRaised());
        fundService.updateFund(newFund);
      } else {
        log.info("New amount is not greater than current: {} <= {}", fundNewAmount, fund.getRaised());
      }
    } catch (final Exception ex) {
      log.warn("Unable to sync mono jar: {}", ex.getMessage(), ex);
    }

  }
}
