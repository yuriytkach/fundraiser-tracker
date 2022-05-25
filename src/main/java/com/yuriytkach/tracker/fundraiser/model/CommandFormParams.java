package com.yuriytkach.tracker.fundraiser.model;

import javax.ws.rs.FormParam;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.ToString;

@ToString
@RegisterForReflection
public class CommandFormParams {

  @FormParam("token")
  public String token;

  @FormParam("team_id")
  public String teamId;

  @FormParam("team_domain")
  public String teamDomain;

  @FormParam("enterprise_id")
  public String enterpriseId;

  @FormParam("enterprise_name")
  public String enterpriseName;

  @FormParam("channel_id")
  public String channelId;

  @FormParam("channel_name")
  public String channelName;

  @FormParam("user_id")
  public String userId;

  @FormParam("user_name")
  public String userName;

  @FormParam("command")
  public String command;

  @FormParam("text")
  public String text;

  @FormParam("response_url")
  public String responseUrl;

  @FormParam("trigger_id")
  public String triggerId;

  @FormParam("api_app_id")
  public String apiAppId;

  @FormParam("ssl_check")
  public String sslCheck;

}
