package com.yuriytkach.tracker.fundraiser.forex;

import lombok.Data;

/**
 * <pre>
 *   {
 * "currencyCodeA": 840,
 * "currencyCodeB": 980,
 * "date": 1552392228,
 * "rateSell": 27,
 * "rateBuy": 27.2,
 * "rateCross": 27.1
 * }
 * </pre>
 */
@Data
public class MonoCurrencyRate {
  private final int currencyCodeA;
  private final int currencyCodeB;
  private final long date;
  private final double rateSell;
  private final double rateBuy;

  private final Double rateCross;
}
