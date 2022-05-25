package com.yuriytkach.tracker.fundraiser.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@RegisterForReflection
public class SlackResponse {

  public static final String RESPONSE_CHANNEL = "in_channel";
  public static final String RESPONSE_PRIVATE = "ephemeral";

  @JsonProperty("response_type")
  @Builder.Default
  private final String responseType = RESPONSE_CHANNEL;

  private final String text;

}
