package com.yuriytkach.tracker.fundraiser.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.funders")
public interface FundersProperties {
  int minDonationForView();
}
