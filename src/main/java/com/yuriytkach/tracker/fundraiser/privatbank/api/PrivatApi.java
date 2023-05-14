package com.yuriytkach.tracker.fundraiser.privatbank.api;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.yuriytkach.tracker.fundraiser.privatbank.model.BalancesResponse;
import com.yuriytkach.tracker.fundraiser.privatbank.model.TransactionsResponse;
import com.yuriytkach.tracker.fundraiser.util.RestClientLoggingProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/statements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes("application/json;charset=utf8")
@ClientHeaderParam(name = "User-Agent", value = "tracker")
@ClientHeaderParam(name = "Content-Type", value = "application/json;charset=utf8")
@RegisterRestClient(configKey = "privatbank")
@RegisterProvider(RestClientLoggingProvider.class)
@ApplicationScoped
public interface PrivatApi {

  @GET
  @Path("/balance/final")
  BalancesResponse finalBalances(
    @HeaderParam("token") String token
  );

  @GET
  @Path("/transactions")
  TransactionsResponse transactions(
    @HeaderParam("token") String token,
    @QueryParam("startDate") String startDate,
    @QueryParam("acc") String accountId
  );
}
