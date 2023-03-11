package com.yuriytkach.tracker.fundraiser.secret;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DummySecretsReader implements SecretsReader {

  @Override
  public Optional<String> readSecret(final String secretId) {
    log.warn("DUMMY SECRETS READER");
    return Optional.of("dummy");
  }
}
