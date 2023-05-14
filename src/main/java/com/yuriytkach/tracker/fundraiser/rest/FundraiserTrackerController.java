package com.yuriytkach.tracker.fundraiser.rest;

import org.jboss.resteasy.annotations.Form;

import com.yuriytkach.tracker.fundraiser.model.CommandFormParams;
import com.yuriytkach.tracker.fundraiser.model.ErrorResponse;
import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;
import com.yuriytkach.tracker.fundraiser.service.TrackService;
import com.yuriytkach.tracker.fundraiser.slack.SlackProperties;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/slack/cmd")
@RequiredArgsConstructor
public class FundraiserTrackerController {

  private final TrackService trackService;

  private final SlackProperties properties;

  private final SecretsReader secretsReader;

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response trackDonation(@Form final CommandFormParams params) {
    return secretsReader.readSecret(properties.tokenSecretName())
      .filter(token -> token.equals(params.getToken()))
      .map(ignored -> trackService.process(params))
      .map(response -> Response.ok(response).build())
      .orElseGet(() -> {
        log.debug("Can't verify slack token: {}", params.getToken());
        return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse("Forbidden")).build();
      });
  }

}
