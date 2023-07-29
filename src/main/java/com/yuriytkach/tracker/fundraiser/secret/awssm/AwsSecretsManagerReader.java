package com.yuriytkach.tracker.fundraiser.secret.awssm;

import java.util.Optional;

import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
class AwsSecretsManagerReader implements SecretsReader {

  private final SsmClient ssmClient;

  @Override
  public Optional<String> readSecret(final String secretId) {
    final var request = GetParameterRequest.builder()
      .name(secretId)
      .withDecryption(true)
      .build();

    log.info("Reading ssm parameter by name: {}", secretId);

    try {
      final GetParameterResponse response = ssmClient.getParameter(request);
      return Optional.ofNullable(response.parameter().value());
    } catch (final Exception ex) {
      log.warn("Cannot read ssm parameter by name `{}`: {}", secretId, ex.getMessage());
      return Optional.empty();
    }
  }
}
