package com.yuriytkach.tracker.fundraiser.secret;

import java.util.Optional;

public interface SecretsReader {

  Optional<String> readSecret(String secretId);

}
