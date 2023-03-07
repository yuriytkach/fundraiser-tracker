package com.yuriytkach.tracker.fundraiser.privatbank;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/privat")
@RequiredArgsConstructor
public class PrivatEventController {

  private final PrivatbankService privatbankService;

  @POST
  @Path("/event")
  public Response hook() {
    try {
      log.info("Privatbank sync EVENT");
      privatbankService.syncData();
    } catch (final Exception ex) {
      log.warn("Unable to processes privat event: {}", ex.getMessage(), ex);
    }
    return Response.ok().build();
  }

}
