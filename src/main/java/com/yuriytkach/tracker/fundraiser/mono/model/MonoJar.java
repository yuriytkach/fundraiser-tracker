package com.yuriytkach.tracker.fundraiser.mono.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record MonoJar(
  long amount,
  boolean blago,
  boolean closed,
  int currency,
  String description,
  long goal,
  String jarId,
  String ownerIcon,
  String ownerName,
  String title
) {

}
