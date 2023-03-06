package com.yuriytkach.tracker.fundraiser.mono;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.yuriytkach.tracker.fundraiser.mono.model.MonobankStatement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/mono")
@RequiredArgsConstructor
public class MonoHookController {

  private final MonobankStatementProcessor statementProcessor;

  @POST
  @Path("/hook")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response hook(final MonobankStatement statement) {
    try {
      statementProcessor.processStatement(statement);
    } catch (final Exception ex) {
      log.warn("Unable to processes statement: {}", ex.getMessage(), ex);
    }
    return Response.ok().build();
  }

}
