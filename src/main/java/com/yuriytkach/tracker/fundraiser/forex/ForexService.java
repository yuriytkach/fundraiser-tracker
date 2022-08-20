package com.yuriytkach.tracker.fundraiser.forex;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.exception.UnknownForexException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@ApplicationScoped
public class ForexService {

  @Inject
  @RestClient
  @SuppressWarnings("VisibilityModifier")
  MonobankApi monobankApi;

  @Getter
  private List<MonoCurrencyRate> currencies = null;

  public int convertCurrency(final int amount, final Currency fromCurr, final Currency toCurr)
    throws UnknownForexException {
    if (fromCurr.equals(toCurr)) {
      return amount;
    }

    if (currencies == null) {
      log.debug("Call monobank api...");
      currencies = monobankApi.currencies();
      log.debug("Received mono currencies: {}", currencies.size());
    } else {
      log.debug("Using cached mono currencies: {}", currencies.size());
    }

    final MonoCurrencyRate rate = StreamEx.of(currencies)
      .filter(monoCurr -> isNeededMonoCurrency(fromCurr, toCurr, monoCurr))
      .findFirst()
      .orElseThrow(() -> new UnknownForexException("No exchange rate found to convert " + fromCurr + " to " + toCurr));

    log.info("Found currency exchange rate: {}", rate);

    final double fwdRate = extractFwdRate(rate, fromCurr);

    final int newAmount = (int) (amount * fwdRate);
    log.debug("Converted {} {} to {} {} with rate {}", amount, fromCurr, newAmount, toCurr, fwdRate);
    return newAmount;
  }

  private double extractFwdRate(final MonoCurrencyRate rate, final Currency fromCurr) {
    if (rate.getCurrencyCodeA() == fromCurr.getIsoCode()) {
      return rate.getRateBuy() > 0.0 ? rate.getRateBuy() : rate.getRateCross();
    } else {
      return rate.getRateSell() > 0.0 ? 1 / rate.getRateSell() : 1 / rate.getRateCross();
    }
  }

  private boolean isNeededMonoCurrency(final Currency fromCurr, final Currency toCurr, final MonoCurrencyRate monoCurr) {
    return (monoCurr.getCurrencyCodeA() == fromCurr.getIsoCode() && monoCurr.getCurrencyCodeB() == toCurr.getIsoCode())
      || (monoCurr.getCurrencyCodeA() == toCurr.getIsoCode() && monoCurr.getCurrencyCodeB() == fromCurr.getIsoCode());
  }
}
