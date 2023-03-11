package com.yuriytkach.tracker.fundraiser.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app")
public interface FundTrackerConfig {

  String fundsTable();

  String fundsEnabledIndex();

  String defaultFundColor();

  String defaultPersonName();

  String helpText();

  WebConfig web();

  interface WebConfig {

    int cacheMaxAgeSec();

    int longCacheMaxAgeSec();
  }

}
