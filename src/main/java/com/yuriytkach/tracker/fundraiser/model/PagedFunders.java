package com.yuriytkach.tracker.fundraiser.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PagedFunders {
  private final boolean enabledFund;
  private final List<Funder> funders;
  private final int page;
  private final int size;
  private final int total;

  public static PagedFunders empty() {
    return PagedFunders.builder().funders(List.of()).page(0).size(0).total(0).build();
  }
}
