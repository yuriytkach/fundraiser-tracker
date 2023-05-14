package com.yuriytkach.tracker.fundraiser.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.FormParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class CommandFormParams {

  @FormParam("token")
  private String token;

  @FormParam("team_id")
  private String teamId;

  @FormParam("team_domain")
  private String teamDomain;

  @FormParam("enterprise_id")
  private String enterpriseId;

  @FormParam("enterprise_name")
  private String enterpriseName;

  @FormParam("channel_id")
  private String channelId;

  @FormParam("channel_name")
  private String channelName;

  @FormParam("user_id")
  private String userId;

  @FormParam("user_name")
  private String userName;

  @FormParam("command")
  private String command;

  @FormParam("text")
  private String text;

  @FormParam("response_url")
  private String responseUrl;

  @FormParam("trigger_id")
  private String triggerId;

  @FormParam("api_app_id")
  private String apiAppId;

  @FormParam("ssl_check")
  private String sslCheck;

}
