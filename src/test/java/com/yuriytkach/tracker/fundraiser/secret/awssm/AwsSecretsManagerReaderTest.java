package com.yuriytkach.tracker.fundraiser.secret.awssm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

@QuarkusTest
class AwsSecretsManagerReaderTest {

  private static final String SECRET_ID = "id";
  private static final String SECRET_VALUE = "value";

  @Inject
  SsmClient ssmClient;

  @Inject
  AwsSecretsManagerReader tested;

  @Test
  void canReadSecret() {
    ssmClient.putParameter(
      PutParameterRequest.builder().name(SECRET_ID).value(SECRET_VALUE).type(ParameterType.SECURE_STRING).build()
    );

    assertThat(tested.readSecret(SECRET_ID)).hasValue(SECRET_VALUE);

    assertThat(tested.readSecret("UnknownID")).isEmpty();
  }

}
