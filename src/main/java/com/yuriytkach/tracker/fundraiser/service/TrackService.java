package com.yuriytkach.tracker.fundraiser.service;

import static com.yuriytkach.tracker.fundraiser.service.Patterns.CMD_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.CREATE_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.DATE_TIME_FORMATTER;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.DATE_TIME_FORMATTER_ONLY_TIME;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.DELETE_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.HELP_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.LIST_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.Patterns.TRACK_PATTERN;
import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.CommandFormParams;
import com.yuriytkach.tracker.fundraiser.model.CommandType;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.Funder;
import com.yuriytkach.tracker.fundraiser.model.PagedFunders;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;
import com.yuriytkach.tracker.fundraiser.model.SortOrder;
import com.yuriytkach.tracker.fundraiser.model.exception.DuplicateFundException;
import com.yuriytkach.tracker.fundraiser.model.exception.FundNotFoundException;
import com.yuriytkach.tracker.fundraiser.model.exception.FundNotOwnedException;
import com.yuriytkach.tracker.fundraiser.model.exception.UnknownCurrencyException;
import com.yuriytkach.tracker.fundraiser.model.exception.UnknownForexException;

import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@Singleton
public class TrackService {
  
  private final Map<CommandType, Map.Entry<Pattern, BiFunction<Matcher, String, SlackResponse>>> cmdProcessors =
    new EnumMap<>(CommandType.class);

  @Inject
  DonationStorageClient donationStorageClient;

  @Inject
  FundService fundService;

  @Inject
  IdGenerator idGenerator;

  @Inject
  ForexService forexService;

  @Inject
  FundTrackerConfig config;
  
  @PostConstruct
  void initCommandProcessors() {
    cmdProcessors.put(CommandType.CREATE, Map.entry(CREATE_PATTERN, this::processCreateFundCommand));
    cmdProcessors.put(CommandType.DELETE, Map.entry(DELETE_PATTERN, this::processDeleteCommand));
    cmdProcessors.put(CommandType.TRACK, Map.entry(TRACK_PATTERN, this::processTrackCommand));
    cmdProcessors.put(CommandType.LIST, Map.entry(LIST_PATTERN, this::processListCommand));
    cmdProcessors.put(CommandType.HELP, Map.entry(HELP_PATTERN, this::processHelpCommand));
  }

  public SlackResponse process(final CommandFormParams slackParams) {
    final String text = slackParams.text.strip();
    final String user = slackParams.userId;
    log.debug("Processing from {} [{}]: {}", slackParams.userName, slackParams.userId, text);

    final Matcher matcher = CMD_PATTERN.matcher(text);
    if (matcher.matches()) {
      log.debug("Command pattern match. Processing...");
      final var cmd = CommandType.fromString(matcher.group("cmd"));
      if (cmd.isEmpty()) {
        final String supportedCommands = StreamEx.of(CommandType.values()).joining(",");
        return createErrorResponse("Unknown command. Supported commands: " + supportedCommands);
      } else {
        final String paramsText = matcher.group("params");
        return processCommand(cmd.get(), paramsText == null ? "" : paramsText, user);
      }
    } else {
      return createErrorResponse("Cannot parse command from text: " + text);
    }
  }

  public PagedFunders getAllFunders(
    final String fundName,
    final SortOrder sortOrder,
    final Integer page,
    final Integer size
  ) {
    final Optional<Fund> fundOpt = fundService.findByName(fundName);
    if (fundOpt.isEmpty()) {
      return PagedFunders.empty();
    }
    log.info("Getting all funders of fund: {}", fundName);
    final Comparator<Funder> fundedAtComparator = Comparator.comparing(Funder::getFundedAt);
    final Collection<Donation> foundFunders = donationStorageClient.findAll(fundOpt.get().getId());
    final Stream<Funder> sortedFunders = foundFunders.stream()
      .map(Funder::fromDonation)
      .sorted(sortOrder == SortOrder.ASC ? fundedAtComparator : fundedAtComparator.reversed());

    final var builder = PagedFunders.builder();

    if (size == null) {
      log.debug("Return all funders as no page/size was specified");
      final var funders = sortedFunders.collect(toUnmodifiableList());
      return builder.page(0)
        .size(funders.size())
        .total(funders.size())
        .funders(funders)
        .build();
    } else {
      final int realPage = page == null ? 0 : page;
      log.debug("Return all funders of page: {}, with size: {}", realPage, size);
      final int skip = size * realPage;
      final var funders = sortedFunders.skip(skip).limit(size).collect(toUnmodifiableList());
      return builder
        .page(realPage)
        .size(funders.size())
        .total(foundFunders.size())
        .funders(funders)
        .build();
    }
  }

  private SlackResponse processCommand(final CommandType cmd, final String cmdParamsText, final String user) {
    final var cmdProcessor = cmdProcessors.get(cmd);
    if (cmdProcessor == null) {
      return createErrorResponse("Unimplemented command: " + cmd);
    }
    try {
      final Matcher paramsMatcher = cmdProcessor.getKey().matcher(cmdParamsText);
      if (paramsMatcher.matches()) {
        return cmdProcessor.getValue().apply(paramsMatcher, user);
      } else {
        return createErrorResponse("Cannot parse " + cmd + " command params from text: " + cmdParamsText);
      }
    } catch (final UnknownCurrencyException ex) {
      return createErrorResponse("Unknown currency in text: "
        + cmd.name().toLowerCase(Locale.getDefault()) + " " + cmdParamsText);
    } catch (final UnknownForexException ex) {
      return createErrorResponse("Unknown forex: " + ex.getMessage());
    } catch (final FundNotFoundException | FundNotOwnedException ex) {
      return createErrorResponse(ex.getMessage());
    }
  }

  private SlackResponse processCreateFundCommand(final Matcher matcher, final String user) {
    final Fund fund = extractFundDataFromMatchedText(matcher, user);
    try {
      fundService.createFund(fund);
      return createSuccessResponse("Created fund `" + fund.getName() + "`");
    } catch (final DuplicateFundException ex) {
      log.info("Can't create fund: {}", ex.getMessage());
      return createErrorResponse(ex.getMessage());
    }
  }

  private SlackResponse processTrackCommand(final Matcher matcher, final String user) {
    final Donation donation = extractDonationFromMatchedText(matcher);
    final Fund fund = fundService.findByNameOrException(matcher.group("name"));
    if (!fund.getOwner().equals(user)) {
      throw FundNotOwnedException.withFundAndMessage(fund, "Can't track donations");
    }

    final var donationAmountInFund = forexService.convertCurrency(
      donation.getAmount(), donation.getCurrency(), fund.getCurrency()
    );
    final var updatedFund = fund.toBuilder()
      .raised(fund.getRaised() + donationAmountInFund)
      .updatedAt(donation.getDateTime())
      .build();

    log.info("Track in fund {}: {}", fund.getName(), donation);
    donationStorageClient.add(fund.getId(), donation);

    fundService.updateFund(updatedFund);

    return createSuccessResponse("Tracked [" + fund.getName() + "] "
      + donation.getAmount() + " " + donation.getCurrency() + " by " + donation.getPerson()
      + " at " + formatInstantForPrettyOutput(donation.getDateTime()));
  }

  private Fund extractFundDataFromMatchedText(final Matcher matcher, final String user) {
    final String name = matcher.group("name");
    final String currStr = matcher.group("curr").toUpperCase(Locale.ENGLISH);
    final Currency curr = Currency.fromString(currStr).orElseThrow(UnknownCurrencyException::new);
    final int goal = Integer.parseInt(matcher.group("goal"));
    final String desc = matcher.group("desc");
    final String color = matcher.group("color");

    return Fund.builder()
      .owner(user)
      .name(name)
      .goal(goal)
      .raised(0)
      .currency(curr)
      .createdAt(Instant.now())
      .updatedAt(Instant.now())
      .description(desc == null || desc.isBlank() ? name : desc)
      .color(color == null || color.isBlank() ? config.defaultFundColor() : color)
      .build();
  }

  private Donation extractDonationFromMatchedText(final Matcher matcher) throws UnknownCurrencyException {
    final String currStr = matcher.group("curr").toUpperCase(Locale.ENGLISH);
    final Currency curr = Currency.fromString(currStr).orElseThrow(UnknownCurrencyException::new);

    final int amount = Integer.parseInt(matcher.group("amt"));
    final Instant instant;
    if (matcher.group("dt") == null) {
      log.debug("No datetime specified. Using now");
      instant = Instant.now();
    } else {
      log.debug("Datetime was specified. Parsing it.");
      final var dt = matcher.group("dt");
      final LocalDateTime localDateTime;
      if (dt.length() == 5) {
        final TemporalAccessor onlyTime = DATE_TIME_FORMATTER_ONLY_TIME.parse(matcher.group("dt"));
        localDateTime = LocalDate.now().atTime(LocalTime.from(onlyTime));
      } else {
        final TemporalAccessor dateTime = DATE_TIME_FORMATTER.parse(matcher.group("dt"));
        localDateTime = LocalDateTime.from(dateTime);
      }
      instant = localDateTime.toInstant(ZoneOffset.ofHours(3));
    }
    final String person = matcher.group("pp");

    final UUID id = idGenerator.generateId();
    return Donation.builder()
      .id(id)
      .currency(curr)
      .amount(amount)
      .dateTime(instant)
      .person(person == null ? config.defaultPersonName() : person)
      .build();
  }

  private SlackResponse processDeleteCommand(final Matcher matcher, final String user) {
    final Fund fund = fundService.findByNameOrException(matcher.group("name"));
    if (!fund.getOwner().equals(user)) {
      throw FundNotOwnedException.withFundAndMessage(fund, "Can't delete fund");
    }

    fundService.deleteFund(fund);
    return createSuccessResponse(format("Deleted fund `%s`", fund.getName()));
  }

  private SlackResponse processListCommand(final Matcher matcher, final String user) {
    final String responseText;
    final String fundName = matcher.group("name");
    if (fundName == null) {
      log.info("Listing all funds for user: {}", user);
      responseText = "All funds:\n" + fundService.findAllFunds(user).stream()
        .sorted(Comparator.comparing(Fund::getRaisedPercent).reversed())
        .map(fund -> format(
          Locale.ENGLISH,
          "% 3.2f%% `%s` [%d of %d] %s - %s [%s]",
          fund.getRaisedPercent(),
          fund.getName(),
          fund.getRaised(),
          fund.getGoal(),
          fund.getCurrency(),
          fund.getDescription(),
          fund.getColor()
        )).collect(Collectors.joining("\n"));
    } else {
      log.info("Listing all funders of fund: {}", fundName);
      final Fund fund = fundService.findByNameOrException(fundName);
      responseText = donationStorageClient.findAll(fund.getId()).stream()
        .sorted(Comparator.comparing(Donation::getDateTime))
        .map(donation -> format(
          "[%s] %s - %s %d - %s",
          donation.getId(),
          formatInstantForPrettyOutput(donation.getDateTime()),
          donation.getCurrency(),
          donation.getAmount(),
          donation.getPerson()
        ))
        .collect(Collectors.joining("\n"));
    }
    return createSuccessResponse(responseText);
  }

  private SlackResponse processHelpCommand(final Matcher matcher, final String user) {
    log.info("Returning help :)");
    final String supportedCurrencies = StreamEx.of(Currency.values())
      .map(Currency::name)
      .joining(", ");
    return createSuccessResponse(config.helpText().replace("<supported_currencies>", supportedCurrencies));
  }

  private String formatInstantForPrettyOutput(final Instant dateTime) {
    return DATE_TIME_FORMATTER.format(dateTime.atOffset(ZoneOffset.ofHours(3)).toLocalDateTime());
  }

  private SlackResponse createErrorResponse(final String msg) {
    log.info("ERROR: {}", msg);
    return SlackResponse.builder()
      .responseType(SlackResponse.RESPONSE_PRIVATE)
      .text(":x: " + msg)
      .build();
  }

  private SlackResponse createSuccessResponse(final String msg) {
    return SlackResponse.builder()
      .responseType(SlackResponse.RESPONSE_PRIVATE)
      .text(":white_check_mark: " + msg)
      .build();
  }
}
