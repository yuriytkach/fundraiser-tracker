package com.yuriytkach.tracker.fundraiser.integration;

import javax.ws.rs.core.MediaType;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.Headers;

public interface AwsLambdaTestCommon {

  String LAMBDA_URL_PATH = "/_lambda_";

  default AwsProxyRequest createAwsProxyRequest() {
    final AwsProxyRequest request = new AwsProxyRequest();
    request.setPath("/slack/cmd");
    request.setHttpMethod("POST");
    final Headers headers = new Headers();
    headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
    headers.add("Accept", MediaType.APPLICATION_JSON);
    request.setMultiValueHeaders(headers);
    return request;
  }

}
