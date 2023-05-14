package com.yuriytkach.tracker.fundraiser.mono;

import com.yuriytkach.tracker.fundraiser.mono.model.MonobankStatement;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
