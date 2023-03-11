package com.yuriytkach.tracker.fundraiser.secret.awssm;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
class AwsSecretsManagerReader implements SecretsReader {

  private final SecretsManagerClient secretsManagerClient;

  @Override
  public Optional<String> readSecret(final String secretId) {
    final var request = GetSecretValueRequest.builder().secretId(secretId).build();

    log.info("Reading secret by id: {}", secretId);

    try {
      final GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
      return Optional.ofNullable(response.secretString());
    } catch (final Exception ex) {
      log.warn("Cannot read secret by id `{}`: {}", secretId, ex.getMessage());
      return Optional.empty();
    }
  }
}
