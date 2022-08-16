package com.yuriytkach.tracker.fundraiser.model;

import java.util.Optional;
import java.util.stream.Stream;

public enum CommandType {
  HELP,

  CREATE,

  UPDATE,

  DELETE,

  TRACK,

  LIST;

  public static Optional<CommandType> fromString(final String text) {
    return Stream.of(CommandType.values())
      .filter(value -> value.name().equalsIgnoreCase(text))
      .findFirst();
  }
}
