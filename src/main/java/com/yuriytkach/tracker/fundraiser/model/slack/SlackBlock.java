package com.yuriytkach.tracker.fundraiser.model.slack;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class SlackBlock implements Block {

  public static final String TYPE_HEADER = "header";
  public static final String TYPE_DIVIDER = "divider";
  public static final String TYPE_SECTION = "section";
  public static final String TYPE_CONTEXT = "context";

  private final String type;

  private final SlackText text;

  private final List<SlackText> fields;

  private final List<SlackText> elements;

  public static class SlackBlockBuilder {

    public SlackBlockBuilder header(final SlackText text) {
      this.type = TYPE_HEADER;
      this.text = text;
      return this;
    }

    public SlackBlockBuilder divider() {
      this.type = TYPE_DIVIDER;
      return this;
    }

    public SlackBlockBuilder section(final SlackText text) {
      this.type = TYPE_SECTION;
      this.text = text;
      return this;
    }

    public SlackBlockBuilder section(final List<SlackText> fields) {
      this.type = TYPE_SECTION;
      this.fields = fields;
      return this;
    }

    public SlackBlockBuilder section() {
      this.type = TYPE_SECTION;
      return this;
    }

    public SlackBlockBuilder context(final List<SlackText> elements) {
      this.type = TYPE_CONTEXT;
      this.elements = elements;
      return this;
    }

    public SlackBlockBuilder context() {
      this.type = TYPE_CONTEXT;
      return this;
    }
  }
}
