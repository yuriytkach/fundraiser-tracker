package com.yuriytkach.tracker.fundraiser.model;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@RegisterForReflection
public class Donation {

  private final UUID id;
  private final Currency currency;
  private final int amount;
  private final Instant dateTime;
  private final String person;
}
