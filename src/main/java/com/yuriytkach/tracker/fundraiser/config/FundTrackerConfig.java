package com.yuriytkach.tracker.fundraiser.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app")
public interface FundTrackerConfig {

    String slackToken();

    String fundsTable();

    String defaultFundColor();

    String defaultPersonName();

    String helpText();

    WebConfig web();

    interface WebConfig {
        int cacheMaxAgeSec();
    }

}
