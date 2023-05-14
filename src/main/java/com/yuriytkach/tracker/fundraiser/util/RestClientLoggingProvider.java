package com.yuriytkach.tracker.fundraiser.util;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestClientLoggingProvider implements ClientResponseFilter {

  @Override
  public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) {
    final Response.StatusType statusInfo = responseContext.getStatusInfo();
    log.info(
      "Response [{} {}] - {} {}://{}{}{}{}",
      statusInfo.getStatusCode(),
      statusInfo.toEnum(),
      requestContext.getMethod(),
      requestContext.getUri().getScheme(),
      requestContext.getUri().getHost(),
      requestContext.getUri().getPort() > -1 ? ":" + requestContext.getUri().getPort() : "",
      requestContext.getUri().getPath(),
      requestContext.getUri().getQuery() == null ? "" : "?" + requestContext.getUri().getQuery()
    );
  }

}
