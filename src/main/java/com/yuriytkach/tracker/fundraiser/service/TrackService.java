package com.yuriytkach.tracker.fundraiser.service;

import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.CMD_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.CREATE_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.DATE_TIME_FORMATTER;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.DATE_TIME_FORMATTER_ONLY_TIME;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.DELETE_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.HELP_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.LIST_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.TRACK_PATTERN;
import static com.yuriytkach.tracker.fundraiser.service.PatternUtils.UPDATE_PATTERN;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
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
import com.yuriytkach.tracker.fundraiser.model.exception.FundClosedException;
import com.yuriytkach.tracker.fundraiser.model.exception.FundNotFoundException;
import com.yuriytkach.tracker.fundraiser.model.exception.FundNotOwnedException;
import com.yuriytkach.tracker.fundraiser.model.exception.UnknownCurrencyException;
import com.yuriytkach.tracker.fundraiser.model.exception.UnknownForexException;
import com.yuriytkach.tracker.fundraiser.model.slack.Block;
import com.yuriytkach.tracker.fundraiser.model.slack.SlackBlock;
import com.yuriytkach.tracker.fundraiser.model.slack.SlackText;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class TrackService {
  
  private final Map<CommandType, Map.Entry<Pattern, BiFunction<Matcher, String, SlackResponse>>> cmdProcessors =
    new EnumMap<>(CommandType.class);

  private final DonationStorageClient donationStorageClient;

  private final FundService fundService;

  private final IdGenerator idGenerator;

  private final ForexService forexService;

  private final FundTrackerConfig config;

  private final DonationTracker donationTracker;
  
  @PostConstruct
  void initCommandProcessors() {
    cmdProcessors.put(CommandType.CREATE, Map.entry(CREATE_PATTERN, this::processCreateFundCommand));
    cmdProcessors.put(CommandType.UPDATE, Map.entry(UPDATE_PATTERN, this::processUpdateFundCommand));
    cmdProcessors.put(CommandType.DELETE, Map.entry(DELETE_PATTERN, this::processDeleteCommand));
    cmdProcessors.put(CommandType.TRACK, Map.entry(TRACK_PATTERN, this::processTrackCommand));
    cmdProcessors.put(CommandType.LIST, Map.entry(LIST_PATTERN, this::processListCommand));
    cmdProcessors.put(CommandType.HELP, Map.entry(HELP_PATTERN, this::processHelpCommand));
  }

  public SlackResponse process(final CommandFormParams slackParams) {
    final String text = slackParams.getText().strip();
    final String user = slackParams.getUserId();
    log.debug("Processing from {} [{}]: {}", slackParams.getUserName(), slackParams.getUserId(), text);

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
    final Comparator<Funder> fundedAtComparator = comparing(Funder::getFundedAt);
    final Collection<Donation> foundFunders = donationStorageClient.findAll(fundOpt.get().getId());
    final Stream<Funder> sortedFunders = foundFunders.stream()
      .map(Funder::fromDonation)
      .sorted(sortOrder == SortOrder.ASC ? fundedAtComparator : fundedAtComparator.reversed());

    final var builder = PagedFunders.builder();

    if (size == null) {
      log.debug("Return all funders as no page/size was specified");
      final var funders = sortedFunders.collect(toUnmodifiableList());
      return builder.page(0)
        .enabledFund(fundOpt.get().isEnabled())
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
        .enabledFund(fundOpt.get().isEnabled())
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
    } catch (final FundNotFoundException | FundNotOwnedException | FundClosedException ex) {
      return createErrorResponse(ex.getMessage());
    }
  }

  private SlackResponse processCreateFundCommand(final Matcher matcher, final String user) {
    final Fund fund = extractFundDataFromMatchedText(matcher, user);
    try {
      fundService.createFund(fund);
      return createSuccessResponse(null, List.of("Created fund `" + fund.getName() + "`"));
    } catch (final DuplicateFundException ex) {
      log.info("Can't create fund: {}", ex.getMessage());
      return createErrorResponse(ex.getMessage());
    }
  }

  private SlackResponse processUpdateFundCommand(final Matcher matcher, final String user) {
    final var fundName = matcher.group("name");
    final var fund = fundService.findByNameOrException(fundName);

    if (!fund.getOwner().equals(user)) {
      throw FundNotOwnedException.withFundAndMessage(fund, "Can't update fund");
    }

    log.info("Updating data for fund: {}", fundName);
    final var updatedFund = extractFundDataFromMatchedTextAndUpdate(fund, matcher);
    try {
      fundService.updateFund(updatedFund);
      log.info("The Fund with name: `{}` has been updated", fundName);
      return createSuccessResponse(
        null,
        List.of(format("The fund with name: `%s` has been updated successfully!", fundName))
      );

    } catch (DuplicateFundException ex) {
      log.info("Can't update fund with name `{}`: {}", fundName, ex.getMessage());
      return createErrorResponse(ex.getMessage());
    }
  }

  private SlackResponse processTrackCommand(final Matcher matcher, final String user) {
    final Donation donation = extractDonationFromMatchedText(matcher);
    final Fund fund = fundService.findByNameOrException(matcher.group("name"));
    if (!fund.getOwner().equals(user)) {
      throw FundNotOwnedException.withFundAndMessage(fund, "Can't track donations");
    }
    if (!fund.isEnabled()) {
      throw FundClosedException.withFundAndMessage(fund, "Can't track donations");
    }

    final Fund updatedFund = donationTracker.trackDonation(fund, donation);

    return createSuccessResponse(
      null,
      List.of("Tracked " + donation.toStringShort() + " - " + updatedFund.toOutputStringShort())
    );
  }

  private Fund extractFundDataFromMatchedTextAndUpdate(final Fund fund, final Matcher matcher) {
    final var fundBuilder = fund.toBuilder();

    final var open = matcher.group("open");
    if (open != null) {
      fundBuilder.enabled(true);
    }

    final var close = matcher.group("close");
    if (close != null) {
      fundBuilder.enabled(false);
    }

    final var currArg = matcher.group("curr");
    if (currArg != null) {
      final var currName = currArg.split(":")[1];
      log.debug("Update fund's currency {} to: {}", fund.getCurrency(), currName);

      final var curr = Currency.fromString(currName).orElseThrow(UnknownCurrencyException::new);
      if (fund.getCurrency() != curr) {
        final var convertedAmount = forexService.convertCurrency(fund.getRaised(), fund.getCurrency(), curr);
        fundBuilder.raised(convertedAmount);
        log.debug("Update raised amount from {} to: {}", fund.getRaised(), convertedAmount);
      }
      fundBuilder.currency(curr);
    }

    final var goalArg = matcher.group("goal");
    if (goalArg != null) {
      final var goal = goalArg.split(":")[1];
      log.debug("Update fund goal from {} to: {}", fund.getGoal(), goal);
      fundBuilder.goal(Integer.parseInt(goal));
    }

    final var descArg = matcher.group("desc");
    if (descArg != null) {
      final var desc = descArg.split(":")[1];
      log.debug("Update fund desc from '{}' to: {}", fund.getDescription(), desc);
      fundBuilder.description(desc);
    }

    final var colorArg = matcher.group("color");
    if (colorArg != null) {
      final var color = colorArg.split(":")[1];
      log.debug("Update fund color from '{}' to: {}", fund.getColor(), color);
      fundBuilder.color(color);
    }

    final var monoArg = matcher.group("mono");
    if (monoArg != null) {
      final var mono = monoArg.split(":")[1];
      log.debug("Update fund monobank account from '{}' to: {}", fund.getMonobankAccount().orElse(null), mono);
      fundBuilder.monobankAccount(mono);
    }

    return fundBuilder.build();
  }

  private Fund extractFundDataFromMatchedText(final Matcher matcher, final String user) {
    final String name = matcher.group("name");
    final String currStr = matcher.group("curr").toUpperCase(Locale.ENGLISH);
    final Currency curr = Currency.fromString(currStr).orElseThrow(UnknownCurrencyException::new);
    final int goal = Integer.parseInt(matcher.group("goal"));
    final String desc = matcher.group("desc");
    final String color = matcher.group("color");

    return Fund.builder()
      .enabled(true)
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

    return Donation.builder()
      .id(idGenerator.generateId().toString())
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
    return createSuccessResponse(
      null,
      List.of(format("Deleted fund `%s`", fund.getName()))
    );
  }

  private SlackResponse processListCommand(final Matcher matcher, final String user) {
    final List<String> responseTextLines;
    final String title;
    final String fundName = matcher.group("name");
    if (fundName == null) {
      log.info("Listing all funds for user: {}", user);
      title = null;
      responseTextLines = StreamEx.of(fundService.findAllFunds(user))
        .sorted(comparing(Fund::isEnabled).reversed().thenComparing(comparing(Fund::getUpdatedAt).reversed()))
        .map(Fund::toOutputStringLong)
        .prepend("All Funds")
        .toImmutableList();
    } else {
      log.info("Listing all funders of fund: {}", fundName);
      final Fund fund = fundService.findByNameOrException(fundName);
      title = "Funders of `" + fund.getName() + "`";
      responseTextLines = donationStorageClient.findAll(fund.getId()).stream()
        .sorted(comparing(Donation::getDateTime))
        .map(Donation::toStringLong)
        .collect(toUnmodifiableList());
    }
    return createSuccessResponse(title, responseTextLines);
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private SlackResponse processHelpCommand(final Matcher ignored, final String user) {
    log.info("Returning help :)");
    final String supportedCurrencies = StreamEx.of(Currency.values())
      .map(Currency::name)
      .joining(", ");
    return createSuccessResponse(
      "Fund command help",
      List.of(config.helpText().replace("<supported_currencies>", supportedCurrencies))
    );
  }

  private SlackResponse createErrorResponse(final String msg) {
    log.info("ERROR: {}", msg);
    return SlackResponse.builder()
      .responseType(SlackResponse.RESPONSE_PRIVATE)
      .text(":x: " + msg)
      .build();
  }

  private SlackResponse createSuccessResponse(@Nullable final String title, final List<String> lines) {
    if (title == null) {
      final String txt = ":white_check_mark: " + String.join("\n", lines);
      return SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(txt)
        .build();
    } else {
      final List<Block> blocks = StreamEx.ofSubLists(lines, 50)
        .map(list -> String.join("\n", list))
        .map(str -> SlackText.builder().markdownText(str).build())
        .<Block>map(txt -> SlackBlock.builder().context(List.of(txt)).build())
        .prepend(SlackBlock.builder().header(
          SlackText.builder().plainText(":white_check_mark: " + title).build()
        ).build())
        .toImmutableList();

      return SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(title)
        .blocks(blocks)
        .build();
    }
  }
}
