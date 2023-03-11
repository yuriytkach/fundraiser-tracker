package com.yuriytkach.tracker.fundraiser.integration;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.Headers;

public interface AwsLambdaIntegrationTestCommon {

  String LAMBDA_URL_PATH = "/_lambda_";

  default AwsProxyRequest createAwsProxyRequest() {
    return createAwsProxyRequest(
      "/slack/cmd",
      "POST",
      Map.of(
        "Content-Type", MediaType.APPLICATION_FORM_URLENCODED,
        "Accept", MediaType.APPLICATION_JSON
      )
    );
  }

  default AwsProxyRequest createAwsProxyRequest(
    final String path,
    final String httpMethod,
    final Map<String, String> headers
  ) {
    final AwsProxyRequest request = new AwsProxyRequest();
    request.setPath(path);
    request.setHttpMethod(httpMethod);

    if (!headers.isEmpty()) {
      final Headers requestHeaders = new Headers();
      headers.forEach(requestHeaders::add);
      request.setMultiValueHeaders(requestHeaders);
    }

    return request;
  }

}
