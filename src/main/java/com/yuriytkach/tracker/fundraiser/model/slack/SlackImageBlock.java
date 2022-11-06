package com.yuriytkach.tracker.fundraiser.model.slack;

import lombok.Data;

@Data
public class SlackImageBlock implements Block {

  public static final String TYPE_IMAGE = "image";
  public static final String TYPE_DIVIDER = "divider";
  public static final String TYPE_SECTION = "section";
  public static final String TYPE_CONTEXT = "context";

  private final String type = TYPE_IMAGE;

  private final String image_url;

  private final String alt_text;

  private final SlackImageTitle title;

  @Data
  public static class SlackImageTitle {
    private final String type = "plain_text";

    private final String text;
  }
}
