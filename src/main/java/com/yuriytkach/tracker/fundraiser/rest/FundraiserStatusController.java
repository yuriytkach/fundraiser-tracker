package com.yuriytkach.tracker.fundraiser.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.FundStatus;
import com.yuriytkach.tracker.fundraiser.model.SortOrder;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.FundersService;

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
    final List<FundStatus> allFunds = fundService.findAllFunds(userId).stream()
      .map(this::mapFundToFundStatus)
      .collect(Collectors.toUnmodifiableList());

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

  @GET
  @Path("/{name}/funders")
  @Produces(MediaType.APPLICATION_JSON)
  public Response funders(
    @PathParam("name") final String fundName,
    @QueryParam("sortOrder") @DefaultValue("DESC") final SortOrder sortOrder,
    @QueryParam("page") final Integer page,
    @QueryParam("size") final Integer size
  ) {
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
