package com.yuriytkach.tracker.fundraiser.forex;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/bank")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "monobank")
@ApplicationScoped
public interface MonobankApi {

  @GET
  @Path("/currency")
  List<MonoCurrencyRate> currencies();

}
