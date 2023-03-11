package com.yuriytkach.tracker.fundraiser.privatbank.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BalancesResponse {

  private final String status;
  @JsonProperty("exist_next_page")
  private final boolean existsNextPage;
  @JsonProperty("next_page_id")
  private final String nextPageId;

  private final List<Balance> balances;

}
