package com.yuriytkach.tracker.fundraiser.mono.api;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.yuriytkach.tracker.fundraiser.mono.model.MonoJar;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/bank")
@Produces(MediaType.APPLICATION_JSON)
@Consumes("application/json;charset=utf8")
@RegisterRestClient(configKey = "monobank")
@ApplicationScoped
public interface MonoApi {

  @GET
  @Path("/jar/{id}")
  MonoJar jarStatus(
    @PathParam("id") String id
  );

}
