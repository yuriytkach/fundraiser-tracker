package com.yuriytkach.tracker.fundraiser.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class ErrorResponse {
  private final String message;

}
