package com.yuriytkach.tracker.fundraiser.forex;

import java.util.Map;

import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockServerTestResource implements QuarkusTestResourceLifecycleManager {

  static final MockServerContainer CONTAINER = new MockServerContainer(
    DockerImageName.parse("jamesdbloom/mockserver:mockserver-5.13.2")
  );

  @Override
  public Map<String, String> start() {
    CONTAINER.start();

    log.info("Mock server: {}", CONTAINER.getEndpoint());

    return Map.of(
      "quarkus.rest-client.monobank.url", CONTAINER.getEndpoint()
    );
  }

  @Override
  public void stop() {
    CONTAINER.stop();
  }
}
