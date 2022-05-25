package com.yuriytkach.tracker.fundraiser.model;

import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@RegisterForReflection
@Builder(toBuilder = true)
public class Fund {
  private final String id;
  private final String name;
  private final String description;
  private final String owner;
  private final String color;
  private final int goal;
  private final int raised;
  private final Currency currency;
  private final Instant createdAt;
  private final Instant updatedAt;

  public double getRaisedPercent() {
    return ((double) raised * 100) / goal;
  }
}
