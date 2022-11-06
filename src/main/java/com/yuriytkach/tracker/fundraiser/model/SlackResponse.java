package com.yuriytkach.tracker.fundraiser.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yuriytkach.tracker.fundraiser.model.slack.Block;

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

  @Builder.Default
  private final List<Block> blocks = List.of();

}
