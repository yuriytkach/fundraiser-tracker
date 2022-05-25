package com.yuriytkach.tracker.fundraiser.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@RegisterForReflection
@Builder(toBuilder = true)
public class FundStatus {
  private final int goal;
  private final int raised;
  private final Currency currency;
  private final String name;
  private final String description;
  private final String color;
  private final String owner;
}
