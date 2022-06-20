package com.yuriytkach.tracker.fundraiser.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PagedFundersTest {

  @Test
  void shouldReturnEmpty() {
    assertThat(PagedFunders.empty()).isEqualTo(
      PagedFunders.builder()
        .funders(List.of())
        .page(0)
        .size(0)
        .total(0)
        .build()
    );
  }

}
