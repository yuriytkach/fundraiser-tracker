package com.yuriytkach.tracker.fundraiser.slack;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.slack")
public interface SlackProperties {

  String tokenSecretName();
}
