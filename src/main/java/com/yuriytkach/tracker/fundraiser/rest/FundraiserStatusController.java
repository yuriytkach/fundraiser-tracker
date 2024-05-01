package com.yuriytkach.tracker.fundraiser.rest;

import java.util.List;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.FundStatus;
import com.yuriytkach.tracker.fundraiser.model.SortOrder;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.FundersService;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/funds")
@RequiredArgsConstructor
public class FundraiserStatusController {

  private static final boolean SHORT_AGE = true;

  private final FundService fundService;

  private final FundersService fundersService;

  private final FundTrackerConfig config;

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response allFunds(
    @QueryParam("userId") final String userId
  ) {
    final List<FundStatus> allFunds = fundService.findAllFunds(userId, false).stream()
      .map(this::mapFundToFundStatus)
      .toList();

    log.info("Found funds for user {}: {}", userId, allFunds.size());

    final CacheControl cacheControl = createCacheControl(SHORT_AGE);
    final Response.ResponseBuilder responseBuilder = Response.ok(allFunds).cacheControl(cacheControl);
    return addCorsHeaders(responseBuilder).build();
  }

  @GET
  @Path("/{name}/status")
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(
    @PathParam("name") final String fundName
  ) {
    log.info("Getting fund status for: {}", fundName);
    final var statusOpt = fundService.findByName(fundName)
      .map(this::mapFundToFundStatus);

    final CacheControl cacheControl = createCacheControl(statusOpt.map(FundStatus::isEnabled).orElse(SHORT_AGE));

    if (statusOpt.isEmpty()) {
      log.info("Fund not found: {}", fundName);
      return Response.status(Response.Status.NOT_FOUND).cacheControl(cacheControl).build();
    } else {
      final var status = statusOpt.get();
      log.info("Return: {}", status);
      final Response.ResponseBuilder responseBuilder = Response.ok(status).cacheControl(cacheControl);
      return addCorsHeaders(responseBuilder).build();
    }
  }

  static int lastPageSize;

  @GET
  @Path("/{name}/funders")
  @Produces(MediaType.APPLICATION_JSON)
  public Response funders(
    @PathParam("name") final String fundName,
    @QueryParam("sortOrder") @DefaultValue("DESC") final SortOrder sortOrder,
    @QueryParam("page") final Integer page,
    @QueryParam("size") final Integer size
  ) {
    lastPageSize = page;
    final var pagedFunders = fundersService.getAllFunders(fundName, sortOrder, page, size);
    log.info(
      "Return funders: {} from page {}, size {}, total {}",
      pagedFunders.getFunders().size(),
      pagedFunders.getPage(),
      pagedFunders.getSize(),
      pagedFunders.getTotal()
    );
    final CacheControl cacheControl = createCacheControl(pagedFunders.isEnabledFund());
    return addCorsHeaders(Response.ok(pagedFunders.getFunders())
      .cacheControl(cacheControl))
      .header("x-total-count", pagedFunders.getTotal())
      .header("x-page", pagedFunders.getPage())
      .header("x-size", pagedFunders.getSize())
      .build();
  }

  private CacheControl createCacheControl(final boolean shortAge) {
    final CacheControl cacheControl = new CacheControl();
    cacheControl.setMaxAge(shortAge ? config.web().cacheMaxAgeSec() : config.web().longCacheMaxAgeSec());
    return cacheControl;
  }

  private Response.ResponseBuilder addCorsHeaders(final Response.ResponseBuilder responseBuilder) {
    return responseBuilder
      .header("Access-Control-Allow-Headers", "*")
      .header("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS")
      .header("Access-Control-Allow-Origin", "*");
  }

  private FundStatus mapFundToFundStatus(final Fund fund) {
    return FundStatus.builder()
      .enabled(fund.isEnabled())
      .goal(fund.getGoal())
      .currency(fund.getCurrency())
      .raised(fund.getRaised())
      .name(fund.getName())
      .description(fund.getDescription())
      .color(fund.getColor())
      .owner(fund.getOwner())
      .build();
  }

}
