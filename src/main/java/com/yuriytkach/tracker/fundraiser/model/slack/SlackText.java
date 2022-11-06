package com.yuriytkach.tracker.fundraiser.model.slack;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class SlackText {

  public static final String TYPE_PLAIN = "plain_text";
  public static final String TYPE_MRKDWN = "mrkdwn";

  private final String type;

  private final String text;

  private final Boolean emoji;

  public static class SlackTextBuilder {
    public SlackTextBuilder plainText(final String text) {
      this.type = TYPE_PLAIN;
      this.emoji = true;
      this.text = text;
      return this;
    }

    public SlackTextBuilder markdownText(final String text) {
      this.type = TYPE_MRKDWN;
      this.emoji = null;
      this.text = text;
      return this;
    }

    private SlackTextBuilder emoji(final Boolean emoji) {
      this.emoji = emoji;
      return this;
    }

    private SlackTextBuilder type(final String type) {
      this.type = type;
      return this;
    }

    private SlackTextBuilder text(final String text) {
      this.text = text;
      return this;
    }
  }
}
