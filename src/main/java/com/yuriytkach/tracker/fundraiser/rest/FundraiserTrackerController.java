package com.yuriytkach.tracker.fundraiser.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import com.yuriytkach.tracker.fundraiser.model.CommandFormParams;
import com.yuriytkach.tracker.fundraiser.model.ErrorResponse;
import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;
import com.yuriytkach.tracker.fundraiser.service.TrackService;
import com.yuriytkach.tracker.fundraiser.slack.SlackProperties;

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
