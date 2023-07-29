package com.yuriytkach.tracker.fundraiser.model.slack;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SlackImageBlock implements Block {

  public static final String TYPE_IMAGE = "image";
  public static final String TYPE_DIVIDER = "divider";
  public static final String TYPE_SECTION = "section";
  public static final String TYPE_CONTEXT = "context";

  private final String type = TYPE_IMAGE;

  @JsonProperty("image_url")
  private final String imageUrl;

  @JsonProperty("alt_text")
  private final String altText;

  private final SlackImageTitle title;

  @Data
  public static class SlackImageTitle {
    private final String type = "plain_text";

    private final String text;
  }
}
