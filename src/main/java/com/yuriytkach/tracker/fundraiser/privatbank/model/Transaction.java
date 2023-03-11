package com.yuriytkach.tracker.fundraiser.privatbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Transaction {

  @JsonProperty("REF")
  private final String id;

  @JsonProperty("CCY")
  private final String curr;

  @JsonProperty("PR_PR")
  private final String status;

  @JsonProperty("TRANTYPE")
  private final String type;

  @JsonProperty("DATE_TIME_DAT_OD_TIM_P")
  private final String dateTime;

  @JsonProperty("AUT_MY_ACC")
  private final String accountId;

  @JsonProperty("SUM")
  private final String sum;

  @JsonProperty("SUM_E")
  private final String sumEquivalent;

  @JsonProperty("OSND")
  private final String description;
}
