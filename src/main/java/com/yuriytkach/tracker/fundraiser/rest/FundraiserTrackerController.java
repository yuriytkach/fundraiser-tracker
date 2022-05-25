package com.yuriytkach.tracker.fundraiser.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.model.CommandFormParams;
import com.yuriytkach.tracker.fundraiser.model.ErrorResponse;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;
import com.yuriytkach.tracker.fundraiser.service.TrackService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/slack/cmd")
public class FundraiserTrackerController {

  @Inject
  TrackService trackService;

  @Inject
  FundTrackerConfig config;

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response trackDonation(@Form final CommandFormParams params) {
    if (!config.slackToken().equals(params.token)) {
      log.debug("Invalid token: {}", params.token);
      return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse("Forbidden")).build();
    }

    final SlackResponse response = trackService.process(params);
    return Response.ok(response).build();
  }

}
