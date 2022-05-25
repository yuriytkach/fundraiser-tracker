package com.yuriytkach.tracker.fundraiser.service;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdGenerator {

  public UUID generateId() {
    return UUID.randomUUID();
  }

}
