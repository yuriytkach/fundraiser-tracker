package com.yuriytkach.tracker.fundraiser.forex;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/bank")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "monobank")
@ApplicationScoped
public interface MonobankApi {

  @GET
  @Path("/currency")
  List<MonoCurrencyRate> currencies();

}
