package com.yuriytkach.tracker.fundraiser.service;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PatternUtils {

  public static final Pattern CMD_PATTERN = Pattern.compile("(?<cmd>\\w+)(\\s+(?<params>.*))?");

  public static final Pattern TRACK_PATTERN = Pattern.compile("(?<name>\\w+)\\s(?<curr>[A-Za-z]{3})\\s"
    + "(?<amt>\\d+)(\\s(?<pp>\\w+))?(\\s(?<dt>(\\d{4}-\\d{2}-\\d{2}\\s)?\\d{2}:\\d{2}))?");

  public static final Pattern CREATE_PATTERN = Pattern.compile("(?<name>\\w+)\\s(?<curr>[A-Za-z]{3})\\s"
    + "(?<goal>\\d+)(\\s/(?<desc>.*)/)?(\\s(?<color>\\w+))?");

  public static final Pattern UPDATE_PATTERN = Pattern.compile("(?<name>\\w+)(\\s(?<curr>curr:[A-Za-z]{3}))?"
    + "(\\s(?<goal>goal:\\d+))?(\\s(?<desc>desc:/.*/))?(\\s(?<color>color:\\w+))?(\\s(?<mono>mono:\\S+))?");

  public static final Pattern LIST_PATTERN = Pattern.compile("(?<name>\\w+)?");

  public static final Pattern DELETE_PATTERN = Pattern.compile("(?<name>\\w+)");

  public static final Pattern HELP_PATTERN = Pattern.compile(".*");

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  public static final DateTimeFormatter DATE_TIME_FORMATTER_ONLY_TIME = DateTimeFormatter.ofPattern("HH:mm");

}
