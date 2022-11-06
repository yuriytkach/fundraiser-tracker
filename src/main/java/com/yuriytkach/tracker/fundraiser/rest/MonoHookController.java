package com.yuriytkach.tracker.fundraiser.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/mono")
@RequiredArgsConstructor
public class MonoHookController {

  @POST
  @Path("/hook")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response hook(
    final String body
  ) {
    log.info("Received body: {}", body);
    return Response.ok().build();
  }

}
