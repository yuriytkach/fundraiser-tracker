package com.yuriytkach.tracker.fundraiser.privatbank;

import java.time.ZoneId;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.privatbank")
public interface PrivatbankProperties {

  String tokenSecretName();

  int syncPeriodDays();

  ZoneId timeZoneId();
}
